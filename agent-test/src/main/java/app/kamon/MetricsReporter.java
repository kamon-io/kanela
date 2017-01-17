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

package app.kamon;

import lombok.val;

import java.util.Arrays;
import java.util.List;

public class MetricsReporter {

    private static List<String> units = Arrays.asList("ns.", "μs.", "ms.", "sec.");

    static void report(final String taskName, final List<Double> samples) {
        val average = average(samples);
        val per95 = percentile(samples, 95);
        val per90 = percentile(samples, 90);
        val per80 = percentile(samples, 80);
        System.out.println(String.format("Method %s. # Samples: %d. Avg: %s P95: %s P90: %s P80: %s",
                taskName, samples.size(), readableTiming(average), readableTiming(per95), readableTiming(per90), readableTiming(per80)));
    }

    private static String readableTiming(double nanoseconds) {
        double timing = nanoseconds;
        int i = 1;
        while (timing > 1000 && i < units.size()) {
            timing = timing / 1000;
            i++;
        }
        return String.format("%10.2f %s", timing, units.get(i - 1));
    }

    private static double average(final List<Double> values) {
        double sum = values.stream().mapToDouble(Double::doubleValue).sum();
        return sum / values.size();
    }

    /**
     * pick from 'http://alvinalexander.com/java/jwarehouse/commons-math/src/main/java/org/apache/commons/math/stat/descriptive/rank/Percentile.java.shtml'
     * @param values: samples
     * @param p: quantile
     * @return: percentile value
     */
    private static double percentile(final List<Double> values, final double p) {
        final int length = values.size();
        final int begin = 0;
        if ((p > 100) || (p <= 0)) {
            throw new RuntimeException("out of bounds quantile value: " + p + ", must be in (0, 100]");
        }
        if (length == 0) {
            return Double.NaN;
        }
        if (length == 1) {
            return values.get(begin); // always return single value for n = 1
        }
        double pos = p * (length + 1) / 100;
        double fpos = Math.floor(pos);
        int intPos = (int) fpos;
        double dif = pos - fpos;
        double[] sorted = values.stream().sorted().mapToDouble(Double::doubleValue).toArray();
        if (pos < 1) {
            return sorted[0];
        }
        if (pos >= length) {
            return sorted[length - 1];
        }
        double lower = sorted[intPos - 1];
        double upper = sorted[intPos];
        return lower + dif * (upper - lower);
    }
}
