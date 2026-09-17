package com.bazarfx.concurrency;

import javafx.application.Platform;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

/**
 * Long-lived daemon Thread that polls a local event queue every few seconds for new
 * orders, status changes, incoming reviews, and wishlist price-drops, then hands each
 * event to the UI safely via Platform.runLater() — the boundary between a worker thread
 * and the JavaFX Application Thread.
 */
public class NotificationPoller {

    public static class NotificationEvent {
        public final String message;
        public NotificationEvent(String message) { this.message = message; }
    }

    private final ConcurrentLinkedQueue<NotificationEvent> queue = new ConcurrentLinkedQueue<>();
    private final Consumer<NotificationEvent> onEvent;
    private volatile boolean running = false;
    private Thread pollerThread;

    public NotificationPoller(Consumer<NotificationEvent> onEvent) {
        this.onEvent = onEvent;
    }

    /** Any part of the app (services, other background threads) can push an event here. */
    public void publish(NotificationEvent event) {
        queue.add(event);
    }

    public void start(long pollIntervalMillis) {
        running = true;
        pollerThread = new Thread(() -> {
            while (running) {
                NotificationEvent event;
                while ((event = queue.poll()) != null) {
                    NotificationEvent finalEvent = event;
                    Platform.runLater(() -> onEvent.accept(finalEvent));
                }
                try {
                    Thread.sleep(pollIntervalMillis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    running = false;
                }
            }
        }, "notification-poller");
        pollerThread.setDaemon(true);
        pollerThread.start();
    }

    public void stop() {
        running = false;
        if (pollerThread != null) pollerThread.interrupt();
    }
}
