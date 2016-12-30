package kamon.agent.circuitbreaker;

import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.util.Map;

public class CircuitBreakerTools {

    static boolean isOldGenPool(MemoryPoolMXBean bean) {
        return bean.getName().endsWith("Old Gen") || bean.getName().endsWith("Tenured Gen");
    }

    /** Returns true if memory pool name matches an young generation pool. */
    static boolean isYoungGenPool(MemoryPoolMXBean bean) {
        return bean.getName().endsWith("Eden Space");
    }


    static long getTotalUsage(Map<String, MemoryUsage> usages) {
        long sum = 0L;
        for (Map.Entry<String, MemoryUsage> e : usages.entrySet()) {
            sum += e.getValue().getUsed();
        }
        return sum;
    }

    /** Compute the max usage across all pools. */
    static long getTotalMaxUsage(Map<String, MemoryUsage> usages) {
        long sum = 0L;
        for (Map.Entry<String, MemoryUsage> e : usages.entrySet()) {
            long max = e.getValue().getMax();
            if (max > 0) {
                sum += e.getValue().getMax();
            }
        }
        return sum;
    }
}
