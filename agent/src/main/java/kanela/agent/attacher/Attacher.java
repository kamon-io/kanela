/*
 * =========================================================================================
 * Copyright © 2013-2018 the kamon project <http://kamon.io/>
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 * =========================================================================================
 */

package kanela.agent.attacher;

import io.vavr.control.Try;
import lombok.Value;
import lombok.val;
import net.bytebuddy.agent.ByteBuddyAgent;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.logging.Logger;

import static kanela.agent.attacher.io.Tools.*;


@Value
public class Attacher {

    private final static Logger log = Logger.getLogger("Kanela Attacher");

    /**
     * Try to Attach Kanela to the current process.
     */
    public static void attach()  {
        Try.of(() -> Class.forName("kanela.agent.Kanela"))
                .flatMapTry(Attacher::kanelaJar)
                .andThen((agentJar) -> ByteBuddyAgent.attach(agentJar, getCurrentPID()))
                .onFailure((cause) -> log.severe(() -> "Error trying to attach the KanelaAttacher Agent to process with Id: "+ getCurrentPID() + " with error: " + cause.getMessage()));
    }

    private static Try<File> kanelaJar(Class clazz) {
        return Try.of(() -> getKanelaJar(clazz))
                .orElse(() -> Try.of(() -> generateKanelaJar(clazz)))
                .onFailure(cause -> log.severe(() -> "Error trying to obtain the KanelaAttacher Agent jar: " + cause.getMessage()));
    }


    /**
     * Return the KanelaAttacher Agent Jar File .
     *
     * @return Returns the kanela-agent.jar
     *
     * @throws URISyntaxException
     */
    private static File getKanelaJar(Class clazz) throws URISyntaxException {
        val location =  clazz.getProtectionDomain().getCodeSource().getLocation();
        return Paths.get(location.toURI()).toFile();
    }


    /**
     * Generates a temporary agent file to be loaded.
     *
     * @param agent     The main agent class.
     * @param resources Array of classes to be included with agent.
     * @return Returns a temporary jar file with the specified classes included.
     *
     * @throws FileNotFoundException
     */
    private static File generateKanelaJar(Class agent, Class... resources) throws IOException {
        val jarFile = File.createTempFile("agent", ".jar");
        jarFile.deleteOnExit();

        val manifest = new Manifest();
        val mainAttributes = manifest.getMainAttributes();
        // Create manifest stating that agent is allowed to transform classes
        mainAttributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        mainAttributes.put(new Attributes.Name("Agent-Class"), agent.getName());
        mainAttributes.put(new Attributes.Name("Can-Retransform-Classes"), "true");
        mainAttributes.put(new Attributes.Name("Can-Redefine-Classes"), "true");
        mainAttributes.put(new Attributes.Name("Can-Set-Native-Method-Prefix"), "true");

        val jos = new JarOutputStream(new FileOutputStream(jarFile), manifest);

        jos.putNextEntry(new JarEntry(agent.getName().replace('.', '/') + ".class"));

        jos.write(getBytesFromStream(agent.getClassLoader().getResourceAsStream(unqualify(agent))));
        jos.closeEntry();

        for (Class clazz : resources) {
            val name = unqualify(clazz);
            jos.putNextEntry(new JarEntry(name));
            jos.write(getBytesFromStream(clazz.getClassLoader().getResourceAsStream(name)));
            jos.closeEntry();
        }

        jos.close();
        return jarFile;
    }

    /**
     * Runs the attacher as a Java application.
     *
     * @param args The the process id, and the path to the Kanela Agent jar
     *
     */
    public static void main(String... args) {
        try {

            if(args.length < 3) {
                log.info("Proper Usage is: java -jar kanela-agent-attacher-[version].jar <pid> <path-to-kanela-agent.jar> <kanela-agent-arguments>");
                System.exit(0);
            }

            val pid = args[0];
            val agentJar = args[1];
            val agentArguments = args[2];

            ByteBuddyAgent.attach(new File(agentJar), pid, agentArguments);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
