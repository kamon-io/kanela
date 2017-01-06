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

package kamon.agent.circuitbreaker;


import kamon.agent.broker.EventBroker;
import kamon.agent.broker.Subscribe;
import kamon.agent.util.conf.AgentConfiguration;
import kamon.agent.util.jvm.GcEvent;
import kamon.agent.util.jvm.JvmTools;
import lombok.Value;
import lombok.val;


@Value
public class CircuitBreaker {
    JvmTools jvmTools;
    AgentConfiguration.CircuitBreakerConfig config;

    public CircuitBreaker() {
        EventBroker.instance().add(this);
        this.jvmTools = JvmTools.instance();
        this.config = AgentConfiguration.instance().getCircuitBreakerConfig();
    }

    @Subscribe
    public void onGCEvent(GcEvent event) {
         val gcProcessCpuTimePercentage = 100.0 * ((double) jvmTools.getProcessCPUCollectionTime() / jvmTools.getProcessCPUTime());

         System.out.println("gcProcessCpuTimePercentage " + gcProcessCpuTimePercentage);
         System.out.println("FreeMemoryAfterGC " + event.getPercentageFreeMemoryAfterGc());

        if((gcProcessCpuTimePercentage >= config.getGcProcessCPUThreshold()) && ((event.getPercentageFreeMemoryAfterGc() <= config.getFreeMemoryThreshold()))) {
             System.out.println("piuttttttttooooo");
         }
    }
}
