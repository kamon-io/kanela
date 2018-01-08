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

package kamon.agent.kotlin

import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import kamon.agent.util.conf.AgentConfiguration
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.junit.jupiter.api.Assertions.assertEquals
import java.lang.instrument.Instrumentation


object KamonInstrumentationSpec : Spek({

    describe("a KamonInstrumentation form agent-kotlin-extension") {

        val agentConfigMock = mock<AgentConfiguration.ModuleConfiguration> {
            on { shouldInjectInBootstrap() } doReturn false
        }

        val instrumentationMock = mock<Instrumentation> {}

        on("instrumenting with a single mixin") {
            val ki = kamonInstrumentation {
                forSubtypeOf("laala") {
                    it
                            .withMixin(ExampleMixin::class.java.supplied())
                            .build()
                }
            }

            it("should return a single transformation") {
                val transformations = ki.collectTransformations(agentConfigMock, instrumentationMock)
                assertEquals(transformations.size, 1)
                val transformation = transformations[0]
                assertEquals(transformation.mixins.size(), 1)
                assertEquals(transformation.bridges.size(), 0)
                assertEquals(transformation.transformations.size(), 0)
                assertEquals(transformation.isActive, true)
                assertEquals(transformation.elementMatcher.isDefined, true)
            }
        }
    }

})

class ExampleMixin
