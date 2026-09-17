package com.bazarfx.concurrency;

import com.bazarfx.service.ReportService;
import javafx.application.Platform;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Runs ReportService's ExecutorService/Future-based marketplace report off the JavaFX
 * Application Thread and marshals the finished result (or a progress indicator, or an
 * error) back onto the UI thread. This is the "outer" background thread the Reports
 * screen talks to; ReportService itself owns the inner worker pool that computes the
 * three breakdowns in parallel.
 */
public class ReportGenerator {

    private final ExecutorService driver = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "report-generator");
        t.setDaemon(true);
        return t;
    });

    private final ReportService reportService;

    public ReportGenerator(ReportService reportService) {
        this.reportService = reportService;
    }

    public void generateAsync(Runnable onStart,
                               Consumer<ReportService.MarketplaceReport> onComplete,
                               Consumer<Exception> onError) {
        if (onStart != null) Platform.runLater(onStart);

        CompletableFuture.runAsync(() -> {
            try {
                ReportService.MarketplaceReport report = reportService.generateMarketplaceReport();
                Platform.runLater(() -> onComplete.accept(report));
            } catch (Exception e) {
                Platform.runLater(() -> onError.accept(e));
            }
        }, driver);
    }

    public void shutdown() {
        driver.shutdown();
    }
}
