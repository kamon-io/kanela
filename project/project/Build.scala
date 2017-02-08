package project

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

import sbt._

/**
  * A temporary workaround until sbt/sbt-assembly introduce PR #233 that fix #200 (Thank to @damdev)
  */
object PluginDef extends Build {
  override lazy val projects = Seq(root)
  lazy val root = Project("plugins", file(".")) dependsOn( assemblyPlugin )
  lazy val assemblyPlugin = uri("git://github.com/damdev/sbt-assembly.git")
}
