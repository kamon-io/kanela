/*
 * =========================================================================================
 * Copyright © 2013-2018 the kamon project <http://kamon.io/>
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

package kanela.agent.bootstrap.log;

/**
 * {@code LoggerHandler} provides methods in order call the logger inside of instrumented bytecode.
 *
 * <p>{@code LoggerHandler} avoids tight coupling with the concrete trace API through the {@link
 * LoggerHandler} interface.
 *
 * <p>Both {@link LoggerHandler} and {@link LoggerProvider} are loaded by the bootstrap classloader
 * so that they can be used from classes loaded by the bootstrap classloader. A concrete
 * implementation of {@link LoggerProvider} will be loaded by the system classloader.
 *
 * <p>{@code LoggerHandler} is implemented as a static class to allow for easy and fast use from
 * instrumented bytecode.
 *
 * @since 0.12
 */
public final class LoggerHandler {

    private static LoggerProvider loggerProvider = LoggerProvider.NoOp.INSTANCE;

    private LoggerHandler() {}

    public static void setLoggerProvider(LoggerProvider loggerProvider) {
        if(loggerProvider != LoggerProvider.NoOp.INSTANCE) {
            LoggerHandler.loggerProvider = loggerProvider;
        }
    }

    public static void error(String msg, Throwable t) { loggerProvider.error(msg, t); }
    public static void info(String msg) { loggerProvider.info(msg);}
}
