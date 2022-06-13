/*
 * =========================================================================================
 * Copyright © 2013-2010 the kamon project <http://kamon.io/>
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

import kanela.agent.attacher.Attacher
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.net.{HttpURLConnection, URL}

class BootstrapInstrumentationSpec extends AnyFlatSpec with Matchers {
  "The Bootstrap Injection feature" should
    "provide the necessary helper classes when we instrument the bootstrap class loader" in {
      Attacher.attach()

      val urlConnection = new URL("http://www.google.com").openConnection.asInstanceOf[HttpURLConnection]
      urlConnection.getRequestMethod shouldBe "[Intercepted] GET"
    }
}