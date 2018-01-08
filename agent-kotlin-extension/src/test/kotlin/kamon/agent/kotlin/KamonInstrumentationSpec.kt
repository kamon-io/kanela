package kamon.agent.kotlin

import io.kotlintest.specs.StringSpec

class KamonInstrumentationSpec : StringSpec() {

    init {

        "KamonInstrumentation from kotlin-extension should be constructed well" {
            val ki = object : KamonInstrumentation() {
                init {
                    forSubtypeOf("laala") {
                        it.build()
                    }
                }
            }
        }

    }
}