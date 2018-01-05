/*
 * =========================================================================================
 * Copyright © 2013-2017 the kamon project <http://kamon.io/>
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

package app.kamon.instrumentation.mixin

import app.kamon.cases.multimixins.MixinAware.{ MixinAware1, MixinAware2, MixinAware3 }
import kamon.agent.api.instrumentation.mixin.Initializer

object MixinOverMixin {

  class MixinOverMixin1 extends MixinAware1 {
    private var _value1: String = _
    @Initializer
    def initializer1(): Unit = _value1 = "dummy 1"
    override def dummyMethod1: String = _value1
  }

  class MixinOverMixin2 extends MixinAware2 {
    private var _value2: String = _
    @Initializer
    def initializer2(): Unit = _value2 = "dummy 2"
    override def dummyMethod2: String = _value2
  }

  class MixinOverMixin3 extends MixinAware3 {
    private var _value3: String = _
    @Initializer
    def initializer3(): Unit = _value3 = "dummy 3"
    override def dummyMethod3: String = _value3
  }
}
