package com.bazarfx.concurrency;

import javafx.application.Platform;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class ExchangeRateFetcher {
    private static ExchangeRateFetcher instance;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private Consumer<String> onRateFetchedCallback;

    private ExchangeRateFetcher() {}

    public static synchronized ExchangeRateFetcher getInstance() {
        if (instance == null) instance = new ExchangeRateFetcher();
        return instance;
    }

    public void setOnRateFetchedCallback(Consumer<String> callback) {
        this.onRateFetchedCallback = callback;
    }

    public void start() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                Thread.sleep(800);
                String statusMsg = "Live API Rates: USD (0.0083) | EUR (0.0076) | INR (0.70)";
                if (onRateFetchedCallback != null) {
                    Platform.runLater(() -> onRateFetchedCallback.accept(statusMsg));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, 0, 15, TimeUnit.SECONDS);
    }

    public void stop() {
        scheduler.shutdownNow();
    }
}
