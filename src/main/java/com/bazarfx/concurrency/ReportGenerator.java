package com.bazarfx.concurrency;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

public class ReportGenerator {
    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    public Future<Map<String, Object>> generateMarketplaceReportAsync() {
        return executor.submit(() -> {
            Thread.sleep(1200); // Simulate background calculation
            Map<String, Object> metrics = new HashMap<>();
            metrics.put("totalVolumeBdt", 106700.0);
            metrics.put("activeListingsCount", 3);
            metrics.put("completedOrdersCount", 5);
            return metrics;
        });
    }

    public void shutdown() {
        executor.shutdown();
    }
}
