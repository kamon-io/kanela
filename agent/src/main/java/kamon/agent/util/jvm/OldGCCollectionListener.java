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
import javaslang.collection.List;
import javaslang.control.Option;
import kamon.agent.broker.EventBroker;
import kamon.agent.util.conf.AgentConfiguration;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.val;

import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.NotificationListener;
import javax.management.openmbean.CompositeData;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;


@Value
public class OldGCCollectionListener {

    long jvmStartTime;
    Option<MemoryPoolMXBean> oldGenPool;
    JvmTools tools;
    AgentConfiguration.CircuitBreakerConfig config;
    EventBroker broker;

    @SneakyThrows
    public OldGCCollectionListener() {
        val memoryBeans = List.ofAll(ManagementFactory.getMemoryPoolMXBeans());

        this.jvmStartTime = ManagementFactory.getRuntimeMXBean().getStartTime();
        this.oldGenPool = memoryBeans.find(JvmTools::isOldGenPool);
        this.tools = JvmTools.instance();
        this.config = AgentConfiguration.instance().getCircuitBreakerConfig();
        this.broker = EventBroker.instance();
    }

    /**
     * register the listener
     */
    public void install() {
        val notificationListener = new GcNotificationListener();
        for (GarbageCollectorMXBean mbean : ManagementFactory.getGarbageCollectorMXBeans()) {
            if (mbean instanceof NotificationEmitter) {
                val emitter = (NotificationEmitter) mbean;
                emitter.addNotificationListener(notificationListener, null, null);
            }
        }
    }

    @SneakyThrows
    private void processGCEvent(GarbageCollectionNotificationInfo info) {
//        val event = new GcEvent(info, jvmStartTime + info.getGcInfo().getStartTime());

        if(tools.isEndOfMayorGC(info.getGcAction())) {
            val after = info.getGcInfo().getMemoryUsageAfterGc();

            val percentageFreeMemory = oldGenPool.map((pool) -> {
                val totalMemoryAfterGc = after.get(pool.getName()).getMax();
                val usedMemoryAfterGc = after.get(pool.getName()).getUsed();
                val freeMemoryGc = ((double) totalMemoryAfterGc - usedMemoryAfterGc) / totalMemoryAfterGc;
                return 100.0 * freeMemoryGc;
            });

            percentageFreeMemory.forEach((freeMemory) -> {
                broker.publish(new GcEvent(info, (double) freeMemory));

                if(config.isShouldLogAfterGc()) {
//                    System.out.println("accuracyGCMayorPercentCPUTime " + gcProcessCpuTimePercentage);
//                    System.out.println(freeMemory);
                }
            });
        }
    }

    private class GcNotificationListener implements NotificationListener {
        @Override
        public void handleNotification(Notification notification, Object handback) {
            val type = notification.getType();
            if(type.equals(GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION)) {
                val userData = (CompositeData) notification.getUserData();
                val info = GarbageCollectionNotificationInfo.from(userData);
                processGCEvent(info);
            }
        }
    }
}
