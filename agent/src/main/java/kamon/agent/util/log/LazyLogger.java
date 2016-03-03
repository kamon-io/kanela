package kamon.agent.util.log;


import javaslang.control.Match;
import kamon.agent.AgentLogger;
import org.slf4j.Logger;

import java.util.function.Supplier;

/**
 * Lazy Logger implementing {@link Logger}, which supports lazy evaluation of messages.<br>
 * The message to be logged must be inside a {@link Supplier} which will be evaluated only if the level of debug is enabled.
 */
public class LazyLogger {

    private LazyLogger(){}

    private static Boolean isTraceEnabled(final Object source) {
        return LazyLogger.logger(source).isTraceEnabled();
    }

    public static void trace(final Object source, final Supplier<String> msg) {
        if (isTraceEnabled(source)) {
            LazyLogger.logger(source).trace(msg.get());
        }
    }

    public static void trace(final Object source, final Supplier<String> msg, Throwable t) {
        if (isTraceEnabled(source)) {
            LazyLogger.logger(source).trace(msg.get(), t);
        }
    }

    private static Boolean isDebugEnabled(final Object source) {
        return LazyLogger.logger(source).isDebugEnabled();
    }

    public static void debug(final Object source, final Supplier<String> msg) {
        if (isDebugEnabled(source)) {
            LazyLogger.logger(source).debug(msg.get());
        }
    }

    public static void debug(final Object source, final Supplier<String> msg, final Throwable t) {
        if (isDebugEnabled(source)) {
            LazyLogger.logger(source).debug(msg.get(), t);
        }
    }

    private static Boolean isInfoEnabled(final Object source) {
        return LazyLogger.logger(source).isInfoEnabled();
    }

    public static void info(final Object source, final Supplier<String> msg) {
        if (isInfoEnabled(source)) {
            LazyLogger.logger(source).info(msg.get());
        }
    }

    public static void info(final Object source, final Supplier<String> msg, final Throwable t) {
        if (isInfoEnabled(source)) {
            LazyLogger.logger(source).info(msg.get(), t);
        }
    }

    private static Boolean isWarnEnabled(final Object source) {
        return LazyLogger.logger(source).isWarnEnabled();
    }

    public static void warn(final Object source, final Supplier<String> msg) {
        if (isWarnEnabled(source)) {
            LazyLogger.logger(source).warn(msg.get());
        }
    }

    public static void warn(final Object source, final Supplier<String> msg, final Throwable t) {
        if (isWarnEnabled(source)) {
            LazyLogger.logger(source).warn(msg.get(), t);
        }
    }

    private static Boolean isErrorEnabled(final Object source) {
        return  LazyLogger.logger(source).isErrorEnabled();
    }

    public static void error(final Object source, final Supplier<String> msg) {
        if (isErrorEnabled(source)) {
            LazyLogger.logger(source).error(msg.get());
        }
    }

    public static void error(final Object source, final Supplier<String> msg, final Throwable t) {
        if (isErrorEnabled(source)) {
            LazyLogger.logger(source).error(msg.get(), t);
        }
    }

    private static org.slf4j.Logger logger(final Object source) {
        return Match.of(source).whenType(Class.class).then(AgentLogger::create)
                               .whenType(String.class).then(AgentLogger::create)
                               .getOrElse(AgentLogger.create(source.getClass()));

    }
}
