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

package app.kanela.specs

import app.kanela.cases.simple.TestClass
import kanela.agent.attacher
import kanela.agent.broker.EventBroker
import kanela.agent.reinstrument.Reinstrumenter.ReinstrumentationProtocol._
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

import scala.collection.mutable.ListBuffer

class StoppableInstrumentationSpec extends FlatSpec with Matchers with BeforeAndAfterAll {

  "A module stoppable" should "be able to retransform and reset instrumentation under critical state" in {
    val testClass = new TestClass()
    testClass.addValue(ListBuffer()) shouldBe ListBuffer("body")
    // attach agent
    attacher.Kanela.attach()
    Thread.sleep(5000) // FIXME: maybe a better solution?
    testClass.addValue(ListBuffer()) shouldBe ListBuffer("enter", "body", "exit")
    EventBroker.instance.publish(StopModules.instance)
    testClass.addValue(ListBuffer()) shouldBe ListBuffer("body")
    EventBroker.instance.publish(RestartModules.instance)
    testClass.addValue(ListBuffer()) shouldBe ListBuffer("enter", "body", "exit")
  }

}

