package com.bazarfx.controller;

import com.bazarfx.AppContext;
import com.bazarfx.concurrency.ExchangeRateFetcher;
import com.bazarfx.notification.AppNotification;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Persistent app frame: top navigation bar + a swappable content pane, plus a stack of
 * animated popup notifications (order status changes, new chat messages, published
 * listings...) rendered over everything else. Each popup knows its own icon/message/style
 * via the polymorphic AppNotification hierarchy - this controller just renders whatever
 * it's handed.
 */
public class ShellController {

    @FXML private BorderPane shellBorder;
    @FXML private Label welcomeLabel;
    @FXML private Label rateLabel;
    @FXML private VBox toastStack;

    private static final int MAX_VISIBLE_TOASTS = 4;

    @FXML
    public void initialize() {
        AppContext ctx = AppContext.get();
        ctx.shellController = this;
        if (ctx.authService.getCurrentUser() != null) {
            welcomeLabel.setText("Hi, " + ctx.authService.getCurrentUser().getUsername());
        }
        ExchangeRateFetcher.getInstance().setOnRateFetchedCallback(msg -> {
            rateLabel.setText(msg);
            rateLabel.setVisible(true);
            rateLabel.setManaged(true);
        });
    }

    public void setContent(Parent content) {
        shellBorder.setCenter(content);
    }

    /** Pushes a new animated toast onto the top-right stack for the given notification,
     *  auto-dismissing it after a few seconds; older toasts drop off past MAX_VISIBLE_TOASTS. */
    public void showToast(AppNotification notification) {
        HBox card = new HBox(10);
        card.getStyleClass().addAll("toast-card", notification.getStyleClass());
        Label icon = new Label(notification.getIcon());
        icon.getStyleClass().add("toast-icon");
        Label text = new Label(notification.getMessage());
        text.getStyleClass().add("toast-text");
        text.setWrapText(true);
        text.setMaxWidth(280);
        card.getChildren().addAll(icon, text);
        card.setOpacity(0);
        card.setTranslateX(60);

        toastStack.getChildren().add(0, card);
        while (toastStack.getChildren().size() > MAX_VISIBLE_TOASTS) {
            toastStack.getChildren().remove(toastStack.getChildren().size() - 1);
        }

        FadeTransition fadeIn = new FadeTransition(Duration.millis(220), card);
        fadeIn.setToValue(1);
        TranslateTransition slideIn = new TranslateTransition(Duration.millis(220), card);
        slideIn.setToX(0);
        fadeIn.play();
        slideIn.play();

        PauseTransition pause = new PauseTransition(Duration.seconds(4.5));
        pause.setOnFinished(e -> {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(300), card);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(ev -> toastStack.getChildren().remove(card));
            fadeOut.play();
        });
        pause.play();
    }

    @FXML
    private void onBrowse() { AppContext.get().router.navigate("BrowseView.fxml"); }

    @FXML
    private void onSell() { AppContext.get().router.navigate("SellListingView.fxml"); }

    @FXML
    private void onCart() { AppContext.get().router.navigate("CartView.fxml"); }

    @FXML
    private void onDashboard() { AppContext.get().router.navigate("SellerDashboardView.fxml"); }

    @FXML
    private void onOrders() { AppContext.get().router.navigate("OrdersView.fxml"); }

    @FXML
    private void onMessages() { AppContext.get().router.navigate("MessagesView.fxml"); }

    @FXML
    private void onReports() { AppContext.get().router.navigate("ReportsView.fxml"); }

    @FXML
    private void onLogout() {
        AppContext ctx = AppContext.get();
        ctx.authService.logout();
        ctx.router.showAuthScreen("LoginView.fxml", "BazarFX - Login");
    }
}
