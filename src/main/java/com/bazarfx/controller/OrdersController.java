package com.bazarfx.controller;

import com.bazarfx.AppContext;
import com.bazarfx.model.Order;
import com.bazarfx.model.OrderStatus;
import com.bazarfx.model.User;
import com.bazarfx.service.ReviewService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;

/** Buyer's purchase history plus the "leave a review" action once an order is Delivered. */
public class OrdersController {

    @FXML private VBox ordersBox;

    @FXML
    public void initialize() {
        refresh();
    }

    private void refresh() {
        AppContext ctx = AppContext.get();
        User current = ctx.authService.getCurrentUser();
        List<Order> orders = ctx.orderService.buyerHistory(current.getId());

        ordersBox.getChildren().clear();
        for (Order order : orders) {
            ordersBox.getChildren().add(buildOrderRow(order));
        }
        if (orders.isEmpty()) {
            Label empty = new Label("No orders yet - go browse the marketplace!");
            empty.getStyleClass().add("page-subtext");
            ordersBox.getChildren().add(empty);
        }
    }

    private VBox buildOrderRow(Order order) {
        Label header = new Label("Order " + order.getId().substring(0, 8) + "  \u2014  "
                + order.getStatus() + "  \u2014  $" + String.format("%.2f", order.getTotalPrice()));
        header.getStyleClass().add("order-header");

        VBox itemsList = new VBox(2);
        order.getItems().forEach(i -> {
            Label item = new Label("  " + i.getProductTitleSnapshot() + " x" + i.getQuantity());
            item.getStyleClass().add("detail-meta");
            itemsList.getChildren().add(item);
        });

        VBox row = new VBox(6, header, itemsList);
        row.getStyleClass().add("order-card");

        if (order.getStatus() == OrderStatus.DELIVERED) {
            row.getChildren().add(buildReviewControls(order));
        }
        return row;
    }

    private HBox buildReviewControls(Order order) {
        ComboBox<Integer> ratingBox = new ComboBox<>();
        ratingBox.getItems().addAll(1, 2, 3, 4, 5);
        ratingBox.setValue(5);
        TextField commentField = new TextField();
        commentField.setPromptText("Leave a comment (optional)");
        Button submit = new Button("Submit review");
        submit.getStyleClass().add("secondary-button");
        Label feedback = new Label();
        feedback.getStyleClass().add("status-label");

        submit.setOnAction(e -> {
            try {
                AppContext.get().reviewService.addReview(order, ratingBox.getValue(), commentField.getText());
                feedback.setText("Thanks for the review!");
                submit.setDisable(true);
            } catch (ReviewService.ValidationException ex) {
                feedback.setText(ex.getMessage());
            }
        });

        HBox box = new HBox(8, new Label("Rate:"), ratingBox, commentField, submit, feedback);
        box.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        box.setPadding(new javafx.geometry.Insets(6, 0, 0, 0));
        return box;
    }
}
