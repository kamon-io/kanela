package kamon.agent.circuitbraker

import kamon.agent.circuitbreaker.CircuitBreaker
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

class CircuitBreakerSpec extends FlatSpec with Matchers with BeforeAndAfterAll {
//  "The CircuitBreaker" should {
    "when the GC is triggered" should "manage a GC event" in {
//      System.gc()
      new CircuitBreaker().install()

//      final int INTEGER_COUNT = 1000000;
//      final Integer[] integerArray = new Integer[INTEGER_COUNT];
//
//      MemoryUtil.runGC();
//
//      installGCMonitoring();
//
//      for (int i = 0; i < INTEGER_COUNT; i++) {
//        integerArray[i] = new Integer(i);
//      }
//
      System.gc()

   // Thread.sleep(50000)

    }
//  }
}
