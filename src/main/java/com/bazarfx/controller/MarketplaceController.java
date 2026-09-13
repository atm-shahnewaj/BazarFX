package com.bazarfx.controller;

import com.bazarfx.dao.OrderDao;
import com.bazarfx.dao.ProductDao;
import com.bazarfx.model.Order;
import com.bazarfx.model.Product;
import com.bazarfx.concurrency.ExchangeRateFetcher;
import com.bazarfx.concurrency.ReportGenerator;
import com.bazarfx.service.JsonBackupService;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

public class MarketplaceController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> currencyComboBox;
    @FXML private FlowPane productGrid;
    @FXML private Label statusLabel;
    @FXML private Label exchangeRateLabel;

    @FXML private TextField titleInput;
    @FXML private TextField priceInput;
    @FXML private ComboBox<String> categoryInput;
    @FXML private ComboBox<String> conditionInput;
    @FXML private TextField locationInput;
    @FXML private TextArea descInput;

    @FXML private TableView<Order> ordersTable;
    @FXML private TableColumn<Order, Integer> colOrderId;
    @FXML private TableColumn<Order, String> colProduct;
    @FXML private TableColumn<Order, Double> colPrice;
    @FXML private TableColumn<Order, String> colStatus;

    private final ProductDao productDao = new ProductDao();
    private final OrderDao orderDao = new OrderDao();
    private double currentRate = 1.0;
    private String currencySymbol = "৳";

    @FXML
    public void initialize() {
        currencyComboBox.setItems(FXCollections.observableArrayList("BDT (৳)", "USD ($)", "EUR (€)", "INR (₹)"));
        currencyComboBox.setValue("BDT (৳)");
        currencyComboBox.setOnAction(e -> handleCurrencyChange());

        categoryInput.setItems(FXCollections.observableArrayList("Electronics", "Vehicles", "Property"));
        conditionInput.setItems(FXCollections.observableArrayList("New", "Used", "Refurbished"));

        colOrderId.setCellValueFactory(new PropertyValueFactory<>("orderId"));
        colProduct.setCellValueFactory(new PropertyValueFactory<>("productTitle"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("totalPriceBdt"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        ExchangeRateFetcher.getInstance().setOnRateFetchedCallback(rateMsg -> {
            if (exchangeRateLabel != null) {
                exchangeRateLabel.setText(rateMsg);
            }
        });

        loadProducts("");
        loadOrders();
    }

    private void handleCurrencyChange() {
        String selected = currencyComboBox.getValue();
        if (selected.contains("USD")) {
            currentRate = 0.0083;
            currencySymbol = "$";
        } else if (selected.contains("EUR")) {
            currentRate = 0.0076;
            currencySymbol = "€";
        } else if (selected.contains("INR")) {
            currentRate = 0.70;
            currencySymbol = "₹";
        } else {
            currentRate = 1.0;
            currencySymbol = "৳";
        }
        loadProducts(searchField.getText());
    }

    @FXML
    private void handleSearch() {
        loadProducts(searchField.getText().trim());
    }

    private void loadProducts(String filter) {
        productGrid.getChildren().clear();
        List<Product> products = productDao.getAllActiveProducts();

        for (Product p : products) {
            if (!filter.isEmpty() && !p.getTitle().toLowerCase().contains(filter.toLowerCase())) {
                continue;
            }
            productGrid.getChildren().add(createProductCard(p));
        }
    }

    private VBox createProductCard(Product p) {
        VBox card = new VBox(8);
        card.getStyleClass().add("product-card");
        card.setPrefWidth(240);

        ImageView imgView = new ImageView();
        try {
            imgView.setImage(new Image(p.getImagePath(), 220, 130, true, true, true));
        } catch (Exception ignored) {}
        imgView.setFitWidth(220);
        imgView.setFitHeight(130);

        Label title = new Label(p.getTitle());
        title.getStyleClass().add("card-title");

        double convertedPrice = p.getPriceBdt() * currentRate;
        Label price = new Label(String.format("%s %.2f", currencySymbol, convertedPrice));
        price.getStyleClass().add("card-price");

        Label condBadge = new Label(p.getCondition());
        condBadge.getStyleClass().add("badge-condition");

        Label loc = new Label("📍 " + p.getLocation());
        loc.getStyleClass().add("card-location");

        Button buyBtn = new Button("Buy Now");
        buyBtn.getStyleClass().add("btn-accent");
        buyBtn.setOnAction(e -> handleBuyProduct(p));

        HBox bottomRow = new HBox(10, condBadge, buyBtn);
        bottomRow.setAlignment(Pos.CENTER_LEFT);

        card.getChildren().addAll(imgView, title, price, loc, bottomRow);
        return card;
    }

    private void handleBuyProduct(Product p) {
        Order newOrder = new Order(0, 1, p.getProductId(), p.getTitle(), p.getPriceBdt(), "Pending", null);
        if (orderDao.createOrder(newOrder)) {
            statusLabel.setText("Order placed successfully for: " + p.getTitle());
            loadOrders();
        }
    }

    @FXML
    private void handleAddProduct() {
        try {
            String title = titleInput.getText();
            double price = Double.parseDouble(priceInput.getText());
            String cat = categoryInput.getValue();
            String cond = conditionInput.getValue();
            String loc = locationInput.getText();
            String desc = descInput.getText();

            int catId = "Vehicles".equals(cat) ? 2 : "Property".equals(cat) ? 3 : 1;

            Product p = new Product(0, 1, catId, title, desc, price, cond, "Active", loc, "https://picsum.photos/id/100/300/200", 0);
            if (productDao.addProduct(p)) {
                statusLabel.setText("Product listed successfully!");
                loadProducts("");
                titleInput.clear();
                priceInput.clear();
                locationInput.clear();
                descInput.clear();
            }
        } catch (Exception e) {
            statusLabel.setText("Error adding product. Check inputs.");
        }
    }

    @FXML
    private void handleRefreshOrders() {
        loadOrders();
    }

    private void loadOrders() {
        List<Order> orders = orderDao.getAllOrders();
        ObservableList<Order> obsList = FXCollections.observableArrayList(orders);
        ordersTable.setItems(obsList);
    }

    @FXML
    private void handleExportJson() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Products Backup");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));
        File file = fileChooser.showSaveDialog(null);

        if (file != null) {
            if (JsonBackupService.exportProductsToJson(file.getAbsolutePath())) {
                statusLabel.setText("Exported backup to: " + file.getName());
            }
        }
    }

    @FXML
    private void handleGenerateAnalytics() {
        statusLabel.setText("Calculating marketplace analytics in background...");
        ReportGenerator generator = new ReportGenerator();
        Future<Map<String, Object>> future = generator.generateMarketplaceReportAsync();

        new Thread(() -> {
            try {
                Map<String, Object> result = future.get();
                javafx.application.Platform.runLater(() -> {
                    statusLabel.setText(String.format("Analytics Done! Volume: ৳%.2f | Active: %s",
                            result.get("totalVolumeBdt"), result.get("activeListingsCount")));
                });
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                generator.shutdown();
            }
        }).start();
    }
}
