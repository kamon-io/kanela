/*
 * =========================================================================================
 * Copyright © 2013-2017 the kamon project <http://kamon.io/>
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

package kamon.agent;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

public class KamonAgent {

    /**
     * JVM hook to statically load the javaagent at startup.
     *
     * After the Java Virtual Machine (JVM) has initialized, the premain method
     * will be called. Then the real application main method will be called.
     *
     * @param args Agent argument list
     * @param instrumentation {@link Instrumentation}
     */
    public static void premain(String args, Instrumentation instrumentation) throws Exception {
        AgentEntryPoint.premain(args, instrumentation);
    }

    /**
     * JVM hook to dynamically load javaagent at runtime.
     *
     * The agent class may have an agentmain method for use when the agent is
     * started after VM startup.
     *
     * @param args Agent argument list
     * @param instrumentation {@link Instrumentation}
     */
    public static void agentmain(String args, Instrumentation instrumentation) throws Exception {
        AgentEntryPoint.agentmain(args, instrumentation);
    }


    public static Instrumentation getInstrumentation() {
        Instrumentation instrumentation = doGetInstrumentation();
        if (instrumentation == null) {
            throw new IllegalStateException("The Byte Buddy agent is not initialized");
        }
        return instrumentation;
    }


    private static Instrumentation doGetInstrumentation() {
        try {
            return (Instrumentation) ClassLoader.getSystemClassLoader()
                    .loadClass(Installer.class.getName())
                    .getMethod("getInstrumentation")
                    .invoke(null);
        } catch (Exception ignored) {
            return null;
        }
    }

    static class SimpleTransformer implements ClassFileTransformer {

        @Override
        public byte[] transform(ClassLoader loader,
                                String className,
                                Class<?> classBeingRedefined,
                                ProtectionDomain protectionDomain,
                                byte[] classfileBuffer) throws IllegalClassFormatException {


            System.out.println("Loading class => " + className.replace("/", ".") + " " + "classloader: " + loader);
            if (className.equals("java/util/concurrent/ForkJoinPool")) {
                System.out.println("Aca cargue el ForkJoinPool ----->>>>>>>>>>>>>>>>>>>>>> " + loader);
            }

            return classfileBuffer;
        }
    }
}
