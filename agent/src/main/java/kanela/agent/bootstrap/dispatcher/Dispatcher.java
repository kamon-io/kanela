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

package kanela.agent.bootstrap.dispatcher;

import lombok.Value;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

/**
 * <p> This class is injected into the bootstrap classpath and is used to share objects between classloaders.</p>
 *
 * Credits to @felixbarny
 */

@Value
public class Dispatcher {

    private static ConcurrentMap<String, Object> values = new ConcurrentHashMap<>();

    /**
     * Add a value to the shared map
     *
     * @param key   the key that can be used to retrieve the value via {@link #get(String)}
     * @param value the object to share
     */
    public static void put(String key, Object value) {
        values.put(key, value);
    }

    /**
     * Gets a shared value by it's key
     *
     * <p> Automatically casts the value to the desired type </p>
     *
     * @param key the key
     * @param <T> the type of the value
     * @return the shared value
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(String key) {
        return (T) values.get(key);
    }

    @SuppressWarnings("unchecked")
    public static <T> T computeIfAbsent(String key, Function<String, T> value) {
        return (T) values.computeIfAbsent(key, value);
    }

    /**
     * Gets a shared value by it's key
     *
     * <p> Automatically casts the value to the desired type </p>
     *
     * @param key        the key
     * @param valueClass the class of the value
     * @param <T>        the type of the value
     * @return the shared value
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(String key, Class<T> valueClass) {
        return (T) values.get(key);
    }

    /**
     * Gets a shared value by it's key
     *
     * @param key the key
     * @return the shared value
     */
    public static Object getObject(String key) {
        return values.get(key);
    }

    /**
     * Returns the underlying {@link ConcurrentMap}
     *
     * @return the underlying {@link ConcurrentMap}
     */
    public static ConcurrentMap<String, Object> getValues() {
        return values;
    }
}
