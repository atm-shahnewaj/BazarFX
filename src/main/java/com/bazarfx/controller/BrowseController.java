package com.bazarfx.controller;

import com.bazarfx.AppContext;
import com.bazarfx.model.Category;
import com.bazarfx.model.Condition;
import com.bazarfx.model.Product;
import com.bazarfx.service.ProductService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

import java.io.File;

import java.util.List;
import java.util.stream.Collectors;

public class BrowseController {

    @FXML private TextField searchField;
    @FXML private ComboBox<Category> categoryFilter;
    @FXML private ComboBox<Condition> conditionFilter;
    @FXML private TextField minPriceField;
    @FXML private TextField maxPriceField;
    @FXML private ComboBox<ProductService.SortOrder> sortBox;
    @FXML private FlowPane resultsPane;
    @FXML private Label resultCountLabel;

    @FXML
    public void initialize() {
        categoryFilter.getItems().add(null);
        categoryFilter.getItems().addAll(Category.values());
        conditionFilter.getItems().add(null);
        conditionFilter.getItems().addAll(Condition.values());
        sortBox.getItems().addAll(ProductService.SortOrder.values());
        sortBox.setValue(ProductService.SortOrder.NEWEST);

        runSearch();
    }

    @FXML
    private void onApplyFilters() {
        runSearch();
    }

    private void runSearch() {
        AppContext ctx = AppContext.get();
        List<Product> base;
        String keyword = searchField.getText();
        if (keyword != null && !keyword.isBlank()) {
            List<String> ids = ctx.searchIndex.search(keyword);
            base = ids.stream().map(ctx.productService::getById).filter(p -> p != null).collect(Collectors.toList());
        } else {
            base = ctx.productService.allActive();
        }

        Double minPrice = parseOrNull(minPriceField.getText());
        Double maxPrice = parseOrNull(maxPriceField.getText());
        ProductService.SortOrder sort = sortBox.getValue() == null ? ProductService.SortOrder.NEWEST : sortBox.getValue();

        List<Product> results = ctx.productService.filterAndSort(
                base, categoryFilter.getValue(), minPrice, maxPrice, conditionFilter.getValue(), sort);

        renderResults(results);
    }

    private Double parseOrNull(String text) {
        if (text == null || text.isBlank()) return null;
        try {
            return Double.parseDouble(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void renderResults(List<Product> products) {
        resultsPane.getChildren().clear();
        resultCountLabel.setText(products.size() + " listing(s)");
        for (Product product : products) {
            resultsPane.getChildren().add(buildCard(product));
        }
    }

    private javafx.scene.Node buildCard(Product product) {
        VBoxCard card = new VBoxCard(product);
        card.setOnMouseClicked(e -> openDetail(product));
        return card;
    }

    private void openDetail(Product product) {
        AppContext ctx = AppContext.get();
        FXMLLoader loader = ctx.router.navigate("ProductDetailView.fxml");
        ProductDetailController controller = loader.getController();
        controller.setProduct(product);
    }

    /** Small self-contained product card so BrowseController doesn't need a separate FXML per card.
     *  Sizing comes entirely from the "product-card" / "card-thumb" style classes in style.css (in
     *  em units), so the card grows and shrinks along with the rest of the responsive UI. */
    private static class VBoxCard extends javafx.scene.layout.VBox {
        VBoxCard(Product product) {
            getStyleClass().add("product-card");

            StackPane thumb = new StackPane();
            thumb.getStyleClass().add("card-thumb");
            thumb.setMaxWidth(Double.MAX_VALUE);

            Image photo = loadFirstAvailable(product);
            if (photo != null) {
                ImageView imageView = new ImageView(photo);
                imageView.setPreserveRatio(true);
                imageView.setSmooth(true);
                imageView.fitHeightProperty().bind(thumb.heightProperty());
                imageView.fitWidthProperty().bind(thumb.widthProperty());
                javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle();
                clip.widthProperty().bind(thumb.widthProperty());
                clip.heightProperty().bind(thumb.heightProperty());
                clip.setArcWidth(24);
                clip.setArcHeight(24);
                imageView.setClip(clip);
                thumb.getChildren().add(imageView);
            } else {
                Label thumbGlyph = new Label(initials(product.getTitle()));
                thumbGlyph.getStyleClass().add("card-title");
                thumb.getChildren().add(thumbGlyph);
            }

            Label title = new Label(product.getTitle());
            title.getStyleClass().add("card-title");
            title.setWrapText(true);

            Label price = new Label(String.format("$%.2f", product.getPrice()));
            price.getStyleClass().add("card-price");

            Label badge = new Label(String.valueOf(product.getCondition()));
            badge.getStyleClass().add("card-badge");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            HBox metaRow = new HBox(6, new Label(String.valueOf(product.getCategory())) {{
                getStyleClass().add("card-meta");
            }}, spacer, badge);
            metaRow.setAlignment(Pos.CENTER_LEFT);

            getChildren().addAll(thumb, title, price, metaRow);
        }

        /** Prefer the generated thumbnail (small, fast); fall back to the original photo;
         *  skip silently (leaving the initials placeholder) if nothing loads - covers the
         *  case where background thumbnail generation hasn't finished yet, or the photo
         *  file was moved/deleted since the listing was published. */
        private static Image loadFirstAvailable(Product product) {
            for (String path : product.getThumbnailPaths()) {
                Image img = tryLoad(path);
                if (img != null) return img;
            }
            for (String path : product.getPhotoPaths()) {
                Image img = tryLoad(path);
                if (img != null) return img;
            }
            return null;
        }

        private static Image tryLoad(String path) {
            if (path == null || path.isBlank()) return null;
            try {
                Image img;
                if (path.startsWith("http://") || path.startsWith("https://")) {
                    img = new Image(path, 300, 300, true, true, true);
                } else {
                    File file = new File(path);
                    if (!file.exists()) return null;
                    img = new Image(file.toURI().toString(), 300, 300, true, true, true);
                }
                return img.isError() ? null : img;
            } catch (Exception e) {
                return null;
            }
        }

        private static String initials(String title) {
            if (title == null || title.isBlank()) return "?";
            String[] parts = title.trim().split("\\s+");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < Math.min(2, parts.length); i++) {
                sb.append(Character.toUpperCase(parts[i].charAt(0)));
            }
            return sb.toString();
        }
    }
}
