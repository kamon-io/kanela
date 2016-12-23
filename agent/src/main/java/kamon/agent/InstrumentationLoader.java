package kamon.agent;

import javaslang.control.Try;
import kamon.agent.api.instrumentation.KamonInstrumentation;
import kamon.agent.builder.Agents;
import kamon.agent.util.log.LazyLogger;

import lombok.val;
import java.lang.instrument.Instrumentation;

import static java.text.MessageFormat.format;

public class InstrumentationLoader {

    /**
     *
     * @param instrumentation: provided by JVM
     */
    public static void load(Instrumentation instrumentation) {
        val config = KamonAgentConfig.instance();

        config.getInstrumentations()
                .map(InstrumentationLoader::loadInstrumentation)
                .sortBy(KamonInstrumentation::order)
                .flatMap(KamonInstrumentation::collectTransformations)
                .foldLeft(Agents.from(config), Agents::addTypeTransformation)
                .install(instrumentation);
    }

    private static KamonInstrumentation loadInstrumentation(String instrumentationClassName) {
        LazyLogger.info(() -> format("Loading {0}...", instrumentationClassName));
        return Try.of(() -> (KamonInstrumentation) Class.forName(instrumentationClassName, true, InstrumentationLoader.class.getClassLoader()).newInstance())
                .getOrElseThrow((cause) -> new RuntimeException(format("Error trying to load Instrumentation {0}", instrumentationClassName), cause));
    }
}
