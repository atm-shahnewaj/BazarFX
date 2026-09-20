package com.bazarfx.controller;

import com.bazarfx.AppContext;
import com.bazarfx.model.Product;
import com.bazarfx.model.User;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public class ProductDetailController {

    @FXML private StackPane photoBox;
    @FXML private ImageView photoView;
    @FXML private javafx.scene.control.ScrollPane thumbStripScroll;
    @FXML private HBox thumbStrip;
    @FXML private Label titleLabel;
    @FXML private Label priceLabel;
    @FXML private Label descriptionLabel;
    @FXML private Label metaLabel;
    @FXML private Label sellerLabel;
    @FXML private Label viewCountLabel;
    @FXML private Spinner<Integer> quantitySpinner;
    @FXML private Label statusLabel;

    private Product product;

    @FXML
    public void initialize() {
        quantitySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10, 1));
        photoView.fitWidthProperty().bind(photoBox.widthProperty());
        photoView.fitHeightProperty().bind(photoBox.heightProperty());

        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle();
        clip.widthProperty().bind(photoBox.widthProperty());
        clip.heightProperty().bind(photoBox.heightProperty());
        clip.setArcWidth(28);
        clip.setArcHeight(28);
        photoView.setClip(clip);
    }

    public void setProduct(Product product) {
        this.product = product;
        AppContext ctx = AppContext.get();

        // View count increments here, persisted via ProductService - a lightweight
        // demonstration of a mutation the storage layer writes back to disk.
        ctx.productService.incrementViewCount(product);

        titleLabel.setText(product.getTitle());
        priceLabel.setText(String.format("$%.2f", product.getPrice()));
        descriptionLabel.setText(product.getDescription());
        metaLabel.setText(product.getCategory() + "  \u00b7  " + product.getCondition() + "  \u00b7  " + product.getStatus());
        viewCountLabel.setText(product.getViewCount() + " views");

        User seller = ctx.storage.usersMap().get(product.getSellerId());
        if (seller != null) {
            String ratingText = seller.getSellerRatingCount() > 0
                    ? String.format("%.1f\u2605 (%d)", seller.getSellerRatingAvg(), seller.getSellerRatingCount())
                    : "No ratings yet";
            sellerLabel.setText("Sold by " + seller.getUsername() + "  \u2014  " + ratingText);
        }

        loadGallery(product);
    }

    /** Bug fix: previously only the very first photo was ever shown, even when a listing
     *  had several. This now shows every photo the listing has: the first one large, and
     *  the rest (photos + any thumbnails that aren't just duplicates of a photo) as a
     *  clickable strip beneath it that swaps the large image on click. */
    private void loadGallery(Product product) {
        List<String> allPaths = new ArrayList<>(new LinkedHashSet<>(product.getPhotoPaths()));
        if (allPaths.isEmpty()) {
            allPaths.addAll(new LinkedHashSet<>(product.getThumbnailPaths()));
        }

        thumbStrip.getChildren().clear();
        Image first = null;

        for (String path : allPaths) {
            Image img = loadImage(path);
            if (img == null) continue;
            if (first == null) first = img;

            ImageView thumb = new ImageView(img);
            thumb.setFitWidth(84);
            thumb.setFitHeight(64);
            thumb.setPreserveRatio(false);
            thumb.setSmooth(true);
            thumb.getStyleClass().add("detail-thumb");
            javafx.scene.shape.Rectangle tClip = new javafx.scene.shape.Rectangle(84, 64);
            tClip.setArcWidth(12);
            tClip.setArcHeight(12);
            thumb.setClip(tClip);
            StackPane thumbWrap = new StackPane(thumb);
            thumbWrap.getStyleClass().add("detail-thumb-wrap");
            thumbWrap.setPadding(new Insets(2));
            Image finalImg = img;
            thumbWrap.setOnMouseClicked(e -> photoView.setImage(finalImg));
            thumbStrip.getChildren().add(thumbWrap);
        }

        boolean hasAny = first != null;
        photoBox.setManaged(hasAny);
        photoBox.setVisible(hasAny);
        if (hasAny) photoView.setImage(first);

        boolean showStrip = thumbStrip.getChildren().size() > 1;
        thumbStripScroll.setManaged(showStrip);
        thumbStripScroll.setVisible(showStrip);
    }

    private static Image loadImage(String path) {
        if (path == null || path.isBlank()) return null;
        try {
            Image img;
            if (path.startsWith("http://") || path.startsWith("https://")) {
                img = new Image(path, 640, 480, true, true, true);
            } else {
                File file = new File(path);
                if (!file.exists()) return null;
                img = new Image(file.toURI().toString(), 640, 480, true, true, true);
            }
            return img.isError() ? null : img;
        } catch (Exception e) {
            return null;
        }
    }

    @FXML
    private void onAddToCart() {
        AppContext ctx = AppContext.get();
        User current = ctx.authService.getCurrentUser();
        if (current == null || product == null) return;
        CartStore.addItem(current.getId(), product.getId(), quantitySpinner.getValue());
        statusLabel.setText("Added to cart.");
    }

    @FXML
    private void onAddToWishlist() {
        AppContext ctx = AppContext.get();
        User current = ctx.authService.getCurrentUser();
        if (current == null || product == null) return;
        if (!current.getWishlistProductIds().contains(product.getId())) {
            current.getWishlistProductIds().add(product.getId());
            ctx.storage.saveUser(current);
        }
        statusLabel.setText("Saved to wishlist.");
    }

    @FXML
    private void onMessageSeller() {
        AppContext ctx = AppContext.get();
        User current = ctx.authService.getCurrentUser();
        if (current == null || product == null) return;

        if (current.getId().equals(product.getSellerId())) {
            statusLabel.setText("This is your own listing.");
            return;
        }

        var loader = ctx.router.navigate("MessagesView.fxml");
        MessagesController controller = loader.getController();
        controller.openConversation(product.getId(), current.getId(), product.getSellerId());
    }
}
