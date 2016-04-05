package kamon.akka.instrumentation.mixin

import kamon.trace.{EmptyTraceContext, TraceContext}
import kamon.util.RelativeNanoTimestamp


class EnvelopeInstrumentationMixin extends InstrumentedEnvelope {
  @volatile var envelopeContext: EnvelopeContext = _

  def setEnvelopeContext(envelopeContext: EnvelopeContext): Unit = this.envelopeContext = envelopeContext
}


case class EnvelopeContext(nanoTime: RelativeNanoTimestamp, context: TraceContext)

object EnvelopeContext {
  val Empty = EnvelopeContext(RelativeNanoTimestamp.zero, EmptyTraceContext)
}

trait InstrumentedEnvelope {
  def envelopeContext(): EnvelopeContext
  def setEnvelopeContext(envelopeContext: EnvelopeContext): Unit
}


