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

package kanela.agent.bootstrap.tracing;

import java.io.Closeable;

/**
 * {@code TraceHandler} provides methods for creating and manipulating trace spans from
 * instrumented bytecode.
 *
 * <p>{@code TraceHandler} avoids tight coupling with the concrete trace API through the {@link
 * TraceProvider} interface.
 *
 * <p>Both {@link TraceHandler} and {@link TraceProvider} are loaded by the bootstrap classloader
 * so that they can be used from classes loaded by the bootstrap classloader. A concrete
 * implementation of {@link TraceProvider} will be loaded by the system classloader. This allows for
 * using the same trace API as the instrumented application.
 *
 * <p>{@code TraceHandler} is implemented as a static class to allow for easy and fast use from
 * instrumented bytecode. We cannot use dependency injection for the instrumented bytecode.
 *
 * @since 0.10
 */
public final class TraceHandler {

    private volatile static TraceProvider traceProvider = TraceProvider.NoOp.INSTANCE;

    private TraceHandler() {}

    public static void setTraceProvider(TraceProvider traceProvider) {
        if(traceProvider != TraceProvider.NoOp.INSTANCE) {
            TraceHandler.traceProvider = traceProvider;
        }
    }

    public static Closeable startSpan(String spanName) { return traceProvider.startSpan(spanName); }
    public static void finishSpan(Closeable scope, Throwable throwable) { traceProvider.finishSpan(scope, throwable);}
}
