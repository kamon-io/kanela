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

package kanela.agent.util;

import kanela.agent.util.log.Logger;
import lombok.Value;
import lombok.val;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.loading.ClassInjector;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Value
public class BootstrapInjector {

    public static void injectJar(Instrumentation instrumentation, String jarName) {
        val jarFile = Jar.getEmbeddedJar(jarName + ".jar")
                .onFailure(error -> Logger.error(error::getMessage, error))
                .get();

        instrumentation.appendToBootstrapClassLoaderSearch(jarFile);
    }

    public static void inject(File folder, Instrumentation instrumentation, java.util.List<Class<?>> allClasses) {
        ClassInjector.UsingInstrumentation
                .of(folder, ClassInjector.UsingInstrumentation.Target.BOOTSTRAP, instrumentation)
                .injectRaw(ClassFileLocator.ForClassLoader.readToNames(allClasses));


        for (Class<?> clazz : allClasses) {
            try {
                Class<?> aClass = Class.forName(clazz.getName(), false, null);


                System.out.println("KLADJFKLAKLDFLKJDKFLKAJKFALDKFLKADLKF" + Arrays.toString(ClassFileLocator.ForClassLoader.read(clazz)));


                if(aClass.getClassLoader() == null) System.out.println("yeeeeeeeeee!!! " + clazz.getName());
                else System.out.println("fuuuuuckkkk");
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
//            } catch (IOException e) {
//                e.printStackTrace();
            }
        }
    }

//    private static Map<TypeDescription.ForLoadedType, byte[]> getCollect(List<Class<?>> allClasses) {
//        return allClasses 
//                .stream()
//                .collect(Collectors.toMap(TypeDescription.ForLoadedType::new, value -> ClassFileLocator.ForClassLoader.ofBootLoader().locate(value));
//
//    }
}
