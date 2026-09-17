package com.bazarfx.concurrency;

import com.bazarfx.model.Order;
import com.bazarfx.service.OrderService;
import javafx.application.Platform;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Advances every non-terminal order one step (Pending -> Confirmed -> Shipped -> Delivered)
 * on a fixed schedule via ScheduledExecutorService, so buyers can watch an order "move"
 * during a live demo without any manual action. UI updates are marshalled back onto the
 * JavaFX Application Thread with Platform.runLater().
 */
public class OrderStatusSimulator {

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "order-status-simulator");
        t.setDaemon(true);
        return t;
    });

    private final OrderService orderService;
    private final Consumer<Order> onStatusChanged;

    public OrderStatusSimulator(OrderService orderService, Consumer<Order> onStatusChanged) {
        this.orderService = orderService;
        this.onStatusChanged = onStatusChanged;
    }

    /** Ticks every {@code intervalSeconds}, advancing every in-progress order by one status. */
    public void start(long intervalSeconds) {
        scheduler.scheduleAtFixedRate(this::tick, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
    }

    private void tick() {
        List<Order> inProgress = orderService.ordersInProgress();
        for (Order order : inProgress) {
            orderService.advanceOrder(order);
            if (onStatusChanged != null) {
                Platform.runLater(() -> onStatusChanged.accept(order));
            }
        }
    }

    public void stop() {
        scheduler.shutdown();
    }
}
