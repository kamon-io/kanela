package kamon.agent.api.instrumentation

import java.lang.instrument.Instrumentation

class KamonInstrumentationFake extends KamonInstrumentation {
  import KamonInstrumentationFake._
//  override def register(instrumentation: Instrumentation) = registeringCounter = registeringCounter + 1
}

object KamonInstrumentationFake {
  /**
   * This is a hack to can verify that an instance was used
   */
  var registeringCounter: Int = 0
}
