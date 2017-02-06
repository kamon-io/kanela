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

import kamon.agent.libs.net.bytebuddy.description.NamedElement
import kamon.agent.libs.net.bytebuddy.description.`type`.TypeDescription
import kamon.agent.libs.net.bytebuddy.description.method.MethodDescription
import kamon.agent.libs.net.bytebuddy.matcher.ElementMatcher
import kamon.agent.libs.net.bytebuddy.matcher.ElementMatcher.Junction
import kamon.agent.libs.net.bytebuddy.matcher.ElementMatchers._
import kamon.agent.scala.KamonInstrumentation
import kamon.play.instrumentation.agent.advisor.RouteRequestAdvisor
import kamon.play.instrumentation.agent.interceptor.{ErrorInterceptor, FiltersMethodInterceptor}
import kamon.play.instrumentation.agent.mixin.InjectTraceContext

class RequestInstrumentation extends KamonInstrumentation {

  forSubtypeOf("play.api.mvc.RequestHeader") { builder =>
    builder
      .withMixin(classOf[InjectTraceContext])
      .build()
  }

  val RouteRequestMethod: Junction[MethodDescription] = named("routeRequest")
  forTargetType("play.api.http.DefaultHttpRequestHandler") { builder =>
    builder
      .withAdvisorFor(RouteRequestMethod, classOf[RouteRequestAdvisor])
      .build()
  }

  val FiltersMethod: Junction[MethodDescription] = named("filters")
  def HttpFiltersType: ElementMatcher[_ >: TypeDescription] = {
    nameMatches[NamedElement]("play.api.http..*")
      .or(nameMatches[NamedElement]("kamon.play..*"))
      .and(not(nameContains[NamedElement]("$")))
      .and(not(nameContains[NamedElement]("play.api.http.NoHttpFilters")))
      .and(hasSuperType(named("play.api.http.HttpFilters")))
  }
  forType(HttpFiltersType) { builder =>
    builder
      .withTransformationFor(FiltersMethod, classOf[FiltersMethodInterceptor])
      .build()
  }

//  val OnClientServerErrorMethod: Junction[MethodDescription] = named("onClientServerError").and(takesArguments(3))
  val OnServerErrorMethod: Junction[MethodDescription] = named("onClientError").or(named("onServerError"))
  forSubtypeOf("play.api.http.HttpErrorHandler") { builder =>
    builder
      .withTransformationFor(OnServerErrorMethod, classOf[ErrorInterceptor])
      .build()
  }
}
