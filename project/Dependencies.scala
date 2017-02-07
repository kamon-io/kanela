/* =========================================================================================
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

import sbt.Keys._
import sbt._

object Dependencies {

  val resolutionRepos = Seq(
    Classpaths.typesafeSnapshots,
    "Typesafe Maven Snapshots" at "http://repo.typesafe.com/typesafe/snapshots/",
    "Typesafe Maven Releases" at "http://repo.typesafe.com/typesafe/releases/",
    "Kamon Snapshots" at "http://snapshots.kamon.io"
  )


  val logbackCore       = "ch.qos.logback"            % "logback-core"            % "1.0.13"
  val typesafeConfig    = "com.typesafe"              % "config"                  % "1.3.0"
  val javaslang         = "io.javaslang"              % "javaslang"               % "2.0.5"
  val mockito           = "org.mockito"               % "mockito-core"            % "2.4.2"
  val lombok            = "org.projectlombok"         % "lombok"                  % "1.16.12"
  val expirinMap        = "net.jodah"                 % "expiringmap"             % "0.5.7"
  val scala             = "org.scala-lang"            % "scala-library"           % "2.12.1"


  val servletApi        = "javax.servlet"             % "javax.servlet-api"       % "3.1.0"
  val kamonCore         = "io.kamon"                 %% "kamon-core"              % "0.6.5"
  val kamonTestkit      = "io.kamon"                 %% "kamon-testkit"           % "0.6.5"
  val akkaTestKit       = "com.typesafe.akka"        %% "akka-testkit"            % "2.4.14"
  val springTest        = "org.springframework"       % "spring-test"             % "4.2.5.RELEASE"
  val springWeb         = "org.springframework"       % "spring-web"              % "4.2.5.RELEASE"
  val jetty             = "org.eclipse.jetty"         % "jetty-server"            % "9.3.8.v20160314"
  val jettyServlet      = "org.eclipse.jetty"         % "jetty-servlet"           % "9.3.8.v20160314"
  val httpClient        = "org.apache.httpcomponents" % "httpclient"              % "4.5.2"
  val tinylog           = "org.tinylog"               % "tinylog"                 % "1.1"

  val kamonAgent        = "io.kamon"                  % "agent"                   % "0.0.1" classifier "assembly"

  lazy val unmanagedJarSettings: Setting[Task[Classpath]] = (unmanagedJars in Compile) := fetchUnmanagedJars

  private def fetchUnmanagedJars : Keys.Classpath = {
    val baseDirectories = file(".").getAbsoluteFile / "libs"
    (baseDirectories ** "*.jar").classpath
  }

  lazy val scalaDependency = libraryDependencies ++= Seq(resolveScalaDependency((scalaVersion in Compile).value))

  private def resolveScalaDependency(version: String) = {
    "org.scala-lang" % "scala-library" % version
  }
}
