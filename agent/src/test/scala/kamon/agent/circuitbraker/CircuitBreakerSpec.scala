package kamon.agent.circuitbraker

import kamon.agent.circuitbreaker.CircuitBreaker
import kamon.agent.util.jvm.OldGCCollectionListener
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

class CircuitBreakerSpec extends FlatSpec with Matchers with BeforeAndAfterAll {
//  "The OldGCCollectionListener" should {
    "when the GC is triggered" should "manage a GC event" in {
//      System.gc()
//      System.gc()
      System.gc()

      val  data = new Array[Object](50);

      new CircuitBreaker()
      new OldGCCollectionListener().install()

//      System.gc()
      for (i <- 0 to 100000) {
        data(i%50) = new Array[Int](100000);
      }
//
      System.gc()

    Thread.sleep(1000)

    }
//  }
}
