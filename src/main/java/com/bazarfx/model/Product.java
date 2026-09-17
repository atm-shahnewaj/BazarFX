package com.bazarfx.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class Product {
    private String id;
    private String sellerId;
    private String title;
    private String description;
    private Category category;
    private double price;
    private Condition condition;
    private List<String> photoPaths = new ArrayList<>();
    private List<String> thumbnailPaths = new ArrayList<>();
    private ListingStatus status = ListingStatus.ACTIVE;
    private int viewCount;
    private long createdAt;
    private long updatedAt;

    public Product() {}

    public Product(String id, String sellerId, String title, String description,
                    Category category, double price, Condition condition) {
        this.id = id;
        this.sellerId = sellerId;
        this.title = title;
        this.description = description;
        this.category = category;
        this.price = price;
        this.condition = condition;
        this.createdAt = Instant.now().toEpochMilli();
        this.updatedAt = this.createdAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public Condition getCondition() { return condition; }
    public void setCondition(Condition condition) { this.condition = condition; }
    public List<String> getPhotoPaths() { return photoPaths; }
    public void setPhotoPaths(List<String> photoPaths) { this.photoPaths = photoPaths; }
    public List<String> getThumbnailPaths() { return thumbnailPaths; }
    public void setThumbnailPaths(List<String> thumbnailPaths) { this.thumbnailPaths = thumbnailPaths; }
    public ListingStatus getStatus() { return status; }
    public void setStatus(ListingStatus status) { this.status = status; }
    public int getViewCount() { return viewCount; }
    public void setViewCount(int viewCount) { this.viewCount = viewCount; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}
