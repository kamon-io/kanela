package kamon.agent;


import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import javaslang.control.Option;
import kamon.agent.api.impl.instrumentation.KamonInstrumentation;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.net.URISyntaxException;
import java.util.jar.JarFile;

public class InstrumentationLoader {
    private static final Config factory = ConfigFactory.load();

    public static void load(String args, Instrumentation instrumentation) throws IOException, URISyntaxException {

        // if there isn't a path of the inspector provided by args, trying to use the kamon agent jar
        appendInterceptorToBootstrap(getJar(args), instrumentation);

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
        final AgentBuilder agentBuilder = createAgentBuilder();

        agentBuilder
                .type(ElementMatchers.named("kamon.agent.api.instrumentation.KamonInstrumentation"))
                .transform((builder, typeDescription) -> builder
                        .method(ElementMatchers.named("register"))
                        .intercept(MethodDelegation.to(KamonInstrumentation.class))
                        .method(ElementMatchers.named("withTransformer"))
                        .intercept(MethodDelegation.to(KamonInstrumentation.class))
                        .defineField("elementMatcher", Option.class, Visibility.PRIVATE))
                .installOn(instrumentation);
    }

    private static AgentBuilder createAgentBuilder() {
        // Disable a bunch of stuff and turn on redefine as the only option
        final ByteBuddy byteBuddy = new ByteBuddy().with(Implementation.Context.Disabled.Factory.INSTANCE);

        return new AgentBuilder.Default()
                .with(byteBuddy)
                .with(AgentBuilder.InitializationStrategy.NoOp.INSTANCE)
                .with(AgentBuilder.RedefinitionStrategy.REDEFINITION)
                .with(AgentBuilder.TypeStrategy.Default.REDEFINE);
    }

    /**
     * Appends the JAR file at "arg" to the bootstrap classloader search.
     *
     * @param inst instrumentation instance.
     */
    private static void appendInterceptorToBootstrap(File file, Instrumentation inst) {
        try {
            if (!file.exists()) {
                throw new IllegalStateException("The file " + file.getAbsolutePath() + " does not exist");
            }

            if (!file.isFile()) {
                throw new IllegalStateException("The file " + file.getAbsolutePath() + " is not a file");

            }
            if (!file.canRead()) {
                throw new IllegalStateException("The file " + file.getAbsolutePath() + " cannot be read");

            }
            JarFile jarFile = new JarFile(file);
            inst.appendToBootstrapClassLoaderSearch(jarFile);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static File getJar(String arg) throws URISyntaxException, IOException {
        File file;
        if (arg == null) {
            System.out.println("There isn't a path specified to the interceptor JAR as a javaagent argument. Trying to use the agent.");
            file = new File(KamonAgent.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getCanonicalFile();
        } else {
            file = new File(arg).getCanonicalFile();
        }
        return file;
    }
}

