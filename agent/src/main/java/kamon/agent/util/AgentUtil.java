package kamon.agent.util;

import java.util.function.Consumer;

public class AgentUtil {

    public static void withTimeSpent(final Runnable thunk, Consumer<Long> timeSpent) {
        long startMillis = System.currentTimeMillis();
        thunk.run();
        timeSpent.accept(System.currentTimeMillis() - startMillis);
    }
}
