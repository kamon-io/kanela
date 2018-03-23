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
 * Interface for accessing and manipulating the context.
 *
 * @since 0.10
 */
public interface ContextProvider {

    /**
     * Wraps a {@link Runnable} so that it executes with the context that is associated with the
     * current context.
     *
     * @param runnable a {@link Runnable} object
     * @return the wrapped {@link Runnable} object
     * @since 0.10
     */
    Runnable wrapInContextAware(Runnable runnable);

    /**
     * Wraps a {@link Callable} so that it executes with the context that is associated with the
     * current context.
     *
     * @param callable a {@link Callable} object
     * @return the wrapped {@link Callable} object
     * @since 0.10
     */
    <A> Callable wrapInContextAware(Callable<A> callable);

    enum NoOp implements ContextProvider {

        INSTANCE;

        public Runnable wrapInContextAware(Runnable runnable) { return runnable; }
        public <A> Callable wrapInContextAware(Callable<A> callable) { return callable; }
    }
}

