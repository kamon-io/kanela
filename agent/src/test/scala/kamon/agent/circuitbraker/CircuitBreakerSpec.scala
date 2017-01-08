package kamon.agent.circuitbraker

import kamon.agent.circuitbreaker.SystemThroughputCircuitBreaker
import kamon.agent.util.conf.AgentConfiguration
import kamon.agent.util.jvm.OldGarbageCollectorListener
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class CircuitBreakerSpec extends Matchers with WordSpecLike with BeforeAndAfterAll {
  "The CircuitBreaker" should {
    "when the GC is triggered manage a GC event" in {
      //      System.gc()
      //      System.gc()
      System.gc()

      val data = new Array[Object](50);

      SystemThroughputCircuitBreaker.attach(AgentConfiguration.instance().getCircuitBreakerConfig);
      OldGarbageCollectorListener.attach(AgentConfiguration.instance().getOldGarbageCollectorConfig);

      //      System.gc()
      for (i ← 0 to 100000) {
        data(i % 50) = new Array[Int](100000);
      }
      //
      System.gc()

      Thread.sleep(1000)

    }
  }
  //  }
}
