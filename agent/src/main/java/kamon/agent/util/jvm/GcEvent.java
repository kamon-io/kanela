/*
 * =========================================================================================
 * Copyright © 2013-2017 the kamon project <http://kamon.io/>
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

package kamon.agent.util.jvm;

import com.sun.management.GarbageCollectionNotificationInfo;
import com.sun.management.GcInfo;
import lombok.Value;

import java.lang.management.MemoryUsage;
import java.util.Date;
import java.util.Map;

import static java.lang.String.format;

@Value(staticConstructor = "from")
public class GcEvent {

    static final long ONE_KIBIBYTE = 1 << 10;
    static final long ONE_MEBIBYTE = 1 << 20;
    static final long ONE_GIBIBYTE = 1 << 30;

    GarbageCollectionNotificationInfo info;
    double percentageFreeMemoryAfterGc;
    long startTime;

    @Override
    public String toString() {
        final GcInfo gcInfo = info.getGcInfo();
        final long totalBefore = getTotalUsage(gcInfo.getMemoryUsageBeforeGc());
        final long totalAfter = getTotalUsage(gcInfo.getMemoryUsageAfterGc());
        final long max = getTotalMaxUsage(gcInfo.getMemoryUsageAfterGc());
        final String  name = info.getGcName();

        String unit = "KiB";
        double cnv = ONE_KIBIBYTE;
        if (max > ONE_GIBIBYTE) {
            unit = "GiB";
            cnv = ONE_GIBIBYTE;
        } else if (max > ONE_MEBIBYTE) {
            unit = "MiB";
            cnv = ONE_MEBIBYTE;
        }

        final Date d = new Date(startTime);
        final String change = format("%.1f%s => %.1f%s / %.1f%s", totalBefore / cnv, unit, totalAfter / cnv, unit, max / cnv, unit);
        final String percentChange = format("%.1f%% => %.1f%%", 100.0 * totalBefore / max, 100.0 * totalAfter / max);

        return "OLD" + ": "
                + name + ", id=" + gcInfo.getId() + ", at=" + d.toString()
                + ", duration=" + gcInfo.getDuration() + "ms" + ", cause=[" + info.getGcCause() + "]"
                + ", " + change + " (" + percentChange + ")";
    }

    /** Compute the total usage across all pools. */
    private static long getTotalUsage(Map<String, MemoryUsage> usages) {
        long sum = 0L;
        for (Map.Entry<String, MemoryUsage> e : usages.entrySet()) {
            sum += e.getValue().getUsed();
        }
        return sum;
    }

    /** Compute the max usage across all pools. */
    private static long getTotalMaxUsage(Map<String, MemoryUsage> usages) {
        long sum = 0L;
        for (Map.Entry<String, MemoryUsage> e : usages.entrySet()) {
            long max = e.getValue().getMax();
            if (max > 0) {
                sum += e.getValue().getMax();
            }
        }
        return sum;
    }
}
