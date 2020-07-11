/*
 * =========================================================================================
 * Copyright © 2013-2020 the kamon project <http://kamon.io/>
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

package kanela.agent.api.instrumentation.registry;

import io.vavr.control.Try;
import kanela.agent.util.annotation.Experimental;
import kanela.agent.util.bloom.DynamicBloomFilter;
import kanela.agent.util.bloom.Key;
import kanela.agent.util.conf.KanelaConfiguration.ClassRegistryConfig;
import kanela.agent.util.log.Logger;
import lombok.Value;
import lombok.val;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.ref.SoftReference;
import java.security.ProtectionDomain;

@Value
@Experimental
public class ClassRegistry {

    public static SoftReference<DynamicBloomFilter> bloomFilterReference;

    public synchronized static void attach(Instrumentation instrumentation, ClassRegistryConfig config) {
        Try.run(() -> bloomFilterReference = new SoftReference<>(DynamicBloomFilter.create(config.getSize(), config.getErrorRate(), config.getHashCount())))
           .andThen(() -> instrumentation.addTransformer(new ClassRegistryTransformer()))
           .andThen(() -> Logger.info(() -> "Class Registry activated."))
           .onFailure((cause) -> Logger.warn(() -> "Error when trying to activate Class Registry.", cause));
    }

    public static boolean exist(String clazz) {
        val bloomReference = bloomFilterReference.get();
        if (bloomReference == null) return false;
        return bloomReference.membershipTest(new Key(clazz.getBytes()));
    }

    @Value
    static class ClassRegistryTransformer implements ClassFileTransformer {
        @Override
        public byte[] transform(ClassLoader loader,
                                String className,
                                Class<?> classBeingRedefined,
                                ProtectionDomain protectionDomain,
                                byte[] classfileBuffer) {

            if (className != null) {
                val bloomReference = bloomFilterReference.get();
                if(bloomReference != null)
                    bloomReference.add(new Key(className.replace('/', '.').getBytes()));
            }
            return classfileBuffer;
        }
    }
}
