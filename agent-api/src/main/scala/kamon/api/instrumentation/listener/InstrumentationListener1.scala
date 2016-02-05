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

package kamon.api.instrumentation.listener

import kamon.api.instrumentation.logger.LazyLogger
import net.bytebuddy.agent.builder.AgentBuilder.Listener
import net.bytebuddy.description.`type`.TypeDescription
import net.bytebuddy.dynamic.DynamicType

case class InstrumentationListener1() extends Listener {

  val log = LazyLogger()

  override def onError(typeName: String, throwable: Throwable): Unit = {
    log.info(s"Error - $typeName, ${throwable.getMessage}")
  }

  override def onTransformation(typeDescription: TypeDescription, dynamicType: DynamicType): Unit = {
    log.info(s"Transformed - $typeDescription, type = $dynamicType")
  }

  override def onComplete(typeName: String): Unit = {
    //    log.info(s"Completed - $typeName")
  }

  override def onIgnored(typeDescription: TypeDescription): Unit = {
    //    log.info(s"Ignored - $typeDescription")
  }
}
