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

package kamon.agent.cache;

import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.val;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.pool.TypePool;

import java.util.concurrent.ConcurrentHashMap;

@Value
@EqualsAndHashCode(callSuper = false)
public class PoolStrategyCache extends AgentBuilder.PoolStrategy.WithTypePoolCache {

    ConcurrentHashMap<ClassLoader, TypePool.CacheProvider> cache =  new ConcurrentHashMap<>();

    private PoolStrategyCache() { super(TypePool.Default.ReaderMode.EXTENDED);}

    public static PoolStrategyCache instance() {
//        return new PoolStrategyCache();
        return PoolStrategyCache.Holder.Instance;
    }

//    @Override
//    public TypePool typePool(ClassFileLocator classFileLocator, ClassLoader classLoader) {
//        return new TypePool.Default.WithLazyResolution(locate(classLoader), classFileLocator, readerMode);
//    }

    @Override
    public TypePool typePool(ClassFileLocator classFileLocator, ClassLoader classLoader) {
//        System.out.println("Using ClassLoader ->>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>" + classLoader.getClass().getName());
        return new TypePool.Default.WithLazyResolution(TypePool.CacheProvider.Simple.withObjectType(), classFileLocator, readerMode);
    }

    @Override
    protected synchronized TypePool.CacheProvider locate(ClassLoader classLoader) {
        classLoader = classLoader == null ? ClassLoader.getSystemClassLoader() : classLoader;
//        val result = cache.get(classLoader);

//        if(result != null && result.find("kamon.servlet.instrumentation.mixin.SegmentAwareExtension") != null) {
//            System.out.println("========================>>>>>>>>>>>>>>>>>>" + result.find("kamon.servlet.instrumentation.mixin.SegmentAwareExtension"));
//        }

        return cache.computeIfAbsent(classLoader, (key) -> TypePool.CacheProvider.Simple.withObjectType());
    }

    public void triggerClean() {
//       this.cache.expungeStaleEntries();
    }

    private static class Holder {
        private static final PoolStrategyCache Instance = new PoolStrategyCache();
    }
}
