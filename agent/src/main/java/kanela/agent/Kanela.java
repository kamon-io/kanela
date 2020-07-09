/*
 * =========================================================================================
 * Copyright © 2013-2019 the kamon project <http://kamon.io/>
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

package kanela.agent;

import io.vavr.collection.List;
import kanela.agent.api.instrumentation.listener.InstrumentationRegistryListener;
import kanela.agent.api.instrumentation.registry.ClassRegistry;
import kanela.agent.builder.KanelaFileTransformer;
import kanela.agent.circuitbreaker.SystemThroughputCircuitBreaker;
import kanela.agent.reinstrument.Reinstrumenter;
import kanela.agent.util.BootstrapInjector;
import kanela.agent.util.banner.KanelaBanner;
import kanela.agent.util.classloader.InstrumentationClassPath;
import kanela.agent.util.classloader.PreInitializeClasses;
import kanela.agent.util.conf.KanelaConfiguration;
import kanela.agent.util.jvm.OldGarbageCollectorListener;
import kanela.agent.util.log.Logger;
import lombok.val;

import java.lang.instrument.Instrumentation;

import static kanela.agent.util.Execution.runWithTimeSpent;

public final  class Kanela {

  private static final String loadedPropertyName = "kanela.loaded";
  private static volatile Instrumentation instrumentation;
  private static volatile List<KanelaFileTransformer> installedTransformers = List.empty();

  /**
   * Entry point when the Kanela agent is added with the -javaagent command line option.
   */
  public static void premain(final String args, final Instrumentation instrumentation) {
      Kanela.start(args, instrumentation, false);
  }

  /**
   * Entry point when the Kanela agent is attached at runtime.
   */
  public static void agentmain(final String args, final Instrumentation instrumentation) {
      Kanela.start(args, instrumentation, true);
  }

  /**
   * Scans the instrumentation class path for modules and installs them on the JVM.
   */
  public static void start(final String arguments, final Instrumentation instrumentation, boolean isRuntimeAttach) {

      // This ensures that we will not load Kanela more than once on the same JVM.
      if(Kanela.instrumentation == null) {
          // We keep the reference in case we will need to reload the agent.
          Kanela.instrumentation = instrumentation;

          runWithTimeSpent(() -> {
              InstrumentationClassPath.build().use(instrumentationClassLoader -> {
                  PreInitializeClasses.preInitializeClasses(instrumentationClassLoader);

                  BootstrapInjector.injectJar(instrumentation, "bootstrap");

                  val configuration = KanelaConfiguration.from(instrumentationClassLoader);
                  Logger.configureLogger(configuration);

                  if (isRuntimeAttach) configuration.runtimeAttach();
                  KanelaBanner.show(configuration);

                  ClassRegistry.attach(instrumentation, configuration.getClassRegistryConfig());

                  installedTransformers = InstrumentationLoader.load(instrumentation, instrumentationClassLoader, configuration);
                  Reinstrumenter.attach(instrumentation, configuration, installedTransformers);
                  OldGarbageCollectorListener.attach(configuration.getOldGarbageCollectorConfig());
                  SystemThroughputCircuitBreaker.attach(configuration.getCircuitBreakerConfig());
                  updateLoadedSystemProperty();
              });
          });
      }
  }

  /**
   * Removes all instrumentation modules already applied by Kanela and re-scans the instrumentation class path for
   * modules to apply them on the current JVM.
   */
  public static void reload() {
    Kanela.reload(false);
  }

  /**
   * Removes all instrumentation modules already applied by Kanela and re-scans the instrumentation class path for
   * modules to apply them on the current JVM.
   */
  public static void reload(boolean clearRegistry) {

    // We will only proceed to reload if Kanela was properly started already.
    if(Kanela.instrumentation != null) {
        if(clearRegistry)
          InstrumentationRegistryListener.instance().clear();

        InstrumentationClassPath.build().use(instrumentationClassLoader -> {
            installedTransformers.forEach(transformer -> instrumentation.removeTransformer(transformer.getClassFileTransformer()));
            installedTransformers = List.empty();

            val configuration = KanelaConfiguration.from(instrumentationClassLoader);
            Logger.configureLogger(configuration);
            installedTransformers = InstrumentationLoader.load(instrumentation, instrumentationClassLoader, configuration);
        });
    }
  }

  /**
   * Sets a System property indicating that Kanela has been loaded. That property is meant to be used by external
   * libraries that might want to check whether Kanela was loaded or not.
   */
  private static void updateLoadedSystemProperty() {
    System.setProperty(loadedPropertyName, "true");
  }
}
