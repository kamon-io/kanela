package kamon.agent.util;

import kamon.agent.util.log.LazyLogger;

import java.util.function.Consumer;

import static java.text.MessageFormat.format;

public class AgentUtil {

    public static void withTimeSpent(final Runnable thunk, Consumer<Long> timeSpent) {
        long startMillis = System.currentTimeMillis();
        thunk.run();
        timeSpent.accept(System.currentTimeMillis() - startMillis);
    }

    public static void withTimeLogging(final Runnable thunk, String message) {
        withTimeSpent(thunk, (timeSpent) -> LazyLogger.info(() -> format("{0} {1} ms",message, timeSpent)));
    }
}
