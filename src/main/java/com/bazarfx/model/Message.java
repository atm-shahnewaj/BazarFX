package com.bazarfx.model;

import java.time.Instant;

/**
 * A single chat message between a buyer and a seller about a specific product.
 * The trio (productId, buyerId, sellerId) identifies the "conversation" - a
 * lightweight alternative to a separate Conversation/Thread table that still
 * lets MessagingService group messages correctly.
 */
public class Message {
    private String id;
    private String productId;
    private String buyerId;
    private String sellerId;
    private String senderId;
    private String body;
    private long sentAt;
    private boolean read;

    public Message() {}

    public Message(String id, String productId, String buyerId, String sellerId, String senderId, String body) {
        this.id = id;
        this.productId = productId;
        this.buyerId = buyerId;
        this.sellerId = sellerId;
        this.senderId = senderId;
        this.body = body;
        this.sentAt = Instant.now().toEpochMilli();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public String getBuyerId() { return buyerId; }
    public void setBuyerId(String buyerId) { this.buyerId = buyerId; }
    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }
    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public long getSentAt() { return sentAt; }
    public void setSentAt(long sentAt) { this.sentAt = sentAt; }
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    /** Groups messages into a conversation regardless of who sent the latest one. */
    public String conversationKey() { return productId + "|" + buyerId + "|" + sellerId; }
}
