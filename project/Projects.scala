/*
 * =========================================================================================
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

object Projects extends Build {
  import Dependencies._
  import Settings._

  lazy val root = Project("kamon-agent", file("."))
    .settings(basicSettings: _*)
    .settings(formatSettings: _*)
    .settings(noPublishing: _*)
    .aggregate(agent, agentApi)

  lazy val agent = Project("agent",file("agent"))
    .dependsOn(agentApi)
    .settings(basicSettings: _*)
    .settings(formatSettings: _*)
    .settings(assemblySettings: _*)
    .settings(libraryDependencies ++=
      compile(javaslang, typesafeConfig, slf4jApi ,logbackCore, logbackClassic) ++
      test(scalatest))
    .settings(excludeScalaLib: _*)


  lazy val agentApi = Project("agent-api",file("agent-api"))
    .settings(basicSettings: _*)
    .settings(formatSettings: _*)
    .settings(libraryDependencies ++=
      provided(javaslang, typesafeConfig, slf4jApi))
    .settings(excludeScalaLib: _*)
    .settings(notAggregateInAssembly: _*)

  lazy val agentTest = Project("agent-test",file("agent-test"))
    .dependsOn(agentApi)
    .settings(basicSettings: _*)
    .settings(formatSettings: _*)
    .settings(libraryDependencies ++=
      provided(javaslang, typesafeConfig, slf4jApi))
    .settings(excludeScalaLib: _*)
    .settings(noPublishing: _*)
    .settings(notAggregateInAssembly: _*)
    .settings(mainClass in Compile := Some("app.kamon.MainWithAgent"))

  val noPublishing = Seq(publish := (), publishLocal := (), publishArtifact := false)
}
