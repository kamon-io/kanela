/*
 * =========================================================================================
 * Copyright © 2013-2016 the kamon project <http://kamon.io/>
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 * =========================================================================================
 */

package kamon.scalaz.instrumentation

import java.util
import java.util.concurrent.{ Callable, ExecutorService, Future, TimeUnit }

import kamon.trace.{ TraceContextAware, Tracer }

class TraceContextAwareExecutorService(underlying: ExecutorService) extends ExecutorService {
  override def isShutdown: Boolean = underlying.isShutdown
  override def shutdown(): Unit = underlying.shutdown()
  override def shutdownNow(): util.List[Runnable] = underlying.shutdownNow()

  override def isTerminated: Boolean = underlying.isTerminated
  override def awaitTermination(timeout: Long, unit: TimeUnit): Boolean = underlying.awaitTermination(timeout, unit)

  override def submit[A](task: Callable[A]): Future[A] = underlying.submit(wrapCallable(task))
  override def submit[A](task: Runnable, result: A): Future[A] = underlying.submit(wrapRunnable(task), result)
  override def submit(task: Runnable): Future[_] = underlying.submit(wrapRunnable(task))

  override def execute(command: Runnable): Unit = underlying.execute(wrapRunnable(command))

  override def invokeAll[A](tasks: util.Collection[_ <: Callable[A]]): util.List[Future[A]] = underlying.invokeAll(wrapCallables(tasks))
  override def invokeAll[A](tasks: util.Collection[_ <: Callable[A]], timeout: Long, unit: TimeUnit): util.List[Future[A]] = {
    underlying.invokeAll(wrapCallables(tasks), timeout, unit)
  }

  override def invokeAny[A](tasks: util.Collection[_ <: Callable[A]]): A = underlying.invokeAny(wrapCallables(tasks))
  override def invokeAny[A](tasks: util.Collection[_ <: Callable[A]], timeout: Long, unit: TimeUnit): A = {
    underlying.invokeAny(wrapCallables(tasks), timeout, unit)
  }

  private def wrapRunnable(r: Runnable): TraceContextAwareRunnable = r match {
    case runnable: TraceContextAwareRunnable ⇒ runnable
    case other                               ⇒ new TraceContextAwareRunnable(r)
  }

  private def wrapCallable[T](r: Callable[T]): TraceContextAwareCallable[T] = r match {
    case callable: TraceContextAwareCallable[T] ⇒ callable
    case other                                  ⇒ new TraceContextAwareCallable[T](r)
  }

  private def wrapCallables[T](tasks: util.Collection[_ <: Callable[T]]) = {
    import scala.collection.JavaConverters._

    tasks.asScala.map(wrapCallable).asInstanceOf[util.Collection[_ <: Callable[T]]]
  }
}

class TraceContextAwareRunnable(r: Runnable) extends TraceContextAware with Runnable {
  val traceContext = Tracer.currentContext

  override def run(): Unit = {
    Tracer.withContext(traceContext) {
      r.run()
    }
  }
}

class TraceContextAwareCallable[A](c: Callable[A]) extends TraceContextAware with Callable[A] {
  val traceContext = Tracer.currentContext

  override def call(): A = {
    Tracer.withContext(traceContext) {
      c.call()
    }
  }
}