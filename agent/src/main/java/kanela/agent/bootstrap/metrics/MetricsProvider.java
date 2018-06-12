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
 * Interface for accessing and manipulating metrics instruments.
 *
 * @since 0.10
 */
public interface MetricsProvider {

    // Counters
    void incrementCounter(String name, Map<String, String> tags);
    void incrementCounter(String name, Long times, Map<String, String> tags);

    // Gauges
    void incrementGauge(String name, Map<String, String> tags);
    void incrementGauge(String name, Long times, Map<String, String> tags);
    void decrementGauge(String name, Map<String, String> tags);
    void decrementGauge(String name, Long times, Map<String, String> tags);
    void setGauge(String name, Long value);

    // Histograms
    void recordHistogram(String name, Map<String, String> tags);
    void recordHistogram(String name, Long times, Map<String, String> tags);

    // RangeSamplers
    void incrementRangeSampler(String name, Map<String, String> tags);
    void incrementRangeSampler(String name, Long times, Map<String, String> tags);
    void decrementRangeSampler(String name, Map<String, String> tags);
    void decrementRangeSampler(String name, Long times, Map<String, String> tags);
    void sampleRangeSampler();

    enum NoOp implements MetricsProvider {

        INSTANCE;

        public void incrementCounter(String name, Map<String, String> tags) {}
        public void incrementCounter(String name, Long times, Map<String, String> tags) {}
        public void incrementGauge(String name, Map<String, String> tags) {}
        public void incrementGauge(String name, Long times, Map<String, String> tags) {}
        public void decrementGauge(String name, Map<String, String> tags) {}
        public void decrementGauge(String name, Long times, Map<String, String> tags) {}
        public void setGauge(String name, Long value) {}
        public void recordHistogram(String name, Map<String, String> tags) {}
        public void recordHistogram(String name, Long times, Map<String, String> tags) {}
        public void incrementRangeSampler(String name, Map<String, String> tags) {}
        public void incrementRangeSampler(String name, Long times, Map<String, String> tags) {}
        public void decrementRangeSampler(String name, Map<String, String> tags) {}
        public void decrementRangeSampler(String name, Long times, Map<String, String> tags) {}
        public void sampleRangeSampler() {}
    }
}
