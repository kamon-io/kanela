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
import javaslang.control.Try;
import kamon.agent.broker.EventBroker;
import kamon.agent.util.conf.AgentConfiguration;
import kamon.agent.util.log.LazyLogger;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.val;
import utils.AnsiColor;

import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.NotificationListener;
import javax.management.openmbean.CompositeData;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;

import static java.text.MessageFormat.format;


@Value
public class OldGarbageCollectorListener {

    JvmTools tools;
    long jvmStartTime;
    EventBroker broker;
    Option<MemoryPoolMXBean> oldGenPool;
    AgentConfiguration.OldGarbageCollectorConfig config;

    @SneakyThrows
    private OldGarbageCollectorListener(AgentConfiguration.OldGarbageCollectorConfig configuration) {
        val memoryBeans = List.ofAll(ManagementFactory.getMemoryPoolMXBeans());

        this.jvmStartTime = ManagementFactory.getRuntimeMXBean().getStartTime();
        this.oldGenPool = memoryBeans.find(JvmTools::isOldGenPool);
        this.tools = JvmTools.instance();
        this.config = configuration;
        this.broker = EventBroker.instance();

        startListening();
    }

    public static void attach(AgentConfiguration.OldGarbageCollectorConfig configuration) {
        if(configuration.isCircuitBreakerRunning()) {
            Try.of(() -> new OldGarbageCollectorListener(configuration))
               .andThen(() -> LazyLogger.info(() -> AnsiColor.ParseColors(format(":yellow,n: Old Garbage Collector Listener was activated."))))
               .onFailure((cause) -> LazyLogger.error(() -> AnsiColor.ParseColors(format(":red,n: Error when trying to activate Old Garbage Collector Listener.")), cause));
        }
    }

    /**
     * register the listener
     */

    private void startListening() {
        val notificationListener = new GcNotificationListener();
        for (GarbageCollectorMXBean mbean : ManagementFactory.getGarbageCollectorMXBeans()) {
            if (mbean instanceof NotificationEmitter) {
                val emitter = (NotificationEmitter) mbean;
                emitter.addNotificationListener(notificationListener, null, null);
            }
        }
    }

    private void processGCEvent(GarbageCollectionNotificationInfo info) {
        if(tools.isEndOfMayorGC(info.getGcAction())) {
            val after = info.getGcInfo().getMemoryUsageAfterGc();

            val percentageFreeMemory = oldGenPool.map((pool) -> {
                val totalMemoryAfterGc = after.get(pool.getName()).getMax();
                val usedMemoryAfterGc = after.get(pool.getName()).getUsed();
                val freeMemoryGc = ((double) totalMemoryAfterGc - usedMemoryAfterGc) / totalMemoryAfterGc;
                return 100.0 * freeMemoryGc;
            });

            percentageFreeMemory.forEach((freeMemory) -> {
                val event = new GcEvent(info, (double) freeMemory, jvmStartTime + info.getGcInfo().getStartTime());

                if(config.isShouldLogAfterGc()) {
                    LazyLogger.warn(() -> AnsiColor.ParseColors(format(":yellow,n: {0}", event)));
                }

                broker.publish(event);
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
