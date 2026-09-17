package com.bazarfx.controller;

import com.bazarfx.AppContext;
import com.bazarfx.model.*;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.*;
import java.util.stream.Collectors;

public class CheckoutController {

    @FXML private VBox summaryBox;
    @FXML private Label totalLabel;
    @FXML private Label confirmationLabel;

    private double grandTotal;

    @FXML
    public void initialize() {
        AppContext ctx = AppContext.get();
        User buyer = ctx.authService.getCurrentUser();
        List<CartItem> cart = CartStore.itemsFor(buyer.getId());

        grandTotal = 0;
        summaryBox.getChildren().clear();
        for (CartItem ci : cart) {
            Product p = ctx.productService.getById(ci.getProductId());
            if (p == null) continue;
            double lineTotal = p.getPrice() * ci.getQuantity();
            grandTotal += lineTotal;
            summaryBox.getChildren().add(new Label(p.getTitle() + "  x" + ci.getQuantity() + "  \u2014  $" + String.format("%.2f", lineTotal)));
        }
        totalLabel.setText(String.format("Total: $%.2f", grandTotal));
    }

    @FXML
    private void onConfirmPayment() {
        AppContext ctx = AppContext.get();
        User buyer = ctx.authService.getCurrentUser();
        List<CartItem> cart = CartStore.itemsFor(buyer.getId());

        // Group cart items by seller so each seller gets their own Order (a single cart
        // can span multiple sellers, mirroring a real marketplace checkout).
        Map<String, List<OrderItem>> bySeller = new HashMap<>();
        for (CartItem ci : cart) {
            Product p = ctx.productService.getById(ci.getProductId());
            if (p == null) continue;
            bySeller.computeIfAbsent(p.getSellerId(), s -> new ArrayList<>())
                    .add(new OrderItem(p.getId(), p.getTitle(), p.getPrice(), ci.getQuantity()));
        }

        List<Order> placed = new ArrayList<>();
        for (Map.Entry<String, List<OrderItem>> entry : bySeller.entrySet()) {
            placed.add(ctx.orderService.placeOrder(buyer.getId(), entry.getKey(), entry.getValue()));
        }

        CartStore.clear(buyer.getId());
        confirmationLabel.setText(placed.size() + " order(s) placed. Track their status under \"Orders\".");
        summaryBox.getChildren().clear();
        totalLabel.setText("Total: $0.00");
    }
}
