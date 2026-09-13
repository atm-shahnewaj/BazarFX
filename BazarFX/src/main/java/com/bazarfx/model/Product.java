package com.bazarfx.model;

public class Product {
    private int productId;
    private int sellerId;
    private int categoryId;
    private String title;
    private String description;
    private double priceBdt;
    private String condition;
    private String status;
    private String location;
    private String imagePath;
    private int viewCount;

    public Product(int productId, int sellerId, int categoryId, String title, String description,
                   double priceBdt, String condition, String status, String location, String imagePath, int viewCount) {
        this.productId = productId;
        this.sellerId = sellerId;
        this.categoryId = categoryId;
        this.title = title;
        this.description = description;
        this.priceBdt = priceBdt;
        this.condition = condition;
        this.status = status;
        this.location = location;
        this.imagePath = imagePath;
        this.viewCount = viewCount;
    }

    public int getProductId() { return productId; }
    public int getSellerId() { return sellerId; }
    public int getCategoryId() { return categoryId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public double getPriceBdt() { return priceBdt; }
    public String getCondition() { return condition; }
    public String getStatus() { return status; }
    public String getLocation() { return location; }
    public String getImagePath() { return imagePath; }
    public int getViewCount() { return viewCount; }
}
