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


import javaslang.control.Try;
import kamon.agent.broker.EventBroker;
import kamon.agent.broker.Subscribe;
import kamon.agent.util.conf.AgentConfiguration;
import kamon.agent.util.jvm.GcEvent;
import kamon.agent.util.jvm.Jvm;
import kamon.agent.util.log.LazyLogger;
import lombok.Value;
import lombok.experimental.NonFinal;
import utils.AnsiColor;

import static java.text.MessageFormat.format;


@Value
@NonFinal
public class SystemThroughputCircuitBreaker {
    Jvm jvm;
    AgentConfiguration.CircuitBreakerConfig config;

    private SystemThroughputCircuitBreaker(AgentConfiguration.CircuitBreakerConfig config, Jvm jvm) {
        EventBroker.instance().add(this);
        this.jvm = jvm;
        this.config = config;
    }

    public static void attach(AgentConfiguration.CircuitBreakerConfig config, Jvm jvm) {
        if(config.isEnabled()){
            Try.of(() -> new SystemThroughputCircuitBreaker(config, jvm))
                    .andThen(config::circuitBreakerRunning)
                    .andThen(() -> LazyLogger.info(() -> AnsiColor.ParseColors(format(":yellow,n: System Throughput CircuitBreaker was activated."))))
                    .onFailure((cause) -> LazyLogger.error(() -> AnsiColor.ParseColors(format(":red,n: Error when trying to activate System Throughput CircuitBreaker.")), cause));
        }
    }

    public static void attach(AgentConfiguration.CircuitBreakerConfig config) {
        attach(config, Jvm.instance());
    }

    @Subscribe
    public void onGCEvent(GcEvent event) {
        if((jvm.getGcProcessCpuTimePercent() >= config.getGcProcessCPUThreshold()) && ((event.getPercentageFreeMemoryAfterGc() <= config.getFreeMemoryThreshold()))) {
            LazyLogger.warn(() -> AnsiColor.ParseColors(format(":yellow,n: System Throughput Circuit BreakerCircuit => percentage of free memory {0} and  Process GC CPU time percentage {1}.", event.getPercentageFreeMemoryAfterGc(), jvm.getGcProcessCpuTimePercent())));
        }
    }
}
