package kamon.agent.kotlin

import io.vavr.Function1
import kamon.agent.api.instrumentation.InstrumentationDescription
import kamon.agent.api.instrumentation.KamonInstrumentation as JKamonInstrumentation

open class KamonInstrumentation: JKamonInstrumentation() {

    fun forSubtypeOf(name: String, instrumentationFun: (InstrumentationDescription.Builder) -> InstrumentationDescription) = {
        super.forSubtypeOf({ name }, object : Function1<InstrumentationDescription.Builder, InstrumentationDescription> {
            override fun apply(v: InstrumentationDescription.Builder): InstrumentationDescription {
                return instrumentationFun(v)
            }
        })
    }
}