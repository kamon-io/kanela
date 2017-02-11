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

package kamon.play.instrumentation.agent

import kamon.agent.libs.net.bytebuddy.description.method.MethodDescription
import kamon.agent.libs.net.bytebuddy.matcher.ElementMatcher.Junction
import kamon.agent.libs.net.bytebuddy.matcher.ElementMatchers._
import kamon.agent.scala.KamonInstrumentation
import kamon.play.instrumentation.agent.advisor.{FiltersFieldAdvisor, RouteRequestAdvisor}
import kamon.play.instrumentation.agent.interceptor.{ErrorInterceptor, GlocalSettingsFiltersInterceptor}
import kamon.play.instrumentation.agent.mixin.InjectTraceContext
import play.api.mvc.EssentialFilter

class RequestInstrumentation extends KamonInstrumentation {

  val HandlerConstructorDescription: Junction[MethodDescription] = isConstructor()
    .and(takesArgument(3, classOf[Seq[EssentialFilter]]))
  val RouteRequestMethod: Junction[MethodDescription] = named("routeRequest")
  val FiltersMethod: Junction[MethodDescription] = named("filters")
  val OnServerErrorMethod: Junction[MethodDescription] = named("onClientError").or(named("onServerError"))

  forSubtypeOf("play.api.mvc.RequestHeader") { builder =>
    builder
      .withMixin(classOf[InjectTraceContext])
      .build()
  }

  forTargetType("play.api.http.DefaultHttpRequestHandler") { builder =>
    builder
      .withAdvisorFor(RouteRequestMethod, classOf[RouteRequestAdvisor])
      .withAdvisorFor(HandlerConstructorDescription, classOf[FiltersFieldAdvisor])
      .build()
  }

  forTargetType("play.api.GlobalSettings") { builder =>
    builder
      .withTransformationFor(FiltersMethod, classOf[GlocalSettingsFiltersInterceptor])
      .build()
  }

//  val OnClientServerErrorMethod: Junction[MethodDescription] = named("onClientServerError").and(takesArguments(3))
  forSubtypeOf("play.api.http.HttpErrorHandler") { builder =>
    builder
      .withTransformationFor(OnServerErrorMethod, classOf[ErrorInterceptor])
      .build()
  }
}
