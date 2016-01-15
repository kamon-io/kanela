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

package kamon.agent

import java.lang.instrument.Instrumentation

import com.typesafe.config.ConfigFactory
import kamon.api.instrumentation.KamonInstrumentation
import kamon.api.instrumentation.logger.LazyLogger

import scala.collection.JavaConverters._

object InstrumentationLoader {
  val log = LazyLogger("InstrumentationLoader")
  val factory = ConfigFactory.load()
  val config = factory.getConfig("kamon.agent")

  def load(instrumentation: Instrumentation): Unit = {
    val instrumentationClass = (clazz: String) ⇒ {
      Class.forName(clazz).newInstance().asInstanceOf[KamonInstrumentation]
    }

    config.getStringList("instrumentations").asScala.foreach { clazz ⇒
      instrumentationClass(clazz).register(instrumentation)
      log.info(s"$clazz registered.")
    }
  }
}