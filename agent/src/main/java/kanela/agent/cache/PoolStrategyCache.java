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

package kanela.agent.cache;

import com.blogspot.mydailyjava.weaklockfree.WeakConcurrentMap;
import java.lang.ref.SoftReference;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import kanela.agent.util.NamedThreadFactory;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.val;
import lombok.var;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.pool.TypePool;

/**
 * WeakReference over the ClassLoaders and SoftReference for TypePolCache in order to avoid OOMEs
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class PoolStrategyCache extends AgentBuilder.PoolStrategy.WithTypePoolCache {

    private static final PoolStrategyCache Instance = new PoolStrategyCache();

    WeakConcurrentMap<ClassLoader, SoftReferenceCacheProvider> cache = new WeakConcurrentMap<>(false);

    private PoolStrategyCache() {
        super(TypePool.Default.ReaderMode.FAST);
        Executors.newScheduledThreadPool(1, NamedThreadFactory.instance("cache-pool-cleaner"))
                 .scheduleWithFixedDelay(() -> {
                     removeAfterAccess(1);
                     cache.expungeStaleEntries();
                 }, 1, 1, TimeUnit.MINUTES);
    }

    @Override
    protected TypePool.CacheProvider locate(ClassLoader classLoader) {
        val loader = (classLoader == null) ? ClassLoader.getSystemClassLoader() : classLoader;
        var providerRef = cache.getIfPresent(loader);

        if (providerRef == null || providerRef.get() == null) {
            providerRef = SoftReferenceCacheProvider.newOne();
            cache.put(loader, providerRef);
            providerRef =  cache.get(loader);
        }

        val cacheProvider = providerRef.get();
        return cacheProvider != null ? cacheProvider : TypePool.CacheProvider.Simple.withObjectType();
    }

    public static PoolStrategyCache instance() {
        return Instance;
    }


    private void removeAfterAccess(final long sinceMinutes) {
        cache.forEach(entry -> {
            if (System.currentTimeMillis() >= entry.getValue().getLastAccess() + TimeUnit.MINUTES.toMillis(sinceMinutes)) {
                cache.remove(entry.getKey());
            }
        });
    }

    @Value(staticConstructor = "newOne")
    private static class SoftReferenceCacheProvider {
        AtomicLong lastAccess = new AtomicLong(System.currentTimeMillis());
        SoftReference<TypePool.CacheProvider> delegate = new SoftReference<>(new TypePool.CacheProvider.Simple());

        long getLastAccess() {
            return lastAccess.get();
        }

        TypePool.CacheProvider get() {
            return delegate.get();
        }
    }
}
