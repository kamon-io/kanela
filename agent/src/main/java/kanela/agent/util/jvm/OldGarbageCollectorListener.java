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

import com.sun.management.GarbageCollectionNotificationInfo;
import io.vavr.collection.List;
import io.vavr.control.Option;
import io.vavr.control.Try;
import kanela.agent.broker.EventBroker;
import kanela.agent.util.annotation.Experimental;
import kanela.agent.util.conf.KanelaConfiguration;
import kanela.agent.util.log.Logger;
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

import static java.text.MessageFormat.format;


@Value
@Experimental
public class OldGarbageCollectorListener {
    Jvm tools;
    long jvmStartTime;
    EventBroker broker;
    Option<MemoryPoolMXBean> oldGenPool;
    KanelaConfiguration.OldGarbageCollectorConfig config;

    @SneakyThrows
    private OldGarbageCollectorListener(KanelaConfiguration.OldGarbageCollectorConfig configuration, Jvm jvm) {
        val memoryBeans = List.ofAll(ManagementFactory.getMemoryPoolMXBeans());

        this.jvmStartTime = ManagementFactory.getRuntimeMXBean().getStartTime();
        this.oldGenPool = memoryBeans.find(Jvm::isOldGenPool);
        this.tools = jvm;
        this.config = configuration;
        this.broker = EventBroker.instance();

        startListening();
    }

    /**
     * Attach and start the listener.
     *
     */
    public static void attach(KanelaConfiguration.OldGarbageCollectorConfig configuration, Jvm jvm) {
        if(configuration.isCircuitBreakerRunning()) {
            Try.run(() -> new OldGarbageCollectorListener(configuration, jvm))
               .andThen(() -> Logger.info(() -> format("Old Garbage Collector Listener activated.")))
               .onFailure((cause) -> Logger.error(() -> format("Error when trying to activate Old Garbage Collector Listener."), cause));
        }
    }

    public static void attach(KanelaConfiguration.OldGarbageCollectorConfig config) {
        attach(config, Jvm.instance());
    }

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
                val event = GcEvent.from(info, (double) freeMemory, jvmStartTime + info.getGcInfo().getStartTime());
                if(config.isShouldLogAfterGc()) {
                    Logger.warn(() -> format("{0}", event));
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
