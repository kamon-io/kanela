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

import lombok.SneakyThrows;
import lombok.Value;
import lombok.experimental.NonFinal;
import lombok.val;
import sun.management.counter.LongCounter;
import sun.management.counter.perf.PerfInstrumentation;
import sun.misc.Perf;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.util.concurrent.TimeUnit;

@Value
@NonFinal
public class Jvm {

    private static final Jvm instance = new Jvm();

    PerfInstrumentation perfInstrumentation;

    @SneakyThrows
    private Jvm() {
        val jvm = ManagementFactory.getRuntimeMXBean().getName();
        val pid = Integer.parseInt(jvm.substring(0, jvm.indexOf('@')));
        val buffer = Perf.getPerf().attach(pid, "r");
        this.perfInstrumentation = new PerfInstrumentation(buffer);
    }

    public static Jvm instance() {
        return instance;
    }

    /**
     *  WARNING:
     *
     *  The counters have structured names such as sun.gc.generation.1.name, java.threads.live, java.cls.loadedClasses.
     *  The names of these counters and the data structures used to represent them are considered private, uncommitted interfaces to the HotSpot JVM.
     *  Users should not become dependent on any counter names, particularly those that start with prefixes other than "java.".
     *
     *  @return the time spent in run the GC in the current process.
     */
    public long getProcessCPUCollectionTime() {
        val frequency = getPerformanceCounterValue("sun.os.hrt.frequency");
        val fullGCTime = getPerformanceCounterValue("sun.gc.collector.1.time");
        val tick = ((double) TimeUnit.SECONDS.toNanos(1)) / frequency;
        return (long) tick * fullGCTime;
    }

    public long getProcessCPUTime( ) {
        return ( (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean() ).getProcessCpuTime();
    }

    public double getGcProcessCpuTimePercent() {
        return 100.0 * ((double) getProcessCPUCollectionTime() / getProcessCPUTime());
    }


    private long getPerformanceCounterValue(String name) {
        return ((LongCounter) perfInstrumentation.findByPattern(name).get(0)).longValue();
    }

    static boolean isOldGenPool(MemoryPoolMXBean bean) {
        return bean.getName().endsWith("Old Gen") || bean.getName().endsWith("Tenured Gen");
    }

    boolean isEndOfMayorGC(String gcAction) {
        return "end of major GC".equalsIgnoreCase(gcAction);
    }
}
