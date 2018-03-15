package kanela.agent.bootstrap.metrics;

import java.util.Map;

public interface Metrics {
    void incrementCounter(String name, Map<String, String> tags);

    enum NoOp implements Metrics {

        INSTANCE;

        @Override
        public void incrementCounter(String name, Map<String, String> tags) {}
    }
}
