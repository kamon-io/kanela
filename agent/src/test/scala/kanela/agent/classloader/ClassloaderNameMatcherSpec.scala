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

package kanela.agent.classloader

import io.vavr.control.Option
import kanela.agent.api.instrumentation.InstrumentationBuilder
import kanela.agent.api.instrumentation.classloader.{ClassLoaderRefiner, ClassRefiner}
import kanela.agent.util.classloader.ClassLoaderNameMatcher.RefinedClassLoaderMatcher
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class ClassloaderNameMatcherSpec extends Matchers with WordSpecLike with BeforeAndAfterAll {
  "The ClassloaderNameMatcher" should {
    "refine the search of a class in an classloader through a ClassRefiner" in {
      val refiner = ClassRefiner.builder()
        .mustContains("kanela.agent.api.instrumentation.InstrumentationBuilder")
        .withFields("targets", "notDeclaredByObject")
        .withMethod("buildTransformations", "kanela.agent.api.instrumentation.InstrumentationDescription", "kanela.agent.util.conf.KanelaConfiguration$ModuleConfiguration", "java.lang.instrument.Instrumentation")
        .build()

      val classLoaderMatcher = RefinedClassLoaderMatcher.from(Option.of(ClassLoaderRefiner.from(refiner)))

      classLoaderMatcher.matches(classOf[InstrumentationBuilder].getClassLoader) shouldBe true
    }

    "refine the search of a class in an classloader through a list of ClassRefiners" in {
      val refiners = Array[ClassRefiner](
        ClassRefiner.builder()
          .mustContains("kanela.agent.api.instrumentation.InstrumentationBuilder")
          .withFields("targets", "notDeclaredByObject")
          .withMethod("buildTransformations", "kanela.agent.api.instrumentation.InstrumentationDescription", "kanela.agent.util.conf.KanelaConfiguration$ModuleConfiguration", "java.lang.instrument.Instrumentation")
          .build(),
        ClassRefiner.builder()
          .mustContains("kanela.agent.api.instrumentation.InstrumentationDescription")
          .withFields("classLoaderRefiner")
          .build())

      val classLoaderMatcher = RefinedClassLoaderMatcher.from(Option.of(ClassLoaderRefiner.from(refiners :_*)))

      classLoaderMatcher.matches(classOf[InstrumentationBuilder].getClassLoader) shouldBe true
    }

    "not match if some of the properties to refine the search not exists" in {
      val classLoaderMatcher = RefinedClassLoaderMatcher.from(Option.of(ClassLoaderRefiner.mustContains("kanela.agent.api.instrumentation.CanelaInstrumentation")))

      classLoaderMatcher.matches(classOf[InstrumentationBuilder].getClassLoader) shouldBe false

      val fieldRefiner = ClassRefiner.builder()
        .mustContains("kanela.agent.api.instrumentation.InstrumentationBuilder")
        .withFields("targets", "declaredByObject")
        .build()

      val fieldMatcher = RefinedClassLoaderMatcher.from(Option.of(ClassLoaderRefiner.from(fieldRefiner)))
      fieldMatcher.matches(classOf[InstrumentationBuilder].getClassLoader) shouldBe false


      val methodRefiner = ClassRefiner.builder()
        .mustContains("kanela.agent.api.instrumentation.InstrumentationBuilder")
        .withFields("targets", "notDeclaredByObject")
        .withMethod("buildTransformations", "kanela.agent.api.instrumentation.Instrumentation", "kanela.agent.util.conf.KanelaConfiguration$ModuleConfiguration", "java.lang.instrument.Instrumentation")
        .build()

      val methodMatcher = RefinedClassLoaderMatcher.from(Option.of(ClassLoaderRefiner.from(methodRefiner)))
      methodMatcher.matches(classOf[InstrumentationBuilder].getClassLoader) shouldBe false
    }
  }
}

