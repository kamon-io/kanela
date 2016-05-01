package kamon.akka.instrumentation.mixin

import kamon.agent.api.instrumentation.Initializer
import kamon.trace.{ TraceContext, TraceContextAware, Tracer }

class TraceContextMixin extends TraceContextAware {
  var traceContext: TraceContext = _

  @Initializer
  def _initializer(): Unit = this.traceContext = Tracer.currentContext
}
