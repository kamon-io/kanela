/*
 * =========================================================================================
 * Copyright © 2013-2017 the kamon project <http://kamon.io/>
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

package kamon.servlet.instrumentation.advisor

import javax.servlet.{ AsyncContext, ServletRequest }

import kamon.agent.libs.net.bytebuddy.asm.Advice._
import kamon.servlet.ServletExtension
import kamon.servlet.ServletExtension._
import kamon.servlet.instrumentation.mixin.SegmentAwareExtension
import kamon.trace.{ Segment, Tracer }

/**
 * Advisor for javax.servlet.ServletRequest::startAsync
 */
class StartAsyncAdvisor
object StartAsyncAdvisor {
  @OnMethodEnter
  def onEnter(@This request: ServletRequest): Option[Segment] = {
    Tracer.currentContext.collect { ctx ⇒
      val segmentName = ServletExtension.generateServletSegmentName(request)
      val segment = ctx.startSegment(segmentName, "servlet", SegmentLibraryName)
      segment
    }
  }

  @OnMethodExit
  def onExit(@Enter segment: Option[Segment], @Return(readOnly = false) asyncContext: AsyncContext): Unit = {
    segment.foreach { s ⇒
      val traceContext = asyncContext.asInstanceOf[SegmentAwareExtension]
      traceContext.segment(s)
    }
  }
}
