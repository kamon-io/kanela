package kamon.agent

import java.lang.instrument.Instrumentation

import org.mockito.Mockito
import org.scalatest.{ BeforeAndAfterAll, Matchers, WordSpecLike }

class InstrumentationLoaderSpec extends WordSpecLike with Matchers with BeforeAndAfterAll {

  //  "should redefine API KamonInstrumentation by the agent" in {

  "should run InstrumentationLoader correctly with empty config < kamon.agent.instrumentations >" in {
    val instrumentationMock = Mockito.mock(classOf[Instrumentation])
    InstrumentationLoader.load(instrumentationMock)
  }

  // TODO: necesitamos pasarle al InstrumentationLoader.load la config por parametro, asi testeamos con distinta config
  // Ademas no tiene que romper por que no encuentre la configuración, o por no poder parsearla bien,
  // con lo cual podemos tener un objeto ConfigAgent que parsee la configuracion
  "should not break by an unknown Instrumentation" in {
    //    val instrumentationMock = Mockito.mock(classOf[Instrumentation])
    //    InstrumentationLoader.load(instrumentationMock)
  }

}
