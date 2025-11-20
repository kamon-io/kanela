/*
 *  ==========================================================================================
 *  Copyright © 2013-2025 The Kamon Project <https://kamon.io/>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 *  except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the
 *  License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 *  either express or implied. See the License for the specific language governing permissions
 *  and limitations under the License.
 *  ==========================================================================================
 */

package kanela.agent;

import static java.util.Comparator.comparing;
import static kanela.agent.bytebuddy.ClassPrefixMatcher.*;
import static kanela.agent.bytebuddy.ScalaCompilerClassLoaderMatcher.*;
import static net.bytebuddy.matcher.ElementMatchers.*;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.nio.file.Files;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import kanela.agent.api.instrumentation.InstrumentationBuilder;
import kanela.agent.api.instrumentation.InstrumentationBuilder.Target;
import kanela.agent.bootstrap.BootstrapInjector;
import kanela.agent.bootstrap.StatusApi;
import kanela.agent.bytebuddy.AdviceExceptionHandler;
import kanela.agent.bytebuddy.BridgeClassVisitorWrapper;
import kanela.agent.bytebuddy.ClassPrefixMatcher;
import kanela.agent.bytebuddy.IgnoredClassLoaderMatcher;
import kanela.agent.bytebuddy.MixinClassVisitorWrapper;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.agent.builder.AgentBuilder.InjectionStrategy;
import net.bytebuddy.agent.builder.AgentBuilder.RedefinitionStrategy.ResubmissionScheduler;
import net.bytebuddy.agent.builder.ResettableClassFileTransformer;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.MethodGraph;
import net.bytebuddy.dynamic.scaffold.TypeValidation;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.pool.TypePool;
import net.bytebuddy.utility.JavaModule;
import org.pmw.tinylog.Logger;

public class Kanela {
  private static volatile Instrumentation currentJvmInstrumentation;
  private static volatile ClassLoader instrumentationClassLoader;
  private static volatile ResettableClassFileTransformer currentTransformer;
  private static final String InstrumentationClassLoaderPropertyName =
      "kanela.instrumentation.classLoader";
  private static final String LoadedIndicatorPropertyName = "kanela.loaded";

  public static void premain(String args, Instrumentation instrumentation) {
    start(args, instrumentation, false);
  }

  public static void agentmain(String args, Instrumentation instrumentation) {
    start(args, instrumentation, true);
  }

  private static void start(
      String args, Instrumentation instrumentation, boolean isAttachingAtRuntime) {
    if (currentJvmInstrumentation == null) {
      currentJvmInstrumentation = instrumentation;

      try {
        instrumentationClassLoader = findInstrumentationClassloader();

        // Ensures some critical classes are loaded before we start the instrumentation,
        // so that they don't get loaded in the middle of changing some other class.
        PreInit.loadKnownRequiredClasses(instrumentationClassLoader);

        Configuration configuration = Configuration.createFrom(instrumentationClassLoader);

        if (configuration.agent().requiresBootstrapInjection()) {
          // We must do bootstrap injection before anything else because loading the modules
          // might trigger loading classes that should exist in the Boostrap ClassLoader l
          injectBootstapApiClasses(instrumentation);
        }

        // Kamon will access the StatusApi to figure out what modules are available/active
        registerModulesInStatusApi(configuration);

        Logger.info("Installing the Kanela agent");
        currentTransformer =
            installByteBuddyAgent(
                configuration,
                isAttachingAtRuntime,
                Executors.newScheduledThreadPool(1),
                instrumentation,
                instrumentationClassLoader,
                loadConfiguredModules(configuration, instrumentationClassLoader));

        Logger.info("Kanela agent installed");
        System.setProperty(LoadedIndicatorPropertyName, "true");

      } catch (Throwable e) {
        throw new RuntimeException("Failed to install the Kanela agent on this JVM", e);
      }
    }
  }

  public static void reload() {
    reload(false);
  }

  public static void reload(boolean clearRegisteredModules) {
    if (currentJvmInstrumentation != null && currentTransformer != null) {
      Logger.info("Reloading the Kanela agent");

      try {

        // Resets any instrumentation that was previously installed
        currentTransformer.reset(
            currentJvmInstrumentation, AgentBuilder.RedefinitionStrategy.RETRANSFORMATION);

        instrumentationClassLoader = findInstrumentationClassloader();
        Configuration configuration = Configuration.createFrom(instrumentationClassLoader);
        if (configuration.agent().requiresBootstrapInjection()) {
          // We must do bootstrap injection before anything else because loading the modules
          // might trigger loading classes that should exist in the Boostrap ClassLoader l
          injectBootstapApiClasses(currentJvmInstrumentation);
        }

        if (clearRegisteredModules) StatusApi.clearModules();

        // Kamon will access the StatusApi to figure out what modules are available/active
        registerModulesInStatusApi(configuration);

        // We treat reloads as if we were attaching the agent at runtime.
        boolean isAttachingAtRuntime = true;
        currentTransformer =
            installByteBuddyAgent(
                configuration,
                isAttachingAtRuntime,
                Executors.newScheduledThreadPool(1),
                currentJvmInstrumentation,
                instrumentationClassLoader,
                loadConfiguredModules(configuration, instrumentationClassLoader));

        Logger.debug("Kanela agent reloaded");
        System.setProperty(LoadedIndicatorPropertyName, "true");

      } catch (Throwable e) {
        throw new RuntimeException("Failed to reload the Kanela agent on this JVM", e);
      }
    } else {
      Logger.error("Cannot reload Kanela because it was not installed properly in the current JVM");
    }
  }

  /**
   * Figures out what ClassLoader should be used for finding instrumentation classes. Most of the
   * time all the Instrumentation classes are loaded from the system classloader but in some cases,
   * like when running from SBT or PlayFramework, the instrumentation classes are isolated in a
   * different ClassLoader that we can "expose" to Kanela through the
   * `kanela.instrumentation.classLoader` system property. (Yes, we know that system properties are
   * not meant to be used like this).
   */
  private static ClassLoader findInstrumentationClassloader() {
    Object customClassLoader = System.getProperties().get(InstrumentationClassLoaderPropertyName);
    if (customClassLoader != null && customClassLoader instanceof ClassLoader) {
      System.getProperties().remove(InstrumentationClassLoaderPropertyName);
      return (ClassLoader) customClassLoader;
    } else {
      return ClassLoader.getSystemClassLoader();
    }
  }

  static class ModuleTargets {
    private final Configuration.Module moduleConfig;
    private final List<InstrumentationBuilder.Target> targets;

    ModuleTargets(Configuration.Module moduleConfig, List<InstrumentationBuilder.Target> targets) {
      this.moduleConfig = moduleConfig;
      this.targets = targets;
    }

    public Configuration.Module moduleConfig() {
      return moduleConfig;
    }

    public List<InstrumentationBuilder.Target> targets() {
      return targets;
    }
  }

  private static List<ModuleTargets> loadConfiguredModules(
      Configuration configuration, ClassLoader instrumentationClassLoader) {

    return configuration.modules().stream()
        .filter(Configuration.Module::enabled)
        .sorted(comparing(Configuration.Module::order).thenComparing(Configuration.Module::name))
        .map(
            moduleConfig -> {
              Stream<InstrumentationBuilder.Target> targets =
                  moduleConfig.instrumentations().stream()
                      .flatMap(
                          instrumentationName -> {
                            try {
                              Object instrumentation =
                                  Class.forName(
                                          instrumentationName, true, instrumentationClassLoader)
                                      .getConstructor()
                                      .newInstance();

                              return ((InstrumentationBuilder) instrumentation).targets().stream();

                            } catch (Exception e) {
                              Logger.error(e, "Failed to instantiate {}", instrumentationName);
                              return Stream.empty();
                            }
                          });

              return new ModuleTargets(moduleConfig, targets.collect(Collectors.toList()));
            })
        .collect(Collectors.toList());
  }

  private static void registerModulesInStatusApi(Configuration configuration) {
    configuration
        .modules()
        .forEach(
            moduleConfig ->
                StatusApi.registerModule(
                    moduleConfig.key(),
                    moduleConfig.name(),
                    moduleConfig.description().orElse(""),
                    moduleConfig.enabled()));
  }

  private static void injectBootstapApiClasses(Instrumentation instrumentation) {
    // These classes should be present in the Bootstrap ClassLoader so that they can
    // be used when instrumenting classes provided by the JDK
    BootstrapInjector.inject(
        instrumentation,
        Arrays.asList(
            "kanela.agent.bootstrap.ContextApi",
            "kanela.agent.bootstrap.ContextApiImplementation",
            "kanela.agent.bootstrap.NoopContextApiImplementation"));
  }

  private static ResettableClassFileTransformer installByteBuddyAgent(
      Configuration config,
      boolean isAttachingAtRuntime,
      ScheduledExecutorService executor,
      Instrumentation instrumentation,
      ClassLoader instrumentationClassLoader,
      List<ModuleTargets> modules)
      throws Exception {

    executor.submit(() -> Thread.currentThread().setName("kanela-scheduled-executor"));

    ConcurrentMap<? super ClassLoader, TypePool.CacheProvider> caches = new ConcurrentHashMap<>();

    // Caches must be cleared manually to avoid memory leaks and most of the information in
    // the type pools can be safely discarded after the instrumentation is done.
    executor.scheduleAtFixedRate(
        () -> {
          caches.forEach((key, value) -> value.clear());
        },
        10,
        10,
        TimeUnit.SECONDS);

    ByteBuddy byteBuddy =
        new ByteBuddy()
            .with(TypeValidation.DISABLED)
            .with(MethodGraph.Compiler.ForDeclaredMethods.INSTANCE);

    ResubmissionScheduler resubmission =
        new ResubmissionScheduler.WithFixedDelay(executor, 10, TimeUnit.SECONDS);

    AgentBuilder.PoolStrategy poolStrategy =
        new AgentBuilder.PoolStrategy.WithTypePoolCache.Simple(caches);

    AgentBuilder agentBuilder =
        new AgentBuilder.Default(byteBuddy)
            .with(poolStrategy)
            .with(new InstrumentationEventsLogger());

    List<String> allBootstrapPrefixes =
        modules.stream()
            .flatMap(
                (module) ->
                    module
                        .moduleConfig()
                        .bootstrapPrefixes()
                        .orElse(Collections.emptyList())
                        .stream())
            .collect(Collectors.toList());

    AgentBuilder.Ignored agentBuilderWithIgnores =
        agentBuilder
            .ignore(any(), isExtensionClassLoader())
            .or(any(), new IgnoredClassLoaderMatcher());

    if (allBootstrapPrefixes.isEmpty()) {
      agentBuilder = agentBuilderWithIgnores.or(any(), isBootstrapClassLoader());
    } else {
      File bootstrapClassesDir = Files.createTempDirectory("kanela-bootstrap-classes").toFile();
      agentBuilder =
          agentBuilderWithIgnores
              .or(not(classPrefix(allBootstrapPrefixes)), isBootstrapClassLoader())
              .with(
                  new InjectionStrategy.UsingInstrumentation(instrumentation, bootstrapClassesDir));
    }

    if (isAttachingAtRuntime || config.agent().requiresBootstrapInjection()) {
      agentBuilder =
          agentBuilder
              .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
              .withResubmission(resubmission)
              .resubmitOnError();
    }

    // This is separate from ByteBuddy's own TypePool. Here we only use it to verify if
    // any targets should be skipped due to ClasspathFilter matches without triggering
    // class loading on the target classes.
    TypePool classpathFilterTypePool = TypePool.Default.of(instrumentationClassLoader);

    for (ModuleTargets module : modules) {
      Configuration.Module moduleConfig = module.moduleConfig();
      ClassPrefixMatcher matchesModuleExcludes =
          classPrefix(moduleConfig.excludedPrefixes().orElse(Collections.emptyList()));

      Junction<TypeDescription> matchesModulePrefixes = classPrefix(moduleConfig.prefixes());
      if (moduleConfig.bootstrapPrefixes().isPresent()) {
        matchesModulePrefixes =
            matchesModulePrefixes.or(classPrefix(moduleConfig.bootstrapPrefixes().get()));
      }

      for (Target target : module.targets()) {

        if (!target.classpathFilters().isEmpty()) {
          boolean passesAllClasspathFilters =
              target.classpathFilters().stream()
                  .allMatch(
                      filter -> {
                        TypePool.Resolution typeResolution =
                            classpathFilterTypePool.describe(filter.className());
                        boolean classExists = typeResolution.isResolved();
                        boolean allExpectedMethodsExist = true;

                        if (classExists && !filter.expectedMethodNames().isEmpty()) {
                          TypeDescription typeDescription = typeResolution.resolve();
                          allExpectedMethodsExist =
                              filter.expectedMethodNames().stream()
                                  .allMatch(
                                      methodName ->
                                          typeDescription
                                                  .getDeclaredMethods()
                                                  .filter(named(methodName))
                                                  .size()
                                              > 0);
                        }

                        return classExists && allExpectedMethodsExist;
                      });

          if (!passesAllClasspathFilters) {
            Logger.debug(
                "Skipping target matching [{}] because it doesn't pass the classpath filters",
                target.typeMatcher());
            continue;
          }
        }

        ElementMatcher<TypeDescription> targetTypeMatcher =
            not(matchesModuleExcludes).and(matchesModulePrefixes.and(target.typeMatcher()));

        for (Class<?> mixinClass : target.mixinClasses()) {
          AgentBuilder.Transformer mixinTranformer =
              new AgentBuilder.Transformer() {

                @Override
                public DynamicType.Builder transform(
                    DynamicType.Builder<?> builder,
                    TypeDescription typeDescription,
                    ClassLoader classloader,
                    JavaModule module,
                    ProtectionDomain protectionDomain) {

                  TypeDescription[] interfaces =
                      Arrays.asList(mixinClass.getInterfaces()).stream()
                          .distinct()
                          .map(TypeDescription.ForLoadedType::of)
                          .toArray(size -> new TypeDescription[size]);

                  StatusApi.onTypeTransformed(moduleConfig.key(), typeDescription.getName());
                  return builder
                      .implement(interfaces)
                      .visit(new MixinClassVisitorWrapper(mixinClass, instrumentationClassLoader));
                }
              };

          agentBuilder = agentBuilder.type(targetTypeMatcher).transform(mixinTranformer);
        }

        for (Class<?> bridgeInterface : target.bridgeInterfaces()) {
          AgentBuilder.Transformer bridgeTransformer =
              new AgentBuilder.Transformer() {

                @Override
                public DynamicType.Builder transform(
                    DynamicType.Builder<?> builder,
                    TypeDescription typeDescription,
                    ClassLoader classloader,
                    JavaModule module,
                    ProtectionDomain protectionDomain) {

                  StatusApi.onTypeTransformed(moduleConfig.key(), typeDescription.getName());
                  return builder
                      .implement(TypeDescription.ForLoadedType.of(bridgeInterface))
                      .visit(new BridgeClassVisitorWrapper(bridgeInterface));
                }
              };

          agentBuilder = agentBuilder.type(targetTypeMatcher).transform(bridgeTransformer);
        }

        for (Target.Interceptor interceptor : target.interceptors()) {
          AgentBuilder.Transformer interceptorTransformer =
              new AgentBuilder.Transformer() {

                @Override
                public DynamicType.Builder transform(
                    DynamicType.Builder<?> builder,
                    TypeDescription typeDescription,
                    ClassLoader classloader,
                    JavaModule module,
                    ProtectionDomain protectionDomain) {

                  MethodDelegation methodDelegation =
                      interceptor.eitherObject() != null
                          ? MethodDelegation.to(interceptor.eitherObject())
                          : MethodDelegation.to(interceptor.orImplementation());

                  StatusApi.onTypeTransformed(moduleConfig.key(), typeDescription.getName());
                  return builder.method(interceptor.method()).intercept(methodDelegation);
                }
              };

          agentBuilder = agentBuilder.type(targetTypeMatcher).transform(interceptorTransformer);
        }

        for (Target.Advice advice : target.advisors()) {
          AgentBuilder.Transformer adviceTransformer =
              new AgentBuilder.Transformer.ForAdvice()
                  .advice(advice.method(), advice.implementationClassName())
                  .withExceptionHandler(AdviceExceptionHandler.instance())
                  .include(instrumentationClassLoader);

          agentBuilder =
              agentBuilder
                  .type(targetTypeMatcher)
                  .transform(
                      (builder, typeDescription, classloader, javaModule, protectionDomain) -> {
                        StatusApi.onTypeTransformed(moduleConfig.key(), typeDescription.getName());
                        return adviceTransformer.transform(
                            builder, typeDescription, classloader, javaModule, protectionDomain);
                      });
        }

        for (AgentBuilder.Transformer transformer : target.transformers()) {
          agentBuilder =
              agentBuilder
                  .type(targetTypeMatcher)
                  .transform(
                      (builder, typeDescription, classloader, javaModule, protectionDomain) -> {
                        StatusApi.onTypeTransformed(moduleConfig.key(), typeDescription.getName());
                        return transformer.transform(
                            builder, typeDescription, classloader, javaModule, protectionDomain);
                      });
        }
      }
    }

    return agentBuilder.installOn(instrumentation);
  }

  public static class InstrumentationEventsLogger extends AgentBuilder.Listener.Adapter {

    public void onTransformation(
        TypeDescription typeDescription,
        ClassLoader classLoader,
        JavaModule module,
        boolean loaded,
        DynamicType dynamicType) {

      Logger.debug("Transformed type [{}]", typeDescription.getName());
    }

    public void onIgnored(
        TypeDescription typeDescription,
        ClassLoader classLoader,
        JavaModule module,
        boolean loaded) {

      Logger.trace("Ignored type [{}]", typeDescription.getName());
    }

    public void onError(
        String typeName,
        ClassLoader classLoader,
        JavaModule module,
        boolean loaded,
        Throwable throwable) {

      Logger.error(throwable, "Failed to transform type [{}]", typeName);
    }
  }
}
