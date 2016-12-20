package kamon.agent;

import javaslang.control.Try;
import kamon.agent.api.instrumentation.KamonInstrumentation;
import kamon.agent.util.Agents;
import kamon.agent.util.InstrumentationLoaderFunctions;
import kamon.agent.util.log.LazyLogger;
import lombok.val;

import java.lang.instrument.Instrumentation;

import static java.text.MessageFormat.format;

public class InstrumentationLoader   {

    /**
     *TODO
     * @param instrumentation
     */
    public static void load(Instrumentation instrumentation) {
        val config = KamonAgentConfig.instance();
        val agents = Agents.from(config);

        config.getInstrumentations()
                        .map(InstrumentationLoader::loadInstrumentation)
                        .sortBy(KamonInstrumentation::order)
                        .foldLeft(agents, InstrumentationLoaderFunctions.addTransformationsToBuilders)
                        .install(instrumentation);
    }

    private static KamonInstrumentation loadInstrumentation(String instrumentationClassName) {
        LazyLogger.info(() -> format("Loading {0}...", instrumentationClassName));
        return Try.of(() -> (KamonInstrumentation) Class.forName(instrumentationClassName, true, InstrumentationLoader.class.getClassLoader()).newInstance())
                  .getOrElseThrow((cause) -> new RuntimeException(format("Error trying to load Instrumentation {0}", instrumentationClassName), cause));
    }
}