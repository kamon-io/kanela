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

package kanela.agent.bootstrap.context;

import java.util.concurrent.Callable;

/**
 * {@code ContextTrampoline} provides methods for accessing and manipulating the context from
 * instrumented bytecode.
 *
 * <p>{@code ContextTrampoline} avoids tight coupling with the concrete implementation of the
 * context by accessing and manipulating the context through the {@link ContextManipulationProvider} interface.
 *
 * <p>Both {@link ContextManipulationTrampoline} and {@link ContextManipulationProvider} are loaded by the bootstrap
 * classloader so that they can be used from classes loaded by the bootstrap classloader. A concrete
 * implementation of {@link ContextManipulationProvider} will be loaded by the system classloader. This allows
 * for using the same context implementation as the instrumented application.
 *
 * <p>{@code ContextTrampoline} is implemented as a static class to allow for easy and fast use from
 * instrumented bytecode. We cannot use dependency injection for the instrumented bytecode.
 *
 * @since 0.10
 */

public final class ContextManipulationTrampoline {

    private volatile static ContextManipulationProvider contexManipulationProvider = ContextManipulationProvider.NoOp.INSTANCE;

    private ContextManipulationTrampoline() {}

    public static void setContexManipulationProvider(ContextManipulationProvider contextManipulationProvider) {
        if(contextManipulationProvider != ContextManipulationProvider.NoOp.INSTANCE) {
            ContextManipulationTrampoline.contexManipulationProvider = contextManipulationProvider;
        }
    }

    public Runnable wrapInContextAware(Runnable runnable) { return contexManipulationProvider.wrapInContextAware(runnable);}
    public <A> Callable wrapInContextAware(Callable<A> callable) { return contexManipulationProvider.wrapInContextAware(callable);}
}
