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


package kanela.agent.util.jvm;

import lombok.SneakyThrows;
import lombok.Value;
import lombok.val;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;

@Value
public class Jvm {

    private static final Jvm instance = new Jvm();

    @SneakyThrows
    private Jvm() {
        val jvm = ManagementFactory.getRuntimeMXBean().getName();
        val pid = Integer.parseInt(jvm.substring(0, jvm.indexOf('@')));
    }

    public static Jvm instance() {
        return instance;
    }

    public double getGcCpuTimePercent(GcEvent event) {
        final long totalGcDuration = event.getInfo().getGcInfo().getDuration();
        final long percent = totalGcDuration * 1000L / event.getInfo().getGcInfo().getEndTime();
        return Double.parseDouble((percent/10) +"."+ (percent%10));
    }

    public long getProcessCPUTime( ) {
        return ( (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean() ).getProcessCpuTime();
    }

    static boolean isOldGenPool(MemoryPoolMXBean bean) {
        return bean.getName().endsWith("Old Gen") || bean.getName().endsWith("Tenured Gen");
    }

    boolean isEndOfMayorGC(String gcAction) {
        return "end of major GC".equalsIgnoreCase(gcAction);
    }
}
