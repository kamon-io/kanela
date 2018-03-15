package kanela.agent.bootstrap.metrics;

import java.util.Map;

public final class MetricsHandler {

    volatile static Metrics metrics = Metrics.NoOp.INSTANCE;

    public static void setMetricImplementation(Metrics metrics) {
        if(metrics != Metrics.NoOp.INSTANCE) {
            MetricsHandler.metrics = metrics;
        }
    }

    public static void incrementCounter(String name, Map<String, String> tags) {
        metrics.incrementCounter(name, tags);
    }
}
