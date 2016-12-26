/*
 * =========================================================================================
 * Copyright © 2013-2016 the kamon project <http://kamon.io/>
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

import kamon.agent.util.banner.KamonAgentBanner;
import kamon.agent.util.conf.AgentConfiguration;
import lombok.Value;
import lombok.val;

import java.lang.instrument.Instrumentation;

import static kamon.agent.util.AgentUtil.withTimeLogging;

@Value
public class AgentEntryPoint {
    private static void start(String args, Instrumentation instrumentation) {
        withTimeLogging(() -> {
            val configuration = AgentConfiguration.instance();
            if(configuration.getShowBanner())  {
                KamonAgentBanner.print(System.out);
            }
            InstrumentationLoader.load(instrumentation, configuration);
        }, "Startup complete in");
    }

    public static void premain(String args, Instrumentation instrumentation) {
        start(args, instrumentation);
    }

    public static void agentmain(String args, Instrumentation instrumentation) {
        AgentConfiguration.instance().runtimeAttach();
        premain(args, instrumentation);
    }
}




//public class KamonPremain {
//
//    public static void premain(String args, Instrumentation instrumentation) {
//        try {
//
//            final CodeSource codeSource = KamonPremain.class.getProtectionDomain().getCodeSource();
//            final File kamonAgentJar = getKamonAgentJar(codeSource);
//
//            instrumentation.appendToBootstrapClassLoaderSearch(new JarFile(kamonAgentJar));
//
//            final Class<?> agentClass = Class.forName("kamon.agent.KamonAgent", true, null);
//            final Method premainMethod = agentClass.getMethod("premain", String.class, Instrumentation.class);
//
//            premainMethod.invoke(null, args, instrumentation);
//
//        } catch (Throwable t) {
//            // log error but don't re-throw which would prevent monitored app from starting
//            System.err.println("Kamon Agent not started: " + t.getMessage());
//            t.printStackTrace();
//        }
//    }
//
//    private static File getKamonAgentJar(CodeSource codeSource) throws Exception {
//        final File codeSourceFile = new File(codeSource.getLocation().toURI());
//        if (codeSourceFile.getName().endsWith(".jar")) {
//            return codeSourceFile;
//        }
//        throw new IOException("Could not determine kamon-agent jar location");
//    }
//}

