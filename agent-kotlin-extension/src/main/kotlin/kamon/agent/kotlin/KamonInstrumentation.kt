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

package kamon.agent.kotlin

import kamon.agent.libs.io.vavr.Function1
import kamon.agent.api.instrumentation.InstrumentationDescription
import kamon.agent.libs.net.bytebuddy.description.method.MethodDescription
import kamon.agent.libs.net.bytebuddy.matcher.ElementMatcher
import java.util.function.Supplier
import kamon.agent.api.instrumentation.KamonInstrumentation as JKamonInstrumentation
import kamon.agent.libs.net.bytebuddy.matcher.ElementMatchers as BBMatchers

class KamonInstrumentation: JKamonInstrumentation(), ElementMatcherSugar {

    fun forSubtypeOf(name: String, instrumentationFun: (InstrumentationDescription.Builder) -> InstrumentationDescription) {
        super.forSubtypeOf(name.supplied(), instrumentationFun.toVavrFunc())
    }

    fun forSubtypeOf(names: List<String>, instrumentationFun: (InstrumentationDescription.Builder) -> InstrumentationDescription) {
        names.forEach { forSubtypeOf(it, instrumentationFun) }
    }

    fun forTargetType(name: String, instrumentationFun: (InstrumentationDescription.Builder) -> InstrumentationDescription) {
        super.forTargetType(name.supplied(), instrumentationFun.toVavrFunc())
    }

    fun forTargetType(names: List<String>, instrumentationFun: (InstrumentationDescription.Builder) -> InstrumentationDescription) {
        names.forEach { forTargetType(it, instrumentationFun) }
    }
}

interface ElementMatcherSugar {

    fun isConstructor(): ElementMatcher.Junction<MethodDescription> = BBMatchers.isConstructor()

    fun isAbstract(): ElementMatcher.Junction<MethodDescription> =
    BBMatchers.isAbstract()

    fun method(name: String): ElementMatcher.Junction<MethodDescription> =
    BBMatchers.named(name)

    fun takesArguments(quantity: Int): ElementMatcher.Junction<MethodDescription> =
    BBMatchers.takesArguments(quantity)

    fun takesArguments(vararg classes: Class<*>): ElementMatcher.Junction<MethodDescription> =
        BBMatchers.takesArguments(*classes)

    fun withArgument(index: Int, `type`: Class<*>): ElementMatcher.Junction<MethodDescription> =
    BBMatchers.takesArgument(index, `type`)

    fun anyMethod(vararg names: String): ElementMatcher.Junction<MethodDescription> =
        names.map { method(it) }.reduce { a, b -> a.or(b) }
}

fun String.supplied(): Supplier<String> = Supplier { this }
fun <T> Class<T>.supplied(): Supplier<Class<*>> = Supplier { this }

fun <A, B> ((A) -> B).toVavrFunc(): Function1<A, B> {
    val underlyingFunc = this
    return object : Function1<A, B> {
        override fun apply(v: A): B {
            return underlyingFunc(v)
        }
    }
}

fun kamonInstrumentation(init: KamonInstrumentation.() -> Unit): KamonInstrumentation {
    val kamonInstrumentation = KamonInstrumentation()
    kamonInstrumentation.init()
    return kamonInstrumentation
}
