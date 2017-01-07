package kamon.agent.broker

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class EventBrokerSpec extends  Matchers with WordSpecLike with BeforeAndAfterAll {
  "The EventBroker" should  {
    "publish a message and all interested observers should be notified" in {
        val StringMessage = "message"

        val broker = spy(classOf[EventBroker])
        val observer = spy(classOf[EventObserver])

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
  def onString(s:String):Unit = {}
}
