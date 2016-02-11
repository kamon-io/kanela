package kamon.agent;


import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import kamon.agent.api.instrumentation.KamonInstrumentation1;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.net.URISyntaxException;
import java.util.jar.JarFile;

public class InstrumentationLoader {
    private static final Config factory = ConfigFactory.load();
    private static final Logger logger = LoggerFactory.getLogger(InstrumentationLoader.class);

    public static void load(String args, Instrumentation instrumentation) throws IOException, URISyntaxException {

        // if there isn't a path of the inspector provided by args, trying to use the kamon agent jar
        appendInterceptorToBootstrap(getJar(args), instrumentation);

        loadInstrumentationAPI(instrumentation);

        Config config = factory.getConfig("kamon.agent");
        config.getStringList("instrumentations").forEach(clazz -> {
            try {
                ((KamonInstrumentation1) Class.forName(clazz).newInstance()).register(instrumentation);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    protected static void loadInstrumentationAPI(Instrumentation instrumentation) {
        final AgentBuilder agentBuilder = createAgentBuilder(instrumentation);

        agentBuilder
                .type(ElementMatchers.named("kamon.agent.api.instrumentation.KamonInstrumentation"))
                .transform((builder, typeDescription) -> builder
//                                .implement(KamonInstrumentation.class)
                        .method(ElementMatchers.named("register"))
                        .intercept(MethodDelegation.to(KamonInstrumentation1.class))
                        .method(ElementMatchers.named("withTransformer"))
                        .intercept(MethodDelegation.to(KamonInstrumentation1.class))
//                        .defineField("elementMatcher", Option.class, Visibility.PRIVATE)
//                        .defineField("mixins", List.class, Visibility.PRIVATE)
//                        .defineField("transformers", Option.class, Visibility.PRIVATE)
//                        .defineField("typePool", TypePool.class, PROTECTED, STATIC, FINAL)
//                        .defineField("NotDeclaredByObject", ElementMatcher.Junction.class, PROTECTED, STATIC, FINAL)
//                        .defineField("NotTakesArguments", ElementMatcher.Junction.class, PROTECTED, STATIC, FINAL)
                ).installOn(instrumentation);
    }

    private static AgentBuilder createAgentBuilder(Instrumentation instrumentation) {
        // Disable a bunch of stuff and turn on redefine as the only option
        final ByteBuddy byteBuddy = new ByteBuddy(); // .with(Implementation.Context.Disabled.Factory.INSTANCE);

        return new AgentBuilder.Default()
//                .enableBootstrapInjection(new File("/var/agent"), instrumentation)
                .with(byteBuddy)
                .with(AgentBuilder.InitializationStrategy.SelfInjection.EAGER)
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
            logger.info(String.format("Appending %s to Bootstrap Class Loader", file.getName()));
            inst.appendToBootstrapClassLoaderSearch(jarFile);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static File getJar(String arg) throws URISyntaxException, IOException {
        File file;
        if (arg == null) {
            logger.warn("There isn't a path specified to the interceptor JAR as argument. Trying to use the kamon agent's jar.");

            /*
            Esto rompe porque después aparecen tipos definidos en 2 class loader distinto:
            *******************
            Error message: loader constraint violation: when resolving interface method
            "kamon.agent.libs.net.bytebuddy.agent.builder.AgentBuilder.with(Lkamon/agent/libs/net/bytebuddy/agent/builder/AgentBuilder$InitializationStrategy;)Lkamon/agent/libs/net/bytebuddy/agent/builder/AgentBuilder;"
            the class loader (instance of sun/misc/Launcher$AppClassLoader) of the current class, kamon/agent/InstrumentationLoader, and the class loader (instance of <bootloader>) for the method's defining class,
            kamon/agent/libs/net/bytebuddy/agent/builder/AgentBuilder, have different Class objects for the type kamon/agent/libs/net/bytebuddy/agent/builder/AgentBuilder$InitializationStrategy used in the signature
            *******************
             */
            file = new File(KamonAgent.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getCanonicalFile();
        } else {
            file = new File(arg).getCanonicalFile();
        }
        return file;
    }
}

