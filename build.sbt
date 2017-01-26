/*
 * =========================================================================================
 * Copyright © 2013-2017 the kamon project <http://kamon.io/>
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

import Dependencies._
import Settings._

lazy val root = (project in file("."))
  .settings(moduleName := "kamon-agent")
  .settings(basicSettings: _*)
  .settings(noPublishing: _*)
  .aggregate(agent, agentApi)

lazy val agent = (project in file("agent"))
  .dependsOn(agentApi)
  .enablePlugins(BuildInfoPlugin)
  .settings(moduleName := "agent")
  .settings(buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion), buildInfoPackage := "kamon.agent")
  .settings(basicSettings: _*)
  .settings(Seq(publishArtifact in (Compile, packageDoc) := false, publishArtifact in packageDoc := false, sources in (Compile,doc) := Seq.empty))
  .settings(crossPaths := false,  autoScalaLibrary := false)
  .settings(assemblySettings: _*)
  .settings(libraryDependencies ++=
    compileScope(tinylog, javaslang, typesafeConfig, bytebuddy, expirinMap, scala) ++
    testScope(scalatest, mockito) ++
    providedScope(lombok))
  .settings(excludeScalaLib: _*)


lazy val agentApi = (project in file("agent-api"))
  .settings(moduleName := "agent-api")
  .settings(basicSettings: _*)
  .settings(crossPaths := false,  autoScalaLibrary := false)
  .settings(Seq(publishArtifact in (Compile, packageDoc) := false, publishArtifact in packageDoc := false, sources in (Compile,doc) := Seq.empty))
  .settings(libraryDependencies ++=
    providedScope(javaslang, typesafeConfig, slf4jApi, bytebuddy))
  .settings(excludeScalaLib: _*)
  .settings(notAggregateInAssembly: _*)

lazy val agentScala = (project in file("agent-scala"))
  .settings(moduleName := "agent-scala")
  .settings(basicSettings: _*)
  .settings(libraryDependencies ++=
    compileScope(slf4jApi, logbackCore, logbackClassic) ++
    providedScope(kamonAgent))
  .settings(excludeScalaLib: _*)
  .settings(notAggregateInAssembly: _*)

lazy val agentTest = (project in file("agent-test"))
  .dependsOn(agentScala)
  .settings(moduleName := "agent-test")
  .settings(basicSettings: _*)
  .settings(agentSettings: _*)
  .settings(agentTestSettings: _*)
  .settings(libraryDependencies ++=
    compileScope(slf4jApi, logbackCore, logbackClassic, javaslang) ++
      testScope(scalatest, mockito) ++
    providedScope(lombok, typesafeConfig, kamonAgent))
  .settings(excludeScalaLib: _*)
  .settings(noPublishing: _*)
  .settings(notAggregateInAssembly: _*)

lazy val kamonServlet = (project in file("kamon-servlet"))
  .dependsOn(agentScala)
  .settings(moduleName := "kamon-servlet")
  .settings(basicSettings: _*)
  .settings(agentSettings: _*)
  .settings(libraryDependencies ++=
    compileScope(kamonCore, servletApi) ++
    providedScope(javaslang, typesafeConfig, slf4jApi, kamonAgent) ++
    testScope(scalatest, mockito, springTest, springWeb, jetty, jettyServlet, httpClient))
  .settings(excludeScalaLib: _*)
  .settings(noPublishing: _*)
  .settings(notAggregateInAssembly: _*)

lazy val kamonScala = (project in file("kamon-scala"))
  .dependsOn(agentScala)
  .enablePlugins(JavaAgent)
  .settings(javaAgents += "io.kamon" % "agent" % "0.0.1" % "test" classifier "assembly")
  .settings(moduleName := "kamon-scala")
  .settings(basicSettings: _*)
  .settings(libraryDependencies ++=
    compileScope(kamonCore) ++
    providedScope(javaslang, typesafeConfig, slf4jApi, kamonAgent) ++
    testScope(scalatest, akkaTestKit))
  .settings(excludeScalaLib: _*)
  .settings(noPublishing: _*)
  .settings(notAggregateInAssembly: _*)
