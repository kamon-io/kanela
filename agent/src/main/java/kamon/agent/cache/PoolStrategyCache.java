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
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.pool.TypePool;

@Value
@EqualsAndHashCode(callSuper = false)
public class PoolStrategyCache extends AgentBuilder.PoolStrategy.WithTypePoolCache {

    WeakConcurrentMap<ClassLoader, TypePool.CacheProvider> cache =  WeakConcurrentMap.instance();

    private PoolStrategyCache() { super(TypePool.Default.ReaderMode.EXTENDED);}

    public static PoolStrategyCache instance() {
        return PoolStrategyCache.Holder.Instance;
    }

    @Override
    protected TypePool.CacheProvider locate(ClassLoader classLoader) {
        classLoader = classLoader == null ? ClassLoader.getSystemClassLoader() : classLoader;
        TypePool.CacheProvider cacheProvider = cache.get(classLoader);
        while (cacheProvider == null) {
            cacheProvider = TypePool.CacheProvider.Simple.withObjectType();
            TypePool.CacheProvider previous = cache.putIfAbsent(classLoader, cacheProvider);
            if (previous != null) {
                cacheProvider = previous;
            }
        }
        return cacheProvider;
    }

    public void triggerClean() {
       this.cache.expungeStaleEntries();
    }

    private static class Holder {
        private static final PoolStrategyCache Instance = new PoolStrategyCache();
    }
}
