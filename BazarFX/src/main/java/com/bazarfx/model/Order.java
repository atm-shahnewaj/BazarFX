package com.bazarfx.model;

import java.sql.Timestamp;

public class Order {
    private int orderId;
    private int buyerId;
    private int productId;
    private String productTitle;
    private double totalPriceBdt;
    private String status;
    private Timestamp createdAt;

    public Order(int orderId, int buyerId, int productId, String productTitle, double totalPriceBdt, String status, Timestamp createdAt) {
        this.orderId = orderId;
        this.buyerId = buyerId;
        this.productId = productId;
        this.productTitle = productTitle;
        this.totalPriceBdt = totalPriceBdt;
        this.status = status;
        this.createdAt = createdAt;
    }

    public int getOrderId() { return orderId; }
    public int getBuyerId() { return buyerId; }
    public int getProductId() { return productId; }
    public String getProductTitle() { return productTitle; }
    public double getTotalPriceBdt() { return totalPriceBdt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Timestamp getCreatedAt() { return createdAt; }
}
