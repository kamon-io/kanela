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

public interface TraceProvider {

    /**
     * Starts a new span and sets it as the current span.
     *
     * <p>Enters the scope of code where the newly created {@code Span} is in the current Context, and
     * returns an object that represents that scope. When the returned object is closed, the scope is
     * exited, the previous Context is restored, and the newly created {@code Span} is ended using
     * kamon.trace.Span#finish.
     *
     * <p>Callers must eventually close the returned object to avoid leaking the Context.
     *
     * <p>NB: The return type of this method is intentionally {@link Closeable} and not the more
     * specific kamon.context.Storage.Scope because the latter would not be visible from
     * classes loaded by the bootstrap classloader.
     *
     * @param spanName the name of the returned kamon.trace.Span
     * @return an object that defines a scope where the newly created {@code Span} will be set to the current Context
     * kamon.trace.Tracer#buildSpan(java.lang.String)
     * @since 0.10
     */
    Closeable startSpan(String spanName);

    /**
     * Ends the current span with a status derived from the given (optional) Throwable, and closes the
     * given scope.
     *
     * @param scope an object representing the scope
     * @param throwable an optional Throwable
     * @since 0.10
     */
    void finishSpan(Closeable scope, Throwable throwable);


    enum NoOp implements TraceProvider {

        INSTANCE;

        public Closeable startSpan(String spanName) { return () -> {};}
        public void finishSpan(Closeable scope, Throwable throwable) {}
    }
}
