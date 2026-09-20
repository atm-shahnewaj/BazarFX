package com.bazarfx.concurrency;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import javafx.application.Platform;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Real networking + JSON parsing demo: periodically issues an HTTP GET to a public
 * exchange-rate API and parses the JSON response body with Gson, running entirely on a
 * background scheduled thread so the UI never blocks on the network call. Falls back to
 * a clearly-labelled offline message if the request fails (no internet in this sandbox,
 * DNS blocked, etc.) instead of crashing the background thread.
 */
public class ExchangeRateFetcher {
    private static ExchangeRateFetcher instance;

    private static final String API_URL = "https://open.er-api.com/v6/latest/USD";
    private static final Gson GSON = new Gson();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "exchange-rate-fetcher");
        t.setDaemon(true);
        return t;
    });

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
        scheduler.scheduleAtFixedRate(this::fetchOnce, 0, 60, TimeUnit.SECONDS);
    }

    private void fetchOnce() {
        String statusMsg;
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(API_URL))
                    .timeout(Duration.ofSeconds(6))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // Real JSON parsing: pull the "rates" object out of the response body and
            // read a few currencies of interest out of it.
            JsonObject root = GSON.fromJson(response.body(), JsonObject.class);
            JsonObject rates = root.getAsJsonObject("rates");
            double bdt = rates.get("BDT").getAsDouble();
            double eur = rates.get("EUR").getAsDouble();
            double inr = rates.get("INR").getAsDouble();
            statusMsg = String.format("Live rates (1 USD): \u09F3 %.2f | \u20AC %.3f | \u20B9 %.2f", bdt, eur, inr);
        } catch (Exception e) {
            statusMsg = "Live rates unavailable (offline) - showing last known values.";
        }
        if (onRateFetchedCallback != null) {
            String finalMsg = statusMsg;
            Platform.runLater(() -> onRateFetchedCallback.accept(finalMsg));
        }
    }

    public void stop() {
        scheduler.shutdownNow();
    }
}
