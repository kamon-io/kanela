package kamon.agent.util.log;


import kamon.agent.AgentLogger;
import org.slf4j.Logger;

import java.util.function.Supplier;

import static javaslang.API.*;
import static javaslang.Predicates.*;

/**
 * Lazy Logger implementing {@link Logger}, which supports lazy evaluation of messages.<br>
 * The message to be logged must be inside a {@link Supplier} which will be evaluated only if the level of debug is enabled.
 */
public class LazyLogger {

    private final Logger underlying;

    private LazyLogger(Logger underlying) {
        this.underlying = underlying;
    }

    private Boolean isTraceEnabled() {
        return underlying.isTraceEnabled();
    }

    public void trace(Supplier<String> msg) {
        if (isTraceEnabled()) {
            underlying.trace(msg.get());
        }
    }

    public void trace(Supplier<String> msg, Throwable t) {
        if (isTraceEnabled()) {
            underlying.trace(msg.get(), t);
        }
    }

    private Boolean isDebugEnabled() {
        return underlying.isDebugEnabled();
    }

    public void debug(Supplier<String> msg) {
        if (isDebugEnabled()) {
            underlying.debug(msg.get());
        }
    }

    public void debug(Supplier<String> msg, Throwable t) {
        if (isDebugEnabled()) {
            underlying.debug(msg.get(), t);
        }
    }

    private Boolean isInfoEnabled() {
        return underlying.isInfoEnabled();
    }

    public void info(Supplier<String> msg) {
        if (isInfoEnabled()) {
            underlying.info(msg.get());
        }
    }

    public void info(Supplier<String> msg, Throwable t) {
        if (isInfoEnabled()) {
            underlying.info(msg.get(), t);
        }
    }

    private Boolean isWarnEnabled() {
        return underlying.isWarnEnabled();
    }

    public void warn(Supplier<String> msg) {
        if (isWarnEnabled()) {
            underlying.warn(msg.get());
        }
    }

    public void warn(Supplier<String> msg, Throwable t) {
        if (isWarnEnabled()) {
            underlying.warn(msg.get(), t);
        }
    }

    private Boolean isErrorEnabled() {
        return underlying.isErrorEnabled();
    }

    public void error(Supplier<String> msg) {
        if (isErrorEnabled()) {
            underlying.error(msg.get());
        }
    }

    public void error(Supplier<String> msg, Throwable t) {
        if (isErrorEnabled()) {
            underlying.error(msg.get(), t);
        }
    }

    public  static LazyLogger create(final Object source) {
        return Match(source).of(Case(instanceOf(Class.class), logger -> new LazyLogger(AgentLogger.create(logger))),
                Case(instanceOf(String.class), logger -> new LazyLogger(AgentLogger.create(logger))),
                Case($(), o -> new LazyLogger(AgentLogger.create(source.getClass())) ));
    }
}
