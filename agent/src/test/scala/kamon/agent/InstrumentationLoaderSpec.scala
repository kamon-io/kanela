package kamon.agent

import java.lang.instrument.Instrumentation

import kamon.agent.api.instrumentation.KamonInstrumentationFake
import org.mockito.Mockito
import org.mockito.Mockito.{ mock, when }
import org.scalatest.{ BeforeAndAfterAll, FlatSpec, Matchers }

import scala.collection.JavaConversions._

class InstrumentationLoaderSpec extends FlatSpec with Matchers with BeforeAndAfterAll {

  //  "should redefine API KamonInstrumentation by the agent" in {

  "with the config empty < kamon.agent.instrumentations >" should "not break" in {
    val instrumentationMock = mock(classOf[Instrumentation])
    val kamonAgentConfigMock = mock(classOf[KamonAgentConfig])
    when(kamonAgentConfigMock.getInstrumentations).thenReturn(javaslang.collection.List.empty[String])

    InstrumentationLoader.load(instrumentationMock, kamonAgentConfigMock)

    Mockito.verify(kamonAgentConfigMock, Mockito.times(1)).getInstrumentations
  }

  "with an unknown instrumentation" should "not break" in {
    val instrumentationMock = mock(classOf[Instrumentation])
    val kamonAgentConfigMock = mock(classOf[KamonAgentConfig])

    when(kamonAgentConfigMock.getInstrumentations).thenReturn(javaslang.collection.List.of("UnknownInstrumentation"))

    InstrumentationLoader.load(instrumentationMock, kamonAgentConfigMock)

    Mockito.verify(kamonAgentConfigMock, Mockito.times(1)).getInstrumentations
  }

  "with an existing instrumentation" should "register it correctly" in {
    val instrumentationMock = mock(classOf[Instrumentation])
    val kamonAgentConfigMock = mock(classOf[KamonAgentConfig])

    when(kamonAgentConfigMock.getInstrumentations) thenReturn javaslang.collection.List.of("kamon.agent.api.instrumentation.KamonInstrumentationFake")

    val registeringCounter = KamonInstrumentationFake.registeringCounter

    InstrumentationLoader.load(instrumentationMock, kamonAgentConfigMock)

    Mockito.verify(kamonAgentConfigMock, Mockito.times(1)).getInstrumentations
    KamonInstrumentationFake.registeringCounter should be(registeringCounter + 1)
  }

}
