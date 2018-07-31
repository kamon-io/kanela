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

package kanela.agent.util.jvm

import kanela.agent.broker.{EventBroker, Subscribe}
import kanela.agent.util.conf.KanelaConfiguration
import org.mockito.ArgumentCaptor
import org.mockito.Mockito._
import org.scalatest.concurrent.Eventually
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration._

class OldGarbageCollectorListenerSpec extends Matchers with WordSpecLike with BeforeAndAfterAll with Eventually{
  "The OldGarbageCollectorListener" should {
    "receive a event when the GC is triggered" in {
      eventually(timeout(10 seconds)) {
        val oldGarbageCollectorConfig = spy(KanelaConfiguration.instance().getOldGarbageCollectorConfig)
        when(oldGarbageCollectorConfig.isCircuitBreakerRunning).thenReturn(true)
        when(oldGarbageCollectorConfig.isShouldLogAfterGc).thenReturn(false)

        val gcListener = spy(classOf[GCEventListener])
        val argumentCaptor = ArgumentCaptor.forClass(classOf[GcEvent])

        OldGarbageCollectorListener.attach(oldGarbageCollectorConfig)

        System.gc()

        Thread.sleep(1000) // the event is async, we need wait..

        verify(gcListener, times(2)).onGCEvent(argumentCaptor.capture())

        argumentCaptor.getValue.asInstanceOf[GcEvent].getInfo should not be null
        argumentCaptor.getValue.asInstanceOf[GcEvent].getPercentageFreeMemoryAfterGc shouldBe >(0.0)
        argumentCaptor.getValue.asInstanceOf[GcEvent].getStartTime shouldBe >(0L)
      }
    }
  }
}

class GCEventListener {
  EventBroker.instance().add(this)
  @Subscribe
  def onGCEvent(event: GcEvent): Unit = {}
}
