package com.bazarfx.notification;

import java.time.Instant;

/**
 * Abstract base for every popup/toast event the app can raise. Concrete subclasses
 * (OrderStatusNotification, NewMessageNotification, ListingPublishedNotification)
 * each know how to describe themselves; ShellController only needs to work with
 * this common type, so adding a brand-new kind of notification later never
 * requires touching the popup rendering code - a small, concrete demonstration
 * of abstraction + polymorphism rather than a switch-on-type.
 */
public abstract class AppNotification {

    private final long createdAt = Instant.now().toEpochMilli();

    /** Short glyph/emoji shown on the popup, e.g. "\uD83D\uDCE6" for orders. */
    public abstract String getIcon();

    /** Human-readable line shown in the toast. */
    public abstract String getMessage();

    /** CSS style class applied to the popup so different kinds can be colored differently. */
    public abstract String getStyleClass();

    public long getCreatedAt() { return createdAt; }
}
