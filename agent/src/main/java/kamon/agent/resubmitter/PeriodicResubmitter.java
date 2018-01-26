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

package kamon.agent.resubmitter;

import io.vavr.Function0;
import kamon.agent.util.NamedThreadFactory;
import kamon.agent.util.ShutdownHook;
import kamon.agent.util.log.AgentLogger;
import lombok.Value;
import net.bytebuddy.agent.builder.AgentBuilder.RedefinitionStrategy.ResubmissionScheduler;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Value
public class PeriodicResubmitter {

    private static final Function0<ResubmissionScheduler> resubmitting = newResubmitting().memoized();
    private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(NamedThreadFactory.instance("periodic-resubmitter-scheduler"));
    private static final long TIMEOUT = 10L;

    private PeriodicResubmitter() { }

    public static ResubmissionScheduler instance() {
        return resubmitting.apply();
    }

    private static Function0<ResubmissionScheduler> newResubmitting() {
        return () -> {
            AgentLogger.info(() -> "Periodic Resubmitter was activated.");
            ShutdownHook.register(executor);
            return new ResubmissionScheduler.WithFixedDelay(executor, TIMEOUT, TimeUnit.SECONDS);
        };
    }
}
