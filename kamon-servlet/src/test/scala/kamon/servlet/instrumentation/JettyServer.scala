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

package kamon.servlet.instrumentation

import java.net.InetSocketAddress
import javax.servlet.Servlet

import kamon.Kamon
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.{ ServletContextHandler, ServletHolder }
import org.scalatest.BeforeAndAfterAll

/**
 * Runs a Servlet or a Filter on an embedded Jetty server.
 */
class JettyServer(socketAddress: InetSocketAddress = new InetSocketAddress(8080)) {
  val server = new Server(socketAddress)
  val context = new ServletContextHandler(server, "/")

  def start(clazz: Class[_ <: Servlet], path: String = "/*"): this.type = {
    val servlet = new ServletHolder(clazz)

    servlet.setAsyncSupported(true)
    context.addServlet(servlet, "/")
    server.start()
    this
  }

  def stop(): this.type = {
    server.stop()
    this
  }

  def join(): this.type = {
    server.join()
    this
  }
}

trait JettySupport {
  self: BeforeAndAfterAll ⇒

  def servletClass: Class[_ <: Servlet]

  var jetty: JettyServer = _
  var port: Int = 8000

  override protected def beforeAll(): Unit = {
    jetty = new JettyServer(new InetSocketAddress(port)).start(servletClass)
    Kamon.start()
  }

  override protected def afterAll(): Unit = {
    Kamon.shutdown()
    jetty.stop()
  }
}