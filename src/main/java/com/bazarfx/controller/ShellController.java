package com.bazarfx.controller;

import com.bazarfx.AppContext;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.animation.PauseTransition;
import javafx.util.Duration;

/**
 * Persistent app frame: top navigation bar + a swappable content pane. Also owns the
 * toast label that NotificationPoller events are routed into (already marshalled to the
 * JavaFX Application Thread by the poller before this method is called).
 */
public class ShellController {

    @FXML private BorderPane root;
    @FXML private Label welcomeLabel;
    @FXML private Label toastLabel;

    @FXML
    public void initialize() {
        AppContext ctx = AppContext.get();
        ctx.shellController = this;
        if (ctx.authService.getCurrentUser() != null) {
            welcomeLabel.setText("Hi, " + ctx.authService.getCurrentUser().getUsername());
        }
        toastLabel.setText("");
    }

    public void setContent(Parent content) {
        root.setCenter(content);
    }

    public void showToast(String message) {
        toastLabel.setText(message);
        PauseTransition pause = new PauseTransition(Duration.seconds(4));
        pause.setOnFinished(e -> toastLabel.setText(""));
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
    private void onReports() { AppContext.get().router.navigate("ReportsView.fxml"); }

    @FXML
    private void onLogout() {
        AppContext ctx = AppContext.get();
        ctx.authService.logout();
        ctx.router.showAuthScreen("LoginView.fxml", "BazarFX - Login");
    }
}
