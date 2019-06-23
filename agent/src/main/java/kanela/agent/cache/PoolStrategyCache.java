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

import kanela.agent.util.NamedThreadFactory;
import kanela.agent.util.log.Logger;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.val;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.pool.TypePool;
import net.jodah.expiringmap.ExpirationListener;
import net.jodah.expiringmap.ExpirationPolicy;
import net.jodah.expiringmap.ExpiringMap;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Value
@EqualsAndHashCode(callSuper = false)
public class PoolStrategyCache extends AgentBuilder.PoolStrategy.WithTypePoolCache {

    private static final PoolStrategyCache Instance = new PoolStrategyCache();

    Map<ClassLoader, TypePool.CacheProvider> cache;

    private PoolStrategyCache() {
        super(TypePool.Default.ReaderMode.FAST);
        ExpiringMap.setThreadFactory(NamedThreadFactory.instance("strategy-cache-listener"));
        this.cache = ExpiringMap
                .builder()
                .entryLoader((key) -> TypePool.CacheProvider.Simple.withObjectType())
                .expiration(10, TimeUnit.MINUTES)
                .expirationPolicy(ExpirationPolicy.ACCESSED)
                .asyncExpirationListener(LogExpirationListener())
                .build();
    }

    @Override
    protected TypePool.CacheProvider locate(ClassLoader classLoader) {
        val mapKey = (classLoader == null) ? ClassLoader.getSystemClassLoader() : classLoader;
        return cache.get(mapKey);
    }

    private ExpirationListener<Object, TypePool.CacheProvider> LogExpirationListener() {
        return (key, value) ->   Logger.debug(() -> "Expiring key: " + key + "with value" + value);
    }

    public static PoolStrategyCache instance() {
        return Instance;
    }
}
