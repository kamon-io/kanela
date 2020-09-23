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

package kanela.agent.cache;

import com.blogspot.mydailyjava.weaklockfree.WeakConcurrentMap;
import kanela.agent.util.NamedThreadFactory;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.val;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.pool.TypePool;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Value
@EqualsAndHashCode(callSuper = false)
public class PoolStrategyCache extends AgentBuilder.PoolStrategy.WithTypePoolCache {

    private static final PoolStrategyCache Instance = new PoolStrategyCache();

    WeakConcurrentMap<Object, TypePool.CacheProvider> cache;


    private PoolStrategyCache() {
        super(TypePool.Default.ReaderMode.FAST);


        Executors.newScheduledThreadPool(1, NamedThreadFactory.instance("strategy-cache-listener"))
                .scheduleWithFixedDelay(new Runnable() {
                    @Override
                    public void run() {
                        cache.expungeStaleEntries();
                    }
                }, 1, 1, TimeUnit.MINUTES);

        this.cache = new WeakConcurrentMap<>(false);
    }

    @Override
    protected TypePool.CacheProvider locate(ClassLoader classLoader) {
        val mapKey = (classLoader == null) ? ClassLoader.getSystemClassLoader() : classLoader;
        val mapValue = cache.getIfPresent(mapKey);
        if (mapValue == null) {
            cache.put(mapKey, TypePool.CacheProvider.Simple.withObjectType());
            return cache.get(mapKey);
        } else {
            return mapValue;
        }
    }

    public static PoolStrategyCache instance() {
        return Instance;
    }
}
