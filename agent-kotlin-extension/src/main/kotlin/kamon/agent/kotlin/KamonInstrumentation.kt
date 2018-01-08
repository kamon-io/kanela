package kamon.agent.kotlin

import io.vavr.Function1
import kamon.agent.api.instrumentation.InstrumentationDescription
import java.util.function.Supplier
import kamon.agent.api.instrumentation.KamonInstrumentation as JKamonInstrumentation

open class KamonInstrumentation: JKamonInstrumentation() {

    fun String.supplied(): Supplier<String> = Supplier { this }

    fun <A, B> ((A) -> B).toVavrFunc(): Function1<A, B> {
        val underlyingFunc = this
        return object : Function1<A, B> {
            override fun apply(v: A): B {
                return underlyingFunc(v)
            }
        }
    }

    fun forSubtypeOf(name: String, instrumentationFun: (InstrumentationDescription.Builder) -> InstrumentationDescription) = {
        super.forSubtypeOf(name.supplied(), instrumentationFun.toVavrFunc())
    }

    fun forSubtypeOf(names: List<String>, instrumentationFun: (InstrumentationDescription.Builder) -> InstrumentationDescription) = {
        names.forEach { forSubtypeOf(it, instrumentationFun) }
    }

    fun forTargetType(name: String, instrumentationFun: (InstrumentationDescription.Builder) -> InstrumentationDescription) = {
        super.forTargetType(name.supplied(), instrumentationFun.toVavrFunc())
    }

    fun forTargetType(names: List<String>, instrumentationFun: (InstrumentationDescription.Builder) -> InstrumentationDescription) = {
        names.forEach { forTargetType(it, instrumentationFun) }
    }
}