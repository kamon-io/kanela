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

package kanela.agent.bootstrap.metrics;

import java.util.Map;


/**
 * {@code MetricsHandler} provides methods for creating and manipulating metrics from
 * instrumented bytecode.
 *
 * <p>{@code MetricsHandler} avoids tight coupling with the concrete trace API through the {@link
 * MetricsHandler} interface.
 *
 * <p>Both {@link MetricsHandler} and {@link MetricsProvider} are loaded by the bootstrap classloader
 * so that they can be used from classes loaded by the bootstrap classloader. A concrete
 * implementation of {@link MetricsProvider} will be loaded by the system classloader. This allows for
 * using the same metrics API as the instrumented application.
 *
 * <p>{@code MetricsHandler} is implemented as a static class to allow for easy and fast use from
 * instrumented bytecode.
 *
 * @since 0.10
 */
public final class MetricsHandler {

    private static MetricsProvider metricsProvider = MetricsProvider.NoOp.INSTANCE;

    private MetricsHandler() {}

    public static void setMetricsProvider(MetricsProvider metricsProvider) {
        if(metricsProvider != MetricsProvider.NoOp.INSTANCE) {
            MetricsHandler.metricsProvider = metricsProvider;
        }
    }

    public static void incrementCounter(String name, Map<String, String> tags) { metricsProvider.incrementCounter(name, tags);}
    public static void incrementCounter(String name, Long times, Map<String, String> tags) { metricsProvider.incrementCounter(name, times, tags); }

    public static void incrementGauge(String name, Map<String, String> tags) { metricsProvider.incrementGauge(name, tags); }
    public static void incrementGauge(String name, Long times, Map<String, String> tags) { metricsProvider.incrementGauge(name, times, tags); }
    public static void decrementGauge(String name, Map<String, String> tags) { metricsProvider.decrementGauge(name, tags); }
    public static void decrementGauge(String name, Long times, Map<String, String> tags) { metricsProvider.decrementGauge(name, times, tags);}
    public static void setGauge(String name, Long value) { metricsProvider.setGauge(name, value);}

    public static void recordHistogram(String name, Map<String, String> tags) { metricsProvider.recordHistogram(name, tags); }
    public static void recordHistogram(String name, Long times, Map<String, String> tags) { metricsProvider.recordHistogram(name, times, tags);}

    public static void incrementRangeSampler(String name, Map<String, String> tags) { metricsProvider.incrementRangeSampler(name, tags); }
    public static void incrementRangeSampler(String name, Long times, Map<String, String> tags) { metricsProvider.incrementRangeSampler(name, times, tags); }
    public static void decrementRangeSampler(String name, Map<String, String> tags) { metricsProvider.decrementRangeSampler(name, tags); }
    public static void decrementRangeSampler(String name, Long times, Map<String, String> tags) { metricsProvider.decrementRangeSampler(name, times, tags); }
    public static void sampleRangeSampler() { metricsProvider.sampleRangeSampler(); }
}
