package com.bazarfx.controller;

import com.bazarfx.AppContext;
import com.bazarfx.model.CartItem;
import com.bazarfx.model.Product;
import com.bazarfx.model.User;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class CartController {

    @FXML private VBox itemsBox;
    @FXML private Label totalLabel;

    @FXML
    public void initialize() {
        refresh();
    }

    private void refresh() {
        AppContext ctx = AppContext.get();
        User current = ctx.authService.getCurrentUser();
        itemsBox.getChildren().clear();
        double total = 0;

        for (CartItem item : CartStore.itemsFor(current.getId())) {
            Product product = ctx.productService.getById(item.getProductId());
            if (product == null) continue;
            total += product.getPrice() * item.getQuantity();
            itemsBox.getChildren().add(buildRow(product, item));
        }
        totalLabel.setText(String.format("Total: $%.2f", total));
    }

    private HBox buildRow(Product product, CartItem item) {
        Label name = new Label(product.getTitle() + "  x" + item.getQuantity());
        name.getStyleClass().add("card-title");
        Label price = new Label(String.format("$%.2f", product.getPrice() * item.getQuantity()));
        price.getStyleClass().add("card-price");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button remove = new Button("Remove");
        remove.getStyleClass().add("danger-button");
        remove.setOnAction(e -> {
            AppContext ctx = AppContext.get();
            CartStore.removeItem(ctx.authService.getCurrentUser().getId(), product.getId());
            refresh();
        });
        HBox row = new HBox(16, name, spacer, price, remove);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("cart-row");
        return row;
    }

    @FXML
    private void onCheckout() {
        AppContext.get().router.navigate("CheckoutView.fxml");
    }
}
