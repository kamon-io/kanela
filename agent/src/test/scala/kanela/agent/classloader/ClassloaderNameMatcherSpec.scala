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

import java.util

import kanela.agent.api.instrumentation.KanelaInstrumentation
import kanela.agent.api.instrumentation.classloader.{ClassLoaderRefiner, ClassRefiner}
import kanela.agent.util.classloader.ClassLoaderNameMatcher
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class ClassloaderNameMatcherSpec extends Matchers with WordSpecLike with BeforeAndAfterAll {
  "The ClassloaderNameMatcher" should {
    "" in {

      val refiner = ClassRefiner.builder()
        .mustContains("kanela.agent.api.instrumentation.KanelaInstrumentation")
        .withFields("instrumentationDescriptions", "notDeclaredByObject")
        .withMethod("buildTransformations", "kanela.agent.api.instrumentation.InstrumentationDescription", "kanela.agent.util.conf.KanelaConfiguration$ModuleConfiguration", "java.lang.instrument.Instrumentation")
        .build()

      val refiner2 = ClassRefiner.builder()
        .mustContains("kanela.agent.api.instrumentation.InstrumentationDescription")
        .withFields("classLoaderRefiner")
        .build()


      val classLoaderMatcher = ClassLoaderNameMatcher.RefinedClassLoaderMatcher.from(io.vavr.control.Option.of(
        new ClassLoaderRefiner {
          override def refiners(): util.List[ClassRefiner] = {
            val refiners = new util.ArrayList[ClassRefiner]()
            refiners.add(refiner)
            refiners.add(refiner2)
            refiners
          }
      }))

      classLoaderMatcher.matches(classOf[KanelaInstrumentation].getClassLoader) shouldBe true

      classLoaderMatcher.matches(classOf[KanelaInstrumentation].getClassLoader) shouldBe true
    }
  }
}

