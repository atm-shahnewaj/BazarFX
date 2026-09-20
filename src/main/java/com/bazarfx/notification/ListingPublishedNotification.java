package com.bazarfx.notification;

import com.bazarfx.model.Product;

public class ListingPublishedNotification extends AppNotification {
    private final Product product;

    public ListingPublishedNotification(Product product) {
        this.product = product;
    }

    @Override
    public String getIcon() { return "\uD83D\uDECD\uFE0F"; }

    @Override
    public String getMessage() { return "Listing published: " + product.getTitle(); }

    @Override
    public String getStyleClass() { return "toast-success"; }
}
