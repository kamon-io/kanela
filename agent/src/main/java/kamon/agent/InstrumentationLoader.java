package kamon.agent;


import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import kamon.agent.api.impl.instrumentation.KamonInstrumentation;
import kamon.agent.api.impl.instrumentation.mixin.MixinDescription;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.instrument.Instrumentation;

import static net.bytebuddy.matcher.ElementMatchers.named;

public class InstrumentationLoader {
    private static final Config factory = ConfigFactory.load();

    public static void load(Instrumentation instrumentation) {

        loadInstrumentationAPI(instrumentation);

        Config config = factory.getConfig("kamon.agent");
        config.getStringList("instrumentations").forEach(clazz -> {
            try {
                ((KamonInstrumentation) Class.forName(clazz).newInstance()).register(instrumentation);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    protected static void loadInstrumentationAPI(Instrumentation instrumentation) {
        final AgentBuilder.Default agentBuilder = new AgentBuilder.Default();

        // TODO: It should "redefine" the class instead of intercet the method calls

        agentBuilder.type(named("kamon.agent.api.instrumentation.KamonInstrumentation"))
                .transform((builder, typeDescription) -> {
                    return builder.method(ElementMatchers.any()) // TODO: redefine the fields
                            .intercept(MethodDelegation.to(KamonInstrumentation.class));
                }).installOn(instrumentation);

        agentBuilder.type(named("kamon.agent.api.instrumentation.mixin.MixinDescription"))
                .transform((builder, typeDescription) -> {
                    return builder.method(ElementMatchers.any())
                            .intercept(MethodDelegation.to(MixinDescription.class));
                }).installOn(instrumentation);
    }
}

