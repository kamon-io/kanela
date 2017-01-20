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

package kamon.servlet.instrumentation

import javax.servlet.Servlet
import javax.servlet.http.{ HttpServlet, HttpServletRequest, HttpServletResponse }

import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClients
import org.scalatest.{ BeforeAndAfterAll, Matchers, WordSpecLike }

class ServletInstrumentationSpec extends WordSpecLike with Matchers with BeforeAndAfterAll with JettySupport with KamonSpec {

  "should propagate the TraceContext and record http server metrics for all processed requests" in {
    val httpclient = HttpClients.createDefault()
    val get = new HttpGet(s"http://localhost:$port/servlet-get")
    val notFound = new HttpGet(s"http://localhost:$port/servlet-not-found")
    val error = new HttpGet(s"http://localhost:$port/servlet-error")

    for (_ ← 1 to 10) {
      httpclient.execute(get)
    }

    for (_ ← 1 to 5) {
      httpclient.execute(notFound)
      httpclient.execute(error)
    }

    val getSnapshot = takeSnapshotOf("GET:/servlet-get", "trace")
    getSnapshot.histogram("elapsed-time").get.numberOfMeasurements should be(10)

    val metrics = takeSnapshotOf("servlet", "http-server")
    metrics.counter("GET:/servlet-get_200").get.count should be(10)
    metrics.counter("GET:/servlet-not-found_404").get.count should be(5)
    metrics.counter("GET:/servlet-error_500").get.count should be(5)
    metrics.counter("200").get.count should be(10)
    metrics.counter("404").get.count should be(5)
    metrics.counter("500").get.count should be(5)
  }

  override def servletClass: Class[_ <: Servlet] = classOf[TestServlet]
}

class TestServlet extends HttpServlet {
  override def doGet(req: HttpServletRequest, resp: HttpServletResponse): Unit = req.getRequestURI match {
    case "/servlet-not-found" ⇒ resp.setStatus(404)
    case "/servlet-error"     ⇒ resp.setStatus(500)
    case other                ⇒ resp.setStatus(200)
  }
}
