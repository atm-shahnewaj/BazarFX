package com.bazarfx.service;

import com.bazarfx.model.*;
import com.bazarfx.storage.FileStorageManager;
import com.bazarfx.util.IdGenerator;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Turns a buyer's cart into an Order and exposes order history queries.
 * Status *progression* over time is owned by concurrency.OrderStatusSimulator,
 * which calls advanceOrder() on a schedule — this service just creates orders
 * and reads them back.
 */
public class OrderService {

    private final FileStorageManager storage;

    public OrderService(FileStorageManager storage) {
        this.storage = storage;
    }

    /** Cart items are grouped by seller in the controller; this creates one order per seller. */
    public Order placeOrder(String buyerId, String sellerId, List<OrderItem> items) {
        double total = items.stream().mapToDouble(OrderItem::lineTotal).sum();
        Order order = new Order(IdGenerator.newId(), buyerId, sellerId, items, total);
        storage.addOrder(order);
        return order;
    }

    public List<Order> buyerHistory(String buyerId) {
        return storage.ordersSnapshot().stream()
                .filter(o -> o.getBuyerId().equals(buyerId))
                .sorted((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()))
                .collect(Collectors.toList());
    }

    public List<Order> sellerLedger(String sellerId) {
        return storage.ordersSnapshot().stream()
                .filter(o -> o.getSellerId().equals(sellerId))
                .sorted((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()))
                .collect(Collectors.toList());
    }

    /** Advances one order's status by one step and persists it. Called from a background thread. */
    public void advanceOrder(Order order) {
        order.advanceStatus();
        storage.flushOrders();
    }

    public List<Order> ordersInProgress() {
        return storage.ordersSnapshot().stream()
                .filter(o -> !o.getStatus().isTerminal())
                .collect(Collectors.toList());
    }
}
