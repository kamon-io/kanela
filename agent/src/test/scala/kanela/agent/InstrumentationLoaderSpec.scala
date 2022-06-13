/*
 * =========================================================================================
 * Copyright © 2013-2018 the kamon project <http://kamon.io/>
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

package kanela.agent

import java.lang.instrument.{ClassFileTransformer, Instrumentation}
import org.mockito.Mockito._
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import io.vavr.collection.{List => JList}
import kanela.agent.util.conf.KanelaConfiguration.ModuleConfiguration
import kanela.agent.util.conf.KanelaConfiguration
import net.bytebuddy.agent.builder.AgentBuilder.Default.ExecutingTransformer
import org.mockito.ArgumentMatchers._

class InstrumentationLoaderSpec extends FlatSpec with Matchers with BeforeAndAfterAll {

  "with the config empty kamon.agent.modules.x-module.instrumentations " should "not break" in {
    val instrumentationMock = mock(classOf[Instrumentation])

    val agentConfiguration = spy(KanelaConfiguration.instance())
    val dumpConfigMock = spy(KanelaConfiguration.instance().getDump)

    val agentModuleDescriptionMock = mock(classOf[ModuleConfiguration])

    when(agentConfiguration.getAgentModules).thenReturn(JList.of(Array(agentModuleDescriptionMock): _*))

    when(dumpConfigMock.getDumpEnabled).thenReturn(false)

    when(agentModuleDescriptionMock.getInstrumentations).thenReturn(JList.empty[String]())
    when(agentModuleDescriptionMock.getWithinPackage).thenReturn("")
    when(agentModuleDescriptionMock.getExcludePackage).thenReturn("")
    when(agentModuleDescriptionMock.getName).thenReturn("x-module")

    InstrumentationLoader.load(instrumentationMock, Thread.currentThread().getContextClassLoader, agentConfiguration)

    verify(agentConfiguration, times(1)).getAgentModules
  }

  // TODO FIXME: This test is useless since it explodes before getting to InstrumentationLoader.load
  // It should be refactored to expect a specific exception and not a generic RuntimeException
  // (currently blowing up due to errorneous spy+mock combo)
  "with an unknown instrumentation" should "blow up" in {
    val instrumentationMock = mock(classOf[Instrumentation])
    val agentModuleDescriptionMock = mock(classOf[ModuleConfiguration])
    when(agentModuleDescriptionMock.getInstrumentations)
      .thenReturn(JList.of[String]("UnknownInstrumentation"))
    when(agentModuleDescriptionMock.getName)
      .thenReturn("x-module")

    intercept[RuntimeException] {
      val agentConfiguration = spy(mock(classOf[KanelaConfiguration]))
      when(agentConfiguration.getAgentModules)
        .thenReturn(JList.of(Array(agentModuleDescriptionMock): _*))

      InstrumentationLoader.load(instrumentationMock, Thread.currentThread().getContextClassLoader, agentConfiguration)
    }

    verifyZeroInteractions(instrumentationMock)
  }

  "with an existing instrumentation" should "register it correctly" in {
    val instrumentationMock = mock(classOf[Instrumentation])

    val agentConfiguration = spy(KanelaConfiguration.instance())
    val dumpConfigMock = spy(KanelaConfiguration.instance().getDump)

    val agentModuleDescriptionMock = mock(classOf[ModuleConfiguration])

    when(agentConfiguration.getAgentModules).thenReturn(JList.of(Array(agentModuleDescriptionMock): _*))

    when(dumpConfigMock.getDumpEnabled).thenReturn(false)

    when(agentModuleDescriptionMock.getInstrumentations).thenReturn(JList.of[String]("kanela.agent.instrumentation.KamonFakeInstrumentationBuilder"))
    when(agentModuleDescriptionMock.getWithinPackage).thenReturn("")
    when(agentModuleDescriptionMock.getExcludePackage).thenReturn("")
    when(agentModuleDescriptionMock.getName).thenReturn("x-module")

    InstrumentationLoader.load(instrumentationMock, Thread.currentThread().getContextClassLoader, agentConfiguration)

    verify(instrumentationMock, times(1)).addTransformer(any())
  }
}
