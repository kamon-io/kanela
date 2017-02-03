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

  import ExtractPropMonoids._

  lazy val settings: Seq[Setting[_]] = Seq(
    fork in Test         := true,
    getTestsAnnotatedWithAdditionalJVMParameters,
    testGrouping in Test <<= Def.task {
      forkedJvmPerTest(
        (definedTests in Test).value,
        (javaOptions in Test).value,
        (testsAnnotatedWithAdditionalJVMParameters in Test).value,
        (javaOptions in Test).value)
    }
  )

  protected def forkedJvmPerTest(testDefs: Seq[TestDefinition],
                                 jvmSettings: Seq[String],
                                 testsToFork: Seq[JVMDescription],
                                 javaOptions: Seq[String]): Seq[Group] = {
    val testsToForkByName: Map[String, JVMDescription] = testsToFork.map(t => t.className -> t).toMap
    val (forkedTests, otherTests) = testDefs.partition { testDef => testsToForkByName.contains(testDef.name) }
    val otherTestsGroup = Group(name = "Single JVM tests", tests = otherTests, runPolicy = SubProcess(ForkOptions(runJVMOptions = Seq.empty[String])))
    val forkedTestGroups = forkedTests map { test =>
      Group(
        name = test.name,
        tests = Seq(test),
        runPolicy = SubProcess(config = testsToForkByName(test.name).buildForkOptions(jvmSettings)))
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
              val params: Seq[String] = extractProperty[String](annotation)("parameters").toSeq.flatMap(_.split(" "))
              val enableJavaAgent: Boolean = extractProperty[Boolean](annotation)("enableJavaAgent").getOrElse(true)
              JVMDescription(definition.name(), params, enableJavaAgent)
            }
        }
    ).toSeq
  }

  protected def extractProperty[T: AnnotationPropertyExtractor](annotation: xsbti.api.Annotation)(name: String): Option[T] = {
    annotation
      .arguments()
      .find(_.name() == name)
      .map(annotationArgument => {
        implicitly[AnnotationPropertyExtractor[T]].extract(annotationArgument.value())
      })
  }

  protected case class JVMDescription(className: String,
                                      extraParameters: Seq[String],
                                      enableJavaAgent: Boolean = true) {

    def buildForkOptions(jvmSettings: Seq[String]): ForkOptions = {

      val (jvmSettingsWithAgent: Seq[String], bootJarPaths: Seq[String]) = {
        if (enableJavaAgent)
          (jvmSettings, Seq.empty)
        else
          settingsWithoutAgent(jvmSettings)
      }

      val allParameters = jvmSettingsWithAgent ++ extraParameters
      ForkOptions(runJVMOptions = allParameters, bootJars = bootJarPaths.map(new File(_)))
    }

    protected def settingsWithoutAgent(jvmSettings: Seq[String]): (Seq[String], Seq[String]) = {
      jvmSettings.partition(param => !(param.startsWith("-javaagent:") && param.contains("io.kamon/agent")))
    }
  }

}

object ExtractPropMonoids {

  trait AnnotationPropertyExtractor[A] {
    def extract(value: String): A
  }

  implicit val StringExtractPropMonoid: AnnotationPropertyExtractor[String] = new AnnotationPropertyExtractor[String] {
    def extract(value: String): String = {
      if (value.charAt(0) == '"')
        value.substring(1, value.length - 1)
      else
        value
    }
  }

  implicit val BooleanExtractPropMonoid: AnnotationPropertyExtractor[Boolean] = new AnnotationPropertyExtractor[Boolean] {
    def extract(value: String): Boolean = {
      if (value.charAt(0) == '"')
        value.substring(1, value.length - 1).toBoolean
      else
        value.toBoolean
    }
  }
}
