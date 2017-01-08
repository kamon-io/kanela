/*
 * =========================================================================================
 * Copyright © 2013-2017 the kamon project <http://kamon.io/>
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

package kamon.agent.broker

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.{ BeforeAndAfterAll, Matchers, WordSpecLike }

class EventBrokerSpec extends Matchers with WordSpecLike with BeforeAndAfterAll {
  "The EventBroker" should {
    "add an observer" in {
      val broker = spy(classOf[EventBroker])
      val observer = mock(classOf[EventObserver])

      broker.add(observer)

      verify(broker, times(1)).add(observer)
    }

    "remove an observer" in {
      val broker = spy(classOf[EventBroker])
      val observer = mock(classOf[EventObserver])

      broker.remove(observer)

      verify(broker, times(1)).remove(observer)
    }

    "publish a message and all interested observers should be notified" in {
      val StringMessage = "message"

      val broker = spy(classOf[EventBroker])
      val observer = mock(classOf[EventObserver])

      broker.add(observer)
      broker.publish(StringMessage)

      verify(broker, times(1)).add(observer)
      verify(broker, times(1)).publish(any())

      verify(observer, times(1)).onString(StringMessage)
    }
  }
}

class EventObserver {
  @Subscribe
  def onString(s: String): Unit = {}
}
