package com.bazarfx.controller;

import com.bazarfx.AppContext;
import com.bazarfx.model.Product;
import com.bazarfx.model.User;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

import java.io.File;

public class ProductDetailController {

    @FXML private StackPane photoBox;
    @FXML private ImageView photoView;
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

        loadPhoto(product);
    }

    /** Shows the listing's first full-size photo if one is on disk; otherwise the photo
     *  box collapses (managed=false) so no empty gap is left in the layout. Falls back
     *  to the thumbnail if the original was somehow removed but the thumbnail remains. */
    private void loadPhoto(Product product) {
        Image image = firstLoadable(product.getPhotoPaths());
        if (image == null) {
            image = firstLoadable(product.getThumbnailPaths());
        }
        if (image != null) {
            photoView.setImage(image);
            photoBox.setManaged(true);
            photoBox.setVisible(true);
        } else {
            photoBox.setManaged(false);
            photoBox.setVisible(false);
        }
    }

    private static Image firstLoadable(java.util.List<String> paths) {
        for (String path : paths) {
            if (path == null || path.isBlank()) continue;
            File file = new File(path);
            if (!file.exists()) continue;
            try {
                Image img = new Image(file.toURI().toString(), 640, 480, true, true, true);
                if (!img.isError()) return img;
            } catch (Exception ignored) {
                // fall through to next candidate photo
            }
        }
        return null;
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
}
