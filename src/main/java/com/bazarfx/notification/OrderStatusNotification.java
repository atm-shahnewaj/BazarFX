package com.bazarfx.notification;

import com.bazarfx.model.Order;
import com.bazarfx.model.OrderStatus;

public class OrderStatusNotification extends AppNotification {
    private final Order order;

    public OrderStatusNotification(Order order) {
        this.order = order;
    }

    @Override
    public String getIcon() {
        return switch (order.getStatus()) {
            case PENDING -> "\u23F3";
            case CONFIRMED -> "\u2705";
            case SHIPPED -> "\uD83D\uDE9A";
            case DELIVERED -> "\uD83C\uDF89";
            case CANCELLED -> "\u274C";
        };
    }

    @Override
    public String getMessage() {
        String shortId = order.getId().substring(0, Math.min(8, order.getId().length()));
        return "Order #" + shortId + " is now " + order.getStatus().name();
    }

    @Override
    public String getStyleClass() {
        return order.getStatus() == OrderStatus.DELIVERED ? "toast-success"
                : order.getStatus() == OrderStatus.CANCELLED ? "toast-danger"
                : "toast-info";
    }
}
