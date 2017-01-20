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

import sbt.Def.Setting
import sbt._
import Keys._
import sbt.Tests.{ SubProcess, Group }

object AgentTest {

  lazy val settings: Seq[Setting[_]] = Seq(
    fork in Test         := true,
    getTestsAnnotatedWithAdditionalJVMParameters,
    testGrouping in Test <<= Def.task { forkedJvmPerTest((definedTests in Test).value, (javaOptions in Test).value, (testsAnnotatedWithAdditionalJVMParameters in Test).value) }
  )

  protected def forkedJvmPerTest(testDefs: Seq[TestDefinition], jvmSettings: Seq[String], testsToFork: Seq[JVMDescription]): Seq[Group] = {
    val testsToForkByName: Map[String, JVMDescription] = testsToFork.map(t => t.className -> t).toMap
    val (forkedTests, otherTests) = testDefs.partition { testDef => testsToForkByName.contains(testDef.name) }
    val otherTestsGroup = Group(name = "Single JVM tests", tests = otherTests, runPolicy = SubProcess(javaOptions = Seq.empty[String]))
    val forkedTestGroups = forkedTests map { test =>
      Group(
        name = test.name,
        tests = Seq(test),
        runPolicy = SubProcess(javaOptions = jvmSettings ++ testsToForkByName(test.name).extraParameters))
    }
    Seq(otherTestsGroup) ++ forkedTestGroups
  }

  protected def isAnnotatedWithAdditionalJVMParameters(definition: xsbti.api.Definition): Boolean = {
    definition.annotations().exists { annotation: xsbti.api.Annotation =>
      annotation.base match {
        case proj: xsbti.api.Projection if proj.id() == "AdditionalJVMParameters" => true
        case _ => false
      }
    }
  }

  protected lazy val testsAnnotatedWithAdditionalJVMParameters: TaskKey[Seq[JVMDescription]] = taskKey[Seq[JVMDescription]]("Returns list of tests annotated with AdditionalJVMParameters")

  protected def getTestsAnnotatedWithAdditionalJVMParameters: Setting[Task[Seq[JVMDescription]]] = testsAnnotatedWithAdditionalJVMParameters in Test := {
    val analysis = (compile in Test).value
    analysis.apis.internal.values.flatMap(source =>
      source.api().definitions()
        .foldLeft(Seq.empty[JVMDescription]) { case (acc, definition) =>
          acc ++ definition.annotations()
            .find { annotation: xsbti.api.Annotation =>
              annotation.base match {
                case proj: xsbti.api.Projection if proj.id() == "AdditionalJVMParameters" => true
                case _ => false
              }
            }
            .map { annotation: xsbti.api.Annotation =>
              val params: Seq[String] = annotation
                .arguments()
                .find(_.name() == "parameters")
                .map(annotationArgument => {
                  // FIXME
                  val sanitizedValue =
                    if (annotationArgument.value().charAt(0) == '"')
                      annotationArgument.value().substring(1, annotationArgument.value().length - 1)
                    else
                      annotationArgument.value()
                  Seq(sanitizedValue)
                }) // FIXME
                .getOrElse(Seq.empty[String])
              JVMDescription(definition.name(), params)
            }
        }
    ).toSeq
  }

  protected case class JVMDescription(className: String, extraParameters: Seq[String])
}