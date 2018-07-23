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

package kanela.agent.circuitbreaker

import kanela.agent.broker.EventBroker
import kanela.agent.circuitbreaker.SystemThroughputCircuitBreaker
import kanela.agent.util.conf.KanelaConfiguration
import kanela.agent.util.jvm.{GcEvent, Jvm}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class CircuitBreakerSpec extends Matchers with WordSpecLike with BeforeAndAfterAll {
  "The CircuitBreaker" should {
    "trip when the thresholds are exceeded" in {
      val circuitBreakerConfig = spy(KanelaConfiguration.instance().getCircuitBreakerConfig)
      when(circuitBreakerConfig.getFreeMemoryThreshold).thenReturn(20.0)
      when(circuitBreakerConfig.getGcProcessCPUThreshold).thenReturn(20.0)

      val jvmTools = mock(classOf[Jvm])
      when(jvmTools.getGcCpuTimePercent(ArgumentMatchers.any(classOf[GcEvent]))).thenReturn(30)
      when(jvmTools.getProcessCPUTime).thenReturn(30)

      val circuitBreaker = SystemThroughputCircuitBreaker.attach(circuitBreakerConfig, jvmTools)
      EventBroker.instance().publish(GcEvent.from(null, 10.0, 100))
//      verify(circuitBreaker, times(1)).onGCEvent(argumentCaptor.capture())
    }
  }
}