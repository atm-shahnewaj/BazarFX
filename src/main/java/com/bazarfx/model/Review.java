package com.bazarfx.model;

import java.time.Instant;

public class Review {
    private String id;
    private String orderId;
    private String buyerId;
    private String sellerId;
    private int rating; // 1-5
    private String comment;
    private long createdAt;
    private boolean flagged;

    public Review() {}

    public Review(String id, String orderId, String buyerId, String sellerId, int rating, String comment) {
        this.id = id;
        this.orderId = orderId;
        this.buyerId = buyerId;
        this.sellerId = sellerId;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = Instant.now().toEpochMilli();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getBuyerId() { return buyerId; }
    public void setBuyerId(String buyerId) { this.buyerId = buyerId; }
    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }
    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public boolean isFlagged() { return flagged; }
    public void setFlagged(boolean flagged) { this.flagged = flagged; }
}
