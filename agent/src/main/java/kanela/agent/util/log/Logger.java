/*
 * =========================================================================================
 * Copyright © 2013-2022 the kamon project <http://kamon.io/>
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

package kanela.agent.util.log;

import io.vavr.control.Try;
import kanela.agent.bootstrap.log.LoggerHandler;
import kanela.agent.bootstrap.log.LoggerProvider;
import kanela.agent.util.conf.KanelaConfiguration;
import lombok.val;
import org.pmw.tinylog.Configurator;
import org.pmw.tinylog.labelers.TimestampLabeler;
import org.pmw.tinylog.policies.SizePolicy;
import org.pmw.tinylog.policies.StartupPolicy;
import org.pmw.tinylog.writers.RollingFileWriter;

import java.util.function.Supplier;

/**
 * Lazy Logger implementing {@link org.pmw.tinylog.Logger}, which supports lazy evaluation of messages.<br>
 * The message to be logged must be inside a {@link Supplier} which will be evaluated only if the level of debug is enabled.
 */
public class Logger {

    static { configureLogger(KanelaConfiguration.instance()); }

    public static void configureLogger(KanelaConfiguration config) {
        Try.run(() -> {
            val configurator = Configurator
                .fromResource("kanela-log.properties")
                .maxStackTraceElements(400) // stack traces
                .level(config.getLogLevel());

            if(config.isDebugMode()) {
                configurator.addWriter(new RollingFileWriter("kanela-agent.log", 2, true,
                    new TimestampLabeler(), new StartupPolicy(), new SizePolicy(10 * 1024)));
            }

            configurator.activate();

        }).andThen(() -> {
            //sets the logger provider in order to be able to access from advisors/interceptors
            LoggerHandler.setLoggerProvider(new LoggerProvider() {
                public void error(String msg, Throwable t) { Logger.error(() -> msg, t); }
                public void info(String msg) {
                    Logger.info(() -> msg);
                }
            });
        }).getOrElseThrow((error) -> new RuntimeException("Error when trying to load configuration: " + error.getMessage()));
    }

    private Logger(){}

    public static void debug(final Supplier<String> msg) { org.pmw.tinylog.Logger.debug(msg.get());}
    public static void trace(final Supplier<String> msg) { org.pmw.tinylog.Logger.trace(msg.get());}
    public static void info(final Supplier<String> msg) { org.pmw.tinylog.Logger.info(msg.get()); }
    public static void info(final Supplier<String> msg, final Throwable t) { org.pmw.tinylog.Logger.info(t, msg.get());}
    public static void warn(final Supplier<String> msg) { org.pmw.tinylog.Logger.warn(msg.get());}
    public static void warn(final Supplier<String> msg, final Throwable t) { org.pmw.tinylog.Logger.warn(t, msg.get());}
    public static void error(final Supplier<String> msg) { org.pmw.tinylog.Logger.error(msg.get()); }
    public static void error(final Supplier<String> msg, final Throwable t) { org.pmw.tinylog.Logger.error(t, msg.get());}
}
