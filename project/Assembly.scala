/* =========================================================================================
 * Copyright © 2013-2016 the kamon project <http://kamon.io/>
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 * =========================================================================================
 */

import sbt._
import sbt.Keys._

import sbtassembly.AssemblyKeys._
import sbtassembly._

object Assembly {

  lazy val settings = Seq(
    assemblyJarName in assembly := s"kamon-agent.jar",
    assemblyOption in assembly := (assemblyOption in assembly).value.copy(cacheUnzip = false),
    packageOptions <+= (name, version, organization) map { (title, version, vendor) =>
        Package.ManifestAttributes(
          "Created-By" -> "Simple Build Tool",
          "Built-By" -> System.getProperty("user.name"),
          "Build-Jdk" -> System.getProperty("java.version"),
          "Specification-Title" -> title,
          "Specification-Version" -> version,
          "Specification-Vendor" -> vendor,
          "Implementation-Title" -> title,
          "Implementation-Version" -> version,
          "Implementation-Vendor-Id" -> vendor,
          "Implementation-Vendor" -> vendor,
          "Premain-Class" -> "kamon.agent.KamonAgent",
          "Agent-Class" -> "kamon.agent.KamonAgent",
          "Can-Redefine-Classes" -> "true",
          "Can-Retransform-Classes" -> "true")
    },
    assemblyShadeRules in assembly := Seq(
        ShadeRule.rename("net.**" -> "kamon.agent.libs.@0").inAll,
        ShadeRule.rename("scala.**" -> "kamon.agent.libs.@0").inAll,
        ShadeRule.rename("com.typesafe.config.**" -> "kamon.agent.libs.@0").inAll,
        ShadeRule.rename("ch.qos.logback.core.**" -> "kamon.agent.libs.@0").inAll,
        ShadeRule.rename("ch.qos.logback.classic.**" -> "kamon.agent.libs.@0").inAll,
        ShadeRule.rename("org.slf4j.**" -> "kamon.agent.libs.@0").inAll
    )
  )

  artifact in (Compile, assembly) := {
    val art = (artifact in (Compile, assembly)).value
    art.copy(`classifier` = Some("assembly"))
  }

  addArtifact(artifact in (Compile, assembly), assembly)

  lazy val notAggregateInAssembly = Seq(aggregate in assembly := false)
  lazy val excludeScalaLib = Seq(assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = false))
}