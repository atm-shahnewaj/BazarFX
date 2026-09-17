package com.bazarfx.controller;

import com.bazarfx.AppContext;
import com.bazarfx.model.Product;
import com.bazarfx.model.User;
import com.bazarfx.service.ProductService;
import com.bazarfx.service.ReportService;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;

public class SellerDashboardController {

    @FXML private Label totalListingsLabel;
    @FXML private Label totalViewsLabel;
    @FXML private Label revenueLabel;
    @FXML private VBox listingsBox;

    @FXML
    public void initialize() {
        refresh();
    }

    private void refresh() {
        AppContext ctx = AppContext.get();
        User seller = ctx.authService.getCurrentUser();

        ReportService.SellerDashboardStats stats = ctx.reportService.sellerDashboard(seller.getId());
        totalListingsLabel.setText(String.valueOf(stats.totalListings));
        totalViewsLabel.setText(String.valueOf(stats.totalViews));
        revenueLabel.setText(String.format("$%.2f", stats.simulatedRevenue));

        List<Product> listings = ctx.productService.listingsBySeller(seller.getId());
        listingsBox.getChildren().clear();
        for (Product p : listings) {
            listingsBox.getChildren().add(buildRow(p));
        }
    }

    private HBox buildRow(Product product) {
        Label info = new Label(product.getTitle() + "  \u2014  $" + String.format("%.2f", product.getPrice())
                + "  \u2014  " + product.getStatus() + "  \u2014  " + product.getViewCount() + " views");
        info.getStyleClass().add("card-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button toggle = new Button(product.getStatus().name().equals("ACTIVE") ? "Pause" : "Activate");
        toggle.getStyleClass().add("secondary-button");
        toggle.setOnAction(e -> {
            AppContext ctx = AppContext.get();
            var newStatus = product.getStatus().name().equals("ACTIVE")
                    ? com.bazarfx.model.ListingStatus.PAUSED
                    : com.bazarfx.model.ListingStatus.ACTIVE;
            ctx.productService.setStatus(product, newStatus);
            ctx.searchIndex.rebuildAsync();
            refresh();
        });

        Button delete = new Button("Delete");
        delete.getStyleClass().add("danger-button");
        delete.setOnAction(e -> {
            AppContext ctx = AppContext.get();
            ctx.productService.setStatus(product, com.bazarfx.model.ListingStatus.ARCHIVED);
            ctx.searchIndex.rebuildAsync();
            refresh();
        });

        HBox row = new HBox(12, info, spacer, toggle, delete);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("dashboard-row");
        return row;
    }
}
