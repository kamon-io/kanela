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

package kamon.agent.circuitbreaker;

import com.sun.management.GarbageCollectionNotificationInfo;
import javaslang.collection.List;
import javaslang.control.Option;
import lombok.Value;
import lombok.experimental.NonFinal;
import lombok.val;

import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.NotificationListener;
import javax.management.openmbean.CompositeData;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;

@Value
public class CircuitBreaker {

    long jvmStartTime;
    Option<MemoryPoolMXBean> oldGenPool;

    @NonFinal long youngGenSizeAfter = 0;

    public CircuitBreaker() {
        val memoryBeans = List.ofAll(ManagementFactory.getMemoryPoolMXBeans());
        this.jvmStartTime = ManagementFactory.getRuntimeMXBean().getStartTime();
        this.oldGenPool = memoryBeans.find(CircuitBreakerTools::isOldGenPool);
    }

    public void install() {
        val notificationListener = new GcNotificationListener();
        for (GarbageCollectorMXBean mbean : ManagementFactory.getGarbageCollectorMXBeans()) {
            if (mbean instanceof NotificationEmitter) {
                val emitter = (NotificationEmitter) mbean;
                emitter.addNotificationListener(notificationListener, null, null);
            }
        }
    }

    private void processGCEvent(GarbageCollectionNotificationInfo info) {
        val event = new GcEvent(info, jvmStartTime + info.getGcInfo().getStartTime());

        val before = info.getGcInfo().getMemoryUsageBeforeGc();
        val after = info.getGcInfo().getMemoryUsageAfterGc();

        val oldDelta = oldGenPool.map((oldGenPoolName) -> {
            val oldBefore = before.get(oldGenPoolName).getUsed();
            val oldAfter = after.get(oldGenPoolName).getUsed();
            return oldAfter - oldBefore;
        });

        
    }


//    @Value(staticConstructor = "instance")
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

    @Value
    private class GcEvent {
        String name;
        GarbageCollectionNotificationInfo info;
//        GcType type;
        long startTime;

        GcEvent(GarbageCollectionNotificationInfo info, long startTime) {
            this.name = info.getGcName();
            this.info = info;
//            this.type = HelperFunctions.getGcType(name);
            this.startTime = startTime;
        }
    }

    private enum GcType { OLD, YOUNG, UNKNOWN};
}
