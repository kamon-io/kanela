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

package kamon.akka.instrumentation.mixin

import kamon.trace.{ EmptyTraceContext, TraceContext }
import kamon.util.RelativeNanoTimestamp

/**
 * Mixin for akka.dispatch.Envelope
 */
class EnvelopeInstrumentationMixin extends InstrumentedEnvelope {
  @volatile var envelopeContext: EnvelopeContext = _

  def setEnvelopeContext(envelopeContext: EnvelopeContext): Unit = {
    this.envelopeContext = envelopeContext
  }
}

case class EnvelopeContext(nanoTime: RelativeNanoTimestamp, context: TraceContext)

object EnvelopeContext {
  val Empty = EnvelopeContext(RelativeNanoTimestamp.zero, EmptyTraceContext)
}

trait InstrumentedEnvelope {
  def envelopeContext(): EnvelopeContext
  def setEnvelopeContext(envelopeContext: EnvelopeContext): Unit
}