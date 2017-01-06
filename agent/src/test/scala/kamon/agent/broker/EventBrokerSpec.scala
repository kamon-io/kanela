package kamon.agent.broker

import java.util.Date

import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

class EventBrokerSpec extends FlatSpec with Matchers with BeforeAndAfterAll {

  val broker = EventBroker.instance()

  "with the agent EventBroker" should "send messages and" in {
    broker.add(new EventObserver)
    broker.publish("Hello")
    broker.publish(new Date())
    broker.publish(3.1415)

//    verify(agentConfiguration, times(1)).getInstrumentations
  }
}

class EventObserver {
  @Subscribe
  def onString(s:String):Unit = {
    println("String - " + s)
  }

  @Subscribe
  def onDate(d:Date):Unit = {
    println("Date - " + d)
  }

  @Subscribe
  def onDouble(d: Double):Unit = {
    println("Double - " + d)
  }
}
