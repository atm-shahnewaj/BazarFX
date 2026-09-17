package com.bazarfx.model;

public class OrderItem {
    private String productId;
    private String productTitleSnapshot;
    private double priceSnapshot;
    private int quantity;

    public OrderItem() {}

    public OrderItem(String productId, String productTitleSnapshot, double priceSnapshot, int quantity) {
        this.productId = productId;
        this.productTitleSnapshot = productTitleSnapshot;
        this.priceSnapshot = priceSnapshot;
        this.quantity = quantity;
    }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public String getProductTitleSnapshot() { return productTitleSnapshot; }
    public void setProductTitleSnapshot(String productTitleSnapshot) { this.productTitleSnapshot = productTitleSnapshot; }
    public double getPriceSnapshot() { return priceSnapshot; }
    public void setPriceSnapshot(double priceSnapshot) { this.priceSnapshot = priceSnapshot; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double lineTotal() { return priceSnapshot * quantity; }
}
