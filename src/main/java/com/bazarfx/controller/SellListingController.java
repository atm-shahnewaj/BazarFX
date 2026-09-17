package com.bazarfx.controller;

import com.bazarfx.AppContext;
import com.bazarfx.model.Category;
import com.bazarfx.model.Condition;
import com.bazarfx.model.Product;
import com.bazarfx.model.User;
import com.bazarfx.service.ProductService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * "Sell an Item" form. The listing is saved and shown to the user immediately with its
 * original photos; thumbnail generation is handed off to ImageProcessor's background
 * thread pool so this form never freezes while images are resized.
 */
public class SellListingController {

    @FXML private TextField titleField;
    @FXML private TextArea descriptionField;
    @FXML private ComboBox<Category> categoryBox;
    @FXML private ComboBox<Condition> conditionBox;
    @FXML private TextField priceField;
    @FXML private Label photoCountLabel;
    @FXML private Label statusLabel;

    private final List<File> selectedPhotos = new ArrayList<>();

    @FXML
    public void initialize() {
        categoryBox.getItems().addAll(Category.values());
        conditionBox.getItems().addAll(Condition.values());
    }

    @FXML
    private void onChoosePhotos() {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
        List<File> chosen = chooser.showOpenMultipleDialog(titleField.getScene().getWindow());
        if (chosen != null) {
            selectedPhotos.clear();
            selectedPhotos.addAll(chosen.size() > 5 ? chosen.subList(0, 5) : chosen);
            photoCountLabel.setText(selectedPhotos.size() + " photo(s) selected");
        }
    }

    @FXML
    private void onPublish() {
        AppContext ctx = AppContext.get();
        User seller = ctx.authService.getCurrentUser();

        double price;
        try {
            price = Double.parseDouble(priceField.getText().trim());
        } catch (NumberFormatException e) {
            statusLabel.setText("Enter a valid price.");
            return;
        }

        Product product;
        try {
            product = ctx.productService.createListing(
                    seller.getId(), titleField.getText(), descriptionField.getText(),
                    categoryBox.getValue(), price, conditionBox.getValue());
        } catch (ProductService.ValidationException e) {
            statusLabel.setText(e.getMessage());
            return;
        }

        statusLabel.setText("Listing published! Processing photos in the background\u2026");
        ctx.searchIndex.rebuildAsync();

        if (!selectedPhotos.isEmpty()) {
            ctx.imageProcessor.processAsync(product, new ArrayList<>(selectedPhotos), new com.bazarfx.concurrency.ImageProcessor.Callback() {
                @Override
                public void onThumbnailsReady(Product p, List<String> thumbnailPaths) {
                    Platform.runLater(() -> statusLabel.setText("Listing published \u2014 photos processed (" + thumbnailPaths.size() + ")."));
                }

                @Override
                public void onFailure(Product p, Exception e) {
                    Platform.runLater(() -> statusLabel.setText("Listing published, but photo processing failed: " + e.getMessage()));
                }
            });
        }

        clearForm();
    }

    private void clearForm() {
        titleField.clear();
        descriptionField.clear();
        priceField.clear();
        categoryBox.setValue(null);
        conditionBox.setValue(null);
        selectedPhotos.clear();
        photoCountLabel.setText("0 photo(s) selected");
    }
}
