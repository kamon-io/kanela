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

package kamon.play.instrumentation.interceptor

import java.util.concurrent.Callable

import kamon.agent.libs.net.bytebuddy.implementation.bind.annotation.{RuntimeType, SuperCall}
import kamon.play.KamonFilter
import play.api.http.HttpFilters
import play.api.mvc.EssentialFilter

/**
  * Interceptor for play.api.GlobalSettings::filters
  */
class GlobalSettingsFiltersInterceptor
object GlobalSettingsFiltersInterceptor {

  @RuntimeType
  def filtersWithKamon(@SuperCall callable: Callable[HttpFilters]): HttpFilters = {
    KamonHttpFilters(callable.call(), KamonFilter)
  }
}

case class KamonHttpFilters(underlyne: HttpFilters, additionalFilter: EssentialFilter) extends HttpFilters {
  override def filters: Seq[EssentialFilter] = underlyne.filters :+ additionalFilter
}
