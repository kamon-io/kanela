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
import sbt.Tests._

def akkaDependency(name: String, version: String) = {
  "com.typesafe.akka" %% s"akka-$name" % version
}

def singleTestPerJvm(tests: Seq[TestDefinition], jvmSettings: Seq[String]): Seq[Group] =
  tests map { test =>
    Group(
      name = test.name,
      tests = Seq(test),
      runPolicy = SubProcess(ForkOptions(runJVMOptions = jvmSettings)))
  }

lazy val root = (project in file("."))
  .settings(moduleName := "kamon-agent")
  .settings(noPublishing: _*)
  .aggregate(agent, agentApi)

lazy val agent = (project in file("agent"))
  .dependsOn(agentApi)
  .enablePlugins(BuildInfoPlugin)
  .settings(moduleName := "agent")
  .settings(fork in Test := true)
  .settings(buildInfoKeys := Seq(name, version, scalaVersion, sbtVersion), buildInfoPackage := "kamon.agent",
    buildInfoRenderer in Compile := JavaClassBuildInfoRender(buildInfoOptions.value, buildInfoPackage.value, buildInfoObject.value))
  .settings(javaCommonSettings: _*)
  .settings(assemblySettings: _*)
  .settings(libraryDependencies ++=
    compileScope(tinylog, javaslang, typesafeConfig, expirinMap) ++
    testScope(scalatest, mockito) ++
    providedScope(lombok))
  .settings(scalaDependency)
  .settings(unmanagedJarSettings)
  .settings(excludeScalaLib: _*)

lazy val agentApi = (project in file("agent-api"))
  .settings(moduleName := "agent-api")
  .settings(javaCommonSettings: _*)
  .settings(libraryDependencies ++=
    providedScope(javaslang, typesafeConfig, slf4jApi))
  .settings(unmanagedJarSettings)
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
  .enablePlugins(JavaAgent)
  .settings(moduleName := "agent-test")
  .settings(basicSettings: _*)
  .settings(agentSettings)
  .settings(agentTestSettings: _*)
  .settings(libraryDependencies ++=
    compileScope(kamonAutowave, slf4jApi, logbackCore, logbackClassic, javaslang) ++
      testScope(scalatest, mockito) ++
    providedScope(lombok, typesafeConfig, kamonAgent))
  .settings((javaOptions in Test) ++= Seq("-Xcheck:jni"))
  .settings(excludeScalaLib: _*)
  .settings(noPublishing: _*)
  .settings(notAggregateInAssembly: _*)

lazy val kamonServlet = (project in file("kamon-servlet"))
  .dependsOn(agentScala)
  .enablePlugins(JavaAgent)
  .settings(moduleName := "kamon-servlet")
  .settings(basicSettings: _*)
  .settings(agentSettings)
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
  .settings(agentSettings)
  .settings(moduleName := "kamon-scala")
  .settings(basicSettings: _*)
  .settings(libraryDependencies ++=
    compileScope(kamonCore) ++
    providedScope(javaslang, typesafeConfig, slf4jApi, kamonAgent) ++
    testScope(scalatest, akkaTestKit))
  .settings(excludeScalaLib: _*)
  .settings(notAggregateInAssembly: _*)

lazy val kamonPlay24 = (project in file("kamon-play-2.4.x"))
  .dependsOn(agentScala, kamonScala)
  .enablePlugins(JavaAgent)
  .settings(agentSettings)
  .settings(basicSettings: _*)
  .settings(Seq(
    moduleName := "kamon-play-2.4",
    scalaVersion := "2.11.8",
    crossScalaVersions := Seq("2.11.8"),
    testGrouping in Test := singleTestPerJvm((definedTests in Test).value, (javaOptions in Test).value)))
  .settings(libraryDependencies ++=
    compileScope(kamonCore, play24, playWS24) ++
    providedScope(javaslang, typesafeConfig, slf4jApi, kamonAgent) ++
    testScope(playTest24))
  .settings(excludeScalaLib: _*)
  .settings(noPublishing: _*)
  .settings(notAggregateInAssembly: _*)

lazy val kamonPlay25 = (project in file("kamon-play-2.5.x"))
  .dependsOn(agentScala, kamonScala)
  .enablePlugins(JavaAgent)
  .settings(agentSettings)
  .settings(basicSettings: _*)
  .settings(Seq(
    moduleName := "kamon-play-2.5",
    scalaVersion := "2.11.8",
    crossScalaVersions := Seq("2.11.8"),
    testGrouping in Test := singleTestPerJvm((definedTests in Test).value, (javaOptions in Test).value)))
  .settings(libraryDependencies ++=
    compileScope(kamonCore, play25, playWS25) ++
    providedScope(javaslang, typesafeConfig, slf4jApi, kamonAgent) ++
    testScope(playTest25))
  .settings(excludeScalaLib: _*)
  .settings(noPublishing: _*)
  .settings(notAggregateInAssembly: _*)

lazy val kamonAkka23 = (project in file("kamon-akka-2.3.x"))
  .dependsOn(agentScala, kamonScala)
  .enablePlugins(JavaAgent)
  .settings(agentSettings)
  .settings(basicSettings: _*)
  .settings(Seq(
    bintrayPackage := "kamon-akka",
    moduleName := "kamon-akka-2.3",
    scalaVersion := "2.11.8",
    crossScalaVersions := Seq("2.11.8")))
  .settings(
    libraryDependencies ++=
      compileScope(akkaDependency("actor", `akka-2.3`), kamonCore) ++
        providedScope(javaslang, typesafeConfig, slf4jApi, kamonAgent) ++
        optionalScope(logbackClassic) ++
        testScope(scalatest, akkaDependency("testkit", `akka-2.3`), akkaDependency("slf4j", `akka-2.3`), logbackClassic))
  .settings(excludeScalaLib: _*)
  .settings(noPublishing: _*)
  .settings(notAggregateInAssembly: _*)

lazy val kamonAkka24 = (project in file("kamon-akka-2.4.x"))
  .dependsOn(agentScala, kamonScala)
  .enablePlugins(JavaAgent)
  .settings(agentSettings)
  .settings(basicSettings: _*)
  .settings(Seq(
    bintrayPackage := "kamon-akka",
    moduleName := "kamon-akka-2.4",
    scalaVersion := "2.12.1",
    crossScalaVersions := Seq("2.11.8", "2.12.1")))
  .settings(
    libraryDependencies ++=
      compileScope(akkaDependency("actor", `akka-2.4`), kamonCore) ++
        providedScope(javaslang, typesafeConfig, slf4jApi, kamonAgent) ++
        optionalScope(logbackClassic) ++
        testScope(scalatest, akkaDependency("testkit", `akka-2.4`), akkaDependency("slf4j", `akka-2.4`), logbackClassic))
  .settings(excludeScalaLib: _*)
  .settings(noPublishing: _*)
  .settings(notAggregateInAssembly: _*)

lazy val javaCommonSettings = Seq(
  crossPaths := false,
  autoScalaLibrary := false,
  publishArtifact in (Compile, packageDoc) := false,
  publishArtifact in packageDoc := false,
  sources in (Compile,doc) := Seq.empty
) ++ basicSettings

lazy val basicSettings = Seq(
  scalaVersion := "2.12.1",
  crossScalaVersions := Seq("2.11.8", "2.12.1"),
  resolvers ++= Dependencies.resolutionRepos,
  fork in run := true,
  parallelExecution in Global := false,
  javacOptions := Seq(
    "-Xlint:none",
    "-XDignore.symbol.file",
    "-source", "1.8", "-target", "1.8"),
  scalacOptions  := Seq(
    "-encoding",
    "utf8",
    "-g:vars",
    "-feature",
    "-language:postfixOps",
    "-language:implicitConversions",
    "-Xlog-reflective-calls"
  )
)

lazy val assemblySettings = Assembly.settings
lazy val notAggregateInAssembly = Assembly.notAggregateInAssembly
lazy val excludeScalaLib = Assembly.excludeScalaLib
lazy val agentTestSettings = AgentTest.settings

lazy val agentSettings = Seq(javaAgents += "io.kamon" % "agent" % (version in ThisBuild).value % "runtime;test" classifier "assembly")

