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

package kamon.play.instrumentation

import kamon.agent.scala.KamonInstrumentation
import kamon.play.instrumentation.advisor.{FiltersFieldAdvisor, RouteRequestAdvisor}
import kamon.play.instrumentation.interceptor.{ErrorInterceptor, GlobalSettingsFiltersInterceptor}
import kamon.play.instrumentation.mixin.InjectTraceContext
import play.api.mvc.EssentialFilter

class RequestInstrumentation extends KamonInstrumentation {

  val DefaultHttpRequestHandlerConstructor = isConstructor().and(withArgument(3, classOf[Seq[EssentialFilter]]))
  val RouteRequestMethod = named("routeRequest")
  val FiltersMethod = named("filters")
  val OnServerOrOnClientErrorMethod = named("onClientError").or(named("onServerError"))

  forSubtypeOf("play.api.mvc.RequestHeader") { builder =>
    builder
      .withMixin(classOf[InjectTraceContext])
      .build()
  }

  forTargetType("play.api.http.DefaultHttpRequestHandler") { builder =>
    builder
      .withAdvisorFor(RouteRequestMethod, classOf[RouteRequestAdvisor])
      .withAdvisorFor(DefaultHttpRequestHandlerConstructor, classOf[FiltersFieldAdvisor])
      .build()
  }

  forTargetType("play.api.GlobalSettings$class") { builder =>
    builder
      .withTransformationFor(FiltersMethod, classOf[GlobalSettingsFiltersInterceptor])
      .build()
  }

  forSubtypeOf("play.api.http.HttpErrorHandler") { builder =>
    builder
      .withTransformationFor(OnServerOrOnClientErrorMethod, classOf[ErrorInterceptor])
      .build()
  }
}
