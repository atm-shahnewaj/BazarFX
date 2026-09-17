package com.bazarfx.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class Order {
    private String id;
    private String buyerId;
    private String sellerId;
    private List<OrderItem> items = new ArrayList<>();
    private double totalPrice;
    private OrderStatus status = OrderStatus.PENDING;
    private List<StatusChange> statusHistory = new ArrayList<>();
    private long createdAt;
    private long lastStatusChangeAt;

    public Order() {}

    public Order(String id, String buyerId, String sellerId, List<OrderItem> items, double totalPrice) {
        this.id = id;
        this.buyerId = buyerId;
        this.sellerId = sellerId;
        this.items = items;
        this.totalPrice = totalPrice;
        this.createdAt = Instant.now().toEpochMilli();
        this.lastStatusChangeAt = this.createdAt;
        this.statusHistory.add(new StatusChange(OrderStatus.PENDING, this.createdAt));
    }

    public static class StatusChange {
        private OrderStatus status;
        private long timestamp;

        public StatusChange() {}
        public StatusChange(OrderStatus status, long timestamp) {
            this.status = status;
            this.timestamp = timestamp;
        }
        public OrderStatus getStatus() { return status; }
        public long getTimestamp() { return timestamp; }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getBuyerId() { return buyerId; }
    public void setBuyerId(String buyerId) { this.buyerId = buyerId; }
    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }
    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }
    public double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
    public List<StatusChange> getStatusHistory() { return statusHistory; }
    public void setStatusHistory(List<StatusChange> statusHistory) { this.statusHistory = statusHistory; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public long getLastStatusChangeAt() { return lastStatusChangeAt; }
    public void setLastStatusChangeAt(long lastStatusChangeAt) { this.lastStatusChangeAt = lastStatusChangeAt; }

    public synchronized void advanceStatus() {
        if (status.isTerminal()) return;
        status = status.next();
        lastStatusChangeAt = Instant.now().toEpochMilli();
        statusHistory.add(new StatusChange(status, lastStatusChangeAt));
    }
}
