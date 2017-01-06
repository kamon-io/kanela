/*
 * =========================================================================================
 * Copyright © 2013-2016 the kamon project <http://kamon.io/>
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

package app.kamon

import app.kamon.instrumentation.ExampleClass
import org.slf4j.LoggerFactory

object MainWithAgent {

  val logger = LoggerFactory.getLogger(MainWithAgent.getClass)

  def main(args: Array[String]) {
    logger.info("Start Run Agent Test")
    val exampleClass = new ExampleClass()
    exampleClass.hello()
    exampleClass.bye()
    logger.info("Exit Run Agent Test")
  }

}

