package com.bazarfx.service;

import com.bazarfx.model.Category;
import com.bazarfx.model.Condition;
import com.bazarfx.model.ListingStatus;
import com.bazarfx.model.Product;
import com.bazarfx.storage.FileStorageManager;
import com.bazarfx.util.IdGenerator;
import com.bazarfx.util.InputValidator;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Business logic for listings: create/edit/pause/delete, view counting, and the
 * browse/filter/sort queries used by the Browse screen. Keyword search itself is
 * served by concurrency.SearchIndex, which this service delegates to.
 */
public class ProductService {

    public enum SortOrder { NEWEST, PRICE_LOW_HIGH, PRICE_HIGH_LOW, POPULARITY }

    private final FileStorageManager storage;

    public ProductService(FileStorageManager storage) {
        this.storage = storage;
    }

    public static class ValidationException extends RuntimeException {
        public ValidationException(String message) { super(message); }
    }

    public Product createListing(String sellerId, String title, String description,
                                  Category category, double price, Condition condition) {
        if (!InputValidator.isNonEmpty(title)) throw new ValidationException("Title is required.");
        if (!InputValidator.isNonEmpty(description)) throw new ValidationException("Description is required.");
        if (!InputValidator.isPositive(price)) throw new ValidationException("Price must be greater than zero.");

        Product product = new Product(IdGenerator.newId(), sellerId, title.trim(), description.trim(), category, price, condition);
        storage.saveProduct(product);
        return product;
    }

    public void updateListing(Product product, String title, String description, double price, Condition condition) {
        product.setTitle(title);
        product.setDescription(description);
        product.setPrice(price);
        product.setCondition(condition);
        product.setUpdatedAt(System.currentTimeMillis());
        storage.saveProduct(product);
    }

    public void setStatus(Product product, ListingStatus status) {
        product.setStatus(status);
        product.setUpdatedAt(System.currentTimeMillis());
        storage.saveProduct(product);
    }

    public void deleteListing(Product product) {
        storage.deleteProduct(product.getId());
    }

    public void incrementViewCount(Product product) {
        product.setViewCount(product.getViewCount() + 1);
        storage.saveProduct(product);
    }

    public Product getById(String productId) {
        return storage.productsMap().get(productId);
    }

    public List<Product> allActive() {
        return storage.productsMap().values().stream()
                .filter(p -> p.getStatus() == ListingStatus.ACTIVE)
                .collect(Collectors.toList());
    }

    public List<Product> listingsBySeller(String sellerId) {
        return storage.productsMap().values().stream()
                .filter(p -> p.getSellerId().equals(sellerId))
                .sorted(Comparator.comparingLong(Product::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    /** Applies category / price-range / condition / location filters and a sort order to a base list. */
    public List<Product> filterAndSort(List<Product> base, Category category, Double minPrice, Double maxPrice,
                                        Condition condition, SortOrder sortOrder) {
        List<Product> filtered = base.stream()
                .filter(p -> category == null || p.getCategory() == category)
                .filter(p -> minPrice == null || p.getPrice() >= minPrice)
                .filter(p -> maxPrice == null || p.getPrice() <= maxPrice)
                .filter(p -> condition == null || p.getCondition() == condition)
                .collect(Collectors.toList());

        Comparator<Product> comparator = switch (sortOrder) {
            case PRICE_LOW_HIGH -> Comparator.comparingDouble(Product::getPrice);
            case PRICE_HIGH_LOW -> Comparator.comparingDouble(Product::getPrice).reversed();
            case POPULARITY -> Comparator.comparingInt(Product::getViewCount).reversed();
            case NEWEST -> Comparator.comparingLong(Product::getCreatedAt).reversed();
        };
        return filtered.stream().sorted(comparator).collect(Collectors.toList());
    }
}
