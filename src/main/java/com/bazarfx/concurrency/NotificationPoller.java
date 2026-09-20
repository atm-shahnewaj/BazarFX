package com.bazarfx.concurrency;

import com.bazarfx.notification.AppNotification;
import javafx.application.Platform;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

/**
 * Long-lived daemon Thread that polls a local event queue every few seconds for new
 * orders, status changes, incoming chat messages, and new listings, then hands each
 * event to the UI safely via Platform.runLater() - the boundary between a worker thread
 * and the JavaFX Application Thread. Events are typed as the polymorphic AppNotification
 * base class so any new notification kind "just works" without touching this class.
 */
public class NotificationPoller {

    private final ConcurrentLinkedQueue<AppNotification> queue = new ConcurrentLinkedQueue<>();
    private final Consumer<AppNotification> onEvent;
    private volatile boolean running = false;
    private Thread pollerThread;

    public NotificationPoller(Consumer<AppNotification> onEvent) {
        this.onEvent = onEvent;
    }

    /** Any part of the app (services, other background threads) can push an event here. */
    public void publish(AppNotification event) {
        queue.add(event);
    }

    public void start(long pollIntervalMillis) {
        running = true;
        pollerThread = new Thread(() -> {
            while (running) {
                AppNotification event;
                while ((event = queue.poll()) != null) {
                    AppNotification finalEvent = event;
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
