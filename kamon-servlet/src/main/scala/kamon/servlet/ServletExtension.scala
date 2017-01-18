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

package kamon.servlet

import javax.servlet.ServletRequest
import javax.servlet.http.HttpServletRequest

import akka.actor.ReflectiveDynamicAccess
import kamon.Kamon
import kamon.util.http.HttpServerMetrics
import kamon.util.logger.LazyLogger

object ServletExtension {
  val SegmentLibraryName = "async-servlet"

  val log = LazyLogger("kamon.servlet.ServletExtension")
  log.info("Starting the Kamon(Servlet) extension")

  val httpServerMetrics: HttpServerMetrics = Kamon.metrics.entity(HttpServerMetrics, "servlet")

  private val config = Kamon.config.getConfig("kamon.servlet")
  private val dynamic = new ReflectiveDynamicAccess(getClass.getClassLoader)

  private val nameGeneratorFQN = config.getString("name-generator")
  private val nameGenerator: NameGenerator = dynamic.createInstanceFor[NameGenerator](nameGeneratorFQN, Nil).get

  def generateTraceName(httpRequest: HttpServletRequest): String = nameGenerator.generateTraceName(httpRequest)
  def generateServletSegmentName(request: ServletRequest): String = nameGenerator.generateServletSegmentName(request)
}

trait NameGenerator {
  def generateTraceName(httpServletRequest: HttpServletRequest): String
  def generateServletSegmentName(servletRequest: ServletRequest): String
}

class DefaultNameGenerator extends NameGenerator {
  override def generateTraceName(httpRequest: HttpServletRequest): String = s"${httpRequest.getMethod}:${httpRequest.getRequestURI}"
  override def generateServletSegmentName(servletRequest: ServletRequest): String = "servlet-async"
}