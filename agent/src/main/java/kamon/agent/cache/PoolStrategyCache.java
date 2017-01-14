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

import kamon.agent.util.log.LazyLogger;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.val;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.pool.TypePool;
import net.jodah.expiringmap.ExpirationListener;
import net.jodah.expiringmap.ExpirationPolicy;
import net.jodah.expiringmap.ExpiringMap;
import net.jodah.expiringmap.internal.NamedThreadFactory;
import utils.AnsiColor;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.text.MessageFormat.format;

@Value
@EqualsAndHashCode(callSuper = false)
public class PoolStrategyCache extends AgentBuilder.PoolStrategy.WithTypePoolCache {

    private static final PoolStrategyCache Instance = new PoolStrategyCache();

    Map<ClassLoader, TypePool.CacheProvider> cache;

    private PoolStrategyCache() {
        super(TypePool.Default.ReaderMode.EXTENDED);
        ExpiringMap.setThreadFactory(new NamedThreadFactory("kamon-agent-strategy-cache-listener-%s"));
        this.cache = ExpiringMap
                .builder()
                .entryLoader((key) -> TypePool.CacheProvider.Simple.withObjectType())
                .expiration(5, TimeUnit.SECONDS) //TODO: configuration
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
        return (key, value) ->   LazyLogger.info(() -> AnsiColor.ParseColors(format(":yellow,n:Expiring key: " + key + "with value" + value)));
    }

    public static PoolStrategyCache instance() {
        return Instance;
    }
}
