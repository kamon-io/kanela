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

package kanela.agent.circuitbreaker;

import io.vavr.control.Try;
import kanela.agent.broker.EventBroker;
import kanela.agent.broker.Subscribe;
import kanela.agent.reinstrument.Reinstrumenter;
import kanela.agent.util.annotation.Experimental;
import kanela.agent.util.conf.KanelaConfiguration;
import kanela.agent.util.jvm.GcEvent;
import kanela.agent.util.jvm.Jvm;
import kanela.agent.util.log.Logger;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.experimental.NonFinal;

import static java.text.MessageFormat.format;

@Value
@Experimental
@RequiredArgsConstructor
public class SystemThroughputCircuitBreaker {
    KanelaConfiguration.CircuitBreakerConfig config;
    Jvm jvm;

    @NonFinal volatile int tripped = 0;

    public static void attach(KanelaConfiguration.CircuitBreakerConfig config) { attach(config, Jvm.instance()); }

    public static void attach(KanelaConfiguration.CircuitBreakerConfig config, Jvm jvm) {
        if(config.isEnabled()){
            Try.run(() -> new SystemThroughputCircuitBreaker(config, jvm))
               .andThen(config::circuitBreakerRunning)
               .andThen(() -> Logger.info(() -> "System Throughput CircuitBreaker activated."))
               .andThen(circuitBreaker ->  EventBroker.instance().add(circuitBreaker))
               .andThen(() -> Logger.debug(() -> "System Throughput CircuitBreaker is listening for GCEvents."))
               .onFailure((cause) -> Logger.error(() -> "Error when trying to activate System Throughput CircuitBreaker.", cause));
        }
    }

    @Subscribe
    public void onGCEvent(GcEvent event) {
        if((jvm.getGcCpuTimePercent(event) >= config.getGcProcessCPUThreshold()) && ((event.getPercentageFreeMemoryAfterGc() <= config.getFreeMemoryThreshold()))) {
            Logger.warn(() -> format("System Throughput Circuit BreakerCircuit => percentage of free memory {0} and  Process GC CPU time percentage {1}.", event.getPercentageFreeMemoryAfterGc(), jvm.getGcCpuTimePercent(event)));
            EventBroker.instance().publish(Reinstrumenter.ReinstrumentationProtocol.StopModules.instance());
            trip();
        } else {
            if (isTripped()) {
                Logger.info(() -> format("System Throughput Circuit BreakerCircuit => The System back to normal :) free memory {0} and  Process GC CPU time percentage {1}.", event.getPercentageFreeMemoryAfterGc(), jvm.getGcCpuTimePercent(event)));
                reset();
                EventBroker.instance().publish(Reinstrumenter.ReinstrumentationProtocol.RestartModules.instance());
            }
        }
    }

    private boolean isTripped() {
        return this.tripped == 1;
    }

    private void trip() {
        this.tripped = 1;
    }

    private void reset() {
        this.tripped = 0;
    }
}
