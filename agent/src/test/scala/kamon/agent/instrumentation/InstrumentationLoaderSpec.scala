/*
 * =========================================================================================
 * Copyright © 2013-2016 the kamon project <http://kamon.io/>
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

package kamon.agent.instrumentation

import java.lang.instrument.Instrumentation

import kamon.agent.InstrumentationLoader
import kamon.agent.util.conf.AgentConfiguration
import org.mockito.Mockito._
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

class InstrumentationLoaderSpec extends FlatSpec with Matchers with BeforeAndAfterAll {

  "with the config empty kamon.agent.instrumentations " should "not break" in {
    val instrumentationMock = mock(classOf[Instrumentation])
    val agentConfiguration = spy(AgentConfiguration.instance())
    when(agentConfiguration.getInstrumentations).thenReturn(javaslang.collection.List.empty[String]())

    InstrumentationLoader.load(instrumentationMock, agentConfiguration)

    verify(agentConfiguration, times(1)).getInstrumentations
  }

  "with an unknown instrumentation" should "blow up" in {
    val instrumentationMock = mock(classOf[Instrumentation])
    val agentConfiguration = spy(AgentConfiguration.instance())
    when(agentConfiguration.getInstrumentations).thenReturn(javaslang.collection.List.of[String]("UnknownInstrumentation"))

    intercept[RuntimeException] {
      InstrumentationLoader.load(instrumentationMock, agentConfiguration)
    }

    verifyZeroInteractions(instrumentationMock)
  }

  "with an existing instrumentation" should "register it correctly" in {
    val instrumentationMock = mock(classOf[Instrumentation])
    val agentConfiguration = spy(AgentConfiguration.instance())
    when(agentConfiguration.getInstrumentations) thenReturn javaslang.collection.List.of[String]("kamon.agent.instrumentation.KamonFakeInstrumentation")

    InstrumentationLoader.load(instrumentationMock, agentConfiguration)

    verify(instrumentationMock, times(1))
  }
}
