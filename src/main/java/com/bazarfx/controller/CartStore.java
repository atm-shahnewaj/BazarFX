package com.bazarfx.controller;

import com.bazarfx.model.CartItem;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-memory, per-user shopping cart. Not written to disk (the storage table in the
 * proposal only persists users/products/orders/reviews) - the cart is transient until
 * checkout turns it into a real, persisted Order. Thread-safe via ConcurrentHashMap +
 * CopyOnWriteArrayList since it can be touched from the UI thread and background flows.
 */
public class CartStore {

    private static final Map<String, List<CartItem>> cartsByUser = new ConcurrentHashMap<>();

    public static List<CartItem> itemsFor(String userId) {
        return cartsByUser.computeIfAbsent(userId, id -> new CopyOnWriteArrayList<>());
    }

    public static void addItem(String userId, String productId, int quantity) {
        List<CartItem> items = itemsFor(userId);
        for (CartItem item : items) {
            if (item.getProductId().equals(productId)) {
                item.setQuantity(item.getQuantity() + quantity);
                return;
            }
        }
        items.add(new CartItem(productId, quantity));
    }

    public static void removeItem(String userId, String productId) {
        itemsFor(userId).removeIf(i -> i.getProductId().equals(productId));
    }

    public static void clear(String userId) {
        itemsFor(userId).clear();
    }
}
