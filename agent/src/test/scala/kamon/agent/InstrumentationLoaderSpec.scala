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

package kamon.agent

import java.lang.instrument.{ClassFileTransformer, Instrumentation}

import kamon.agent.util.conf.AgentConfiguration.AgentModuleDescription
import kamon.agent.util.conf.AgentConfiguration
import org.mockito.Mockito._
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import javaslang.collection.{List ⇒ JList}

import org.mockito.ArgumentMatchers._

class InstrumentationLoaderSpec extends FlatSpec with Matchers with BeforeAndAfterAll {

  "with the config empty kamon.agent.modules.x-module.instrumentations " should "not break" in {
    val instrumentationMock = mock(classOf[Instrumentation])

    val agentConfiguration = spy(AgentConfiguration.instance())
    val dumpConfigMock = spy(AgentConfiguration.instance().getDump)

    val agentModuleDescriptionMock = mock(classOf[AgentModuleDescription])

    when(agentConfiguration.getAgentModules).thenReturn(JList.of(Array(agentModuleDescriptionMock): _*))

    when(dumpConfigMock.getDumpEnabled).thenReturn(false)

    when(agentModuleDescriptionMock.getInstrumentations).thenReturn(JList.empty[String]())
    when(agentModuleDescriptionMock.getWithinPackage).thenReturn("")
    when(agentModuleDescriptionMock.getName).thenReturn("x-module")

    InstrumentationLoader.load(instrumentationMock, agentConfiguration)

    verify(agentConfiguration, times(1)).getAgentModules
  }

  "with an unknown instrumentation" should "blow up" in {
    val instrumentationMock = mock(classOf[Instrumentation])
    val agentModuleDescriptionMock = mock(classOf[AgentModuleDescription])
    when(agentModuleDescriptionMock.getInstrumentations)
      .thenReturn(JList.of[String]("UnknownInstrumentation"))
    when(agentModuleDescriptionMock.getName)
      .thenReturn("x-module")

    val agentConfiguration = spy(mock(classOf[AgentConfiguration]))
    when(agentConfiguration.getAgentModules)
      .thenReturn(JList.of(Array(agentModuleDescriptionMock): _*))

    intercept[RuntimeException] {
      InstrumentationLoader.load(instrumentationMock, agentConfiguration)
    }

    verifyZeroInteractions(instrumentationMock)
  }

  "with an existing instrumentation" should "register it correctly" in {
    val instrumentationMock = mock(classOf[Instrumentation])

    val agentConfiguration = spy(AgentConfiguration.instance())
    val dumpConfigMock = spy(AgentConfiguration.instance().getDump)

    val agentModuleDescriptionMock = mock(classOf[AgentModuleDescription])

    when(agentConfiguration.getAgentModules).thenReturn(JList.of(Array(agentModuleDescriptionMock): _*))

    when(dumpConfigMock.getDumpEnabled).thenReturn(false)

    when(agentModuleDescriptionMock.getInstrumentations).thenReturn(JList.of[String]("kamon.agent.instrumentation.KamonFakeInstrumentation"))
    when(agentModuleDescriptionMock.getWithinPackage).thenReturn("")
    when(agentModuleDescriptionMock.getName).thenReturn("x-module")

    InstrumentationLoader.load(instrumentationMock, agentConfiguration)

    verify(instrumentationMock, times(1)).addTransformer(any(classOf[ClassFileTransformer]), anyBoolean())
  }
}
