/*
 *  ==========================================================================================
 *  Copyright © 2013-2025 The Kamon Project <https://kamon.io/>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 *  except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the
 *  License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 *  either express or implied. See the License for the specific language governing permissions
 *  and limitations under the License.
 *  ==========================================================================================
 */

package kanela.agent

import java.net.URL
import java.io.File
import java.io.PrintWriter
import scala.util.Using
import java.util.Collections
import java.util.Enumeration
import java.util.Optional

class ConfigurationSpec extends munit.FunSuite {

  test("should fail to load when there is no configuration") {
    val loader = classLoaderWithConfig("")
    intercept[RuntimeException] {
      Configuration.createFrom(loader)
    }
  }

  test("should load basic configuration settings") {
    val loader = classLoaderWithConfig(
      """
        |
        |kanela {
        |  log-level = INFO
        |}
        |""".stripMargin
    )

    val config = Configuration.createFrom(loader)
    assertEquals(config.agent().logLevel(), "INFO")
  }

  test("should load a bare minimum module configuration") {
    val loader = classLoaderWithConfig(
      """
        |
        |kanela {
        |  log-level = INFO
        |
        |  modules {
        |    tester {
        |      name = "tester module"
        |      instrumentations = [ "some.type.Instrumentation" ]
        |      within = [ "some.type.*" ]
        |    }
        |  }
        |}
        |""".stripMargin
    )

    val config = Configuration.createFrom(loader)
    val firstModule = config.modules().get(0)
    assertEquals(firstModule.name(), "tester module")
    assertEquals(firstModule.description(), Optional.empty[String]())
    assertEquals(firstModule.enabled(), true)
    assertEquals(firstModule.order(), 1)
  }

  test("should load modules without a 'within' section") {
    val loader = classLoaderWithConfig(
      """
        |
        |kanela {
        |  log-level = INFO
        |
        |  modules {
        |    tester {
        |      name = "tester module"
        |      instrumentations = [ "some.type.Instrumentation" ]
        |    }
        |  }
        |}
        |""".stripMargin
    )

    val config = Configuration.createFrom(loader)
    val firstModule = config.modules().get(0)
    assertEquals(firstModule.name(), "tester module")
    assertEquals(firstModule.description(), Optional.empty[String]())
    assertEquals(firstModule.enabled(), true)
    assertEquals(firstModule.order(), 1)
  }

  test("sohuld load all module configuration settings") {
    val loader = classLoaderWithConfig(
      """
        |
        |kanela {
        |  log-level = INFO
        |
        |  modules {
        |    tester {
        |      name = "tester module"
        |      description = "This does something"
        |      enabled = no
        |      order = 42
        |      instrumentations = [ "some.type.Instrumentation" ]
        |      within = [ "some.type.*" ]
        |      exclude = [ "some.other.type.*" ]
        |    }
        |  }
        |}
        |""".stripMargin
    )

    val config = Configuration.createFrom(loader)
    val firstModule = config.modules().get(0)
    assertEquals(firstModule.name(), "tester module")
    assertEquals(firstModule.description(), Optional.of("This does something"))
    assertEquals(firstModule.enabled(), false)
    assertEquals(firstModule.order(), 42)
    assertEquals(firstModule.instrumentations(), java.util.List.of("some.type.Instrumentation"))
    assertEquals(firstModule.prefixes(), java.util.List.of("some.type.*"))
    assertEquals(firstModule.excludedPrefixes(), Optional.of(java.util.List.of("some.other.type.*")))
  }

  def classLoaderWithConfig(reference: String): ClassLoader = new ClassLoader {
    val tempReferenceFile = File.createTempFile("reference", "conf")
    val referenceConfURL = tempReferenceFile.toURI.toURL
    tempReferenceFile.deleteOnExit()

    Using.resource(new PrintWriter(tempReferenceFile)) { writer =>
      writer.write(reference)
    }

    override def getResource(name: String): URL = {
      if (name == "reference.conf") referenceConfURL
      else super.getResource(name)
    }

    override def getResources(name: String): Enumeration[URL] = {
      if (name == "reference.conf")
        Collections.enumeration(Collections.singletonList(referenceConfURL))
      else
        super.getResources(name)
    }
  }
}
