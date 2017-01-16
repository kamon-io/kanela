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

package kamon.agent.api.instrumentation.listener;

import javaslang.Function1;
import kamon.agent.util.NamedThreadFactory;
import kamon.agent.util.log.LazyLogger;
import lombok.Value;
import net.bytebuddy.agent.builder.AgentBuilder;

import java.lang.instrument.Instrumentation;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Value
public class PeriodicResubmitterListener {

    private static final Function1<Instrumentation, AgentBuilder.Listener> resubmittingListener = newResubmittingListener().memoized();
    private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(NamedThreadFactory.instance("periodic-resubmitter-listener"));

    private PeriodicResubmitterListener(){}

    public static AgentBuilder.Listener instance(Instrumentation instrumentation) {
        return resubmittingListener.apply(instrumentation);
    }

    private static Function1<Instrumentation, AgentBuilder.Listener> newResubmittingListener() {
        return (instrumentation) -> {
            LazyLogger.infoColor(() -> "Periodic Class Resubmitter was activated.");
            return new AgentBuilder.Listener.Resubmitting(instrumentation).scheduled(executor, 10, TimeUnit.SECONDS);
        };
    }
}
