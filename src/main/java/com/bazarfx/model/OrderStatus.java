package com.bazarfx.model;

public enum OrderStatus {
    PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED;

    /** Returns the next status in the simulated pipeline, or the same status if terminal. */
    public OrderStatus next() {
        return switch (this) {
            case PENDING -> CONFIRMED;
            case CONFIRMED -> SHIPPED;
            case SHIPPED -> DELIVERED;
            default -> this;
        };
    }

    public boolean isTerminal() {
        return this == DELIVERED || this == CANCELLED;
    }
}
