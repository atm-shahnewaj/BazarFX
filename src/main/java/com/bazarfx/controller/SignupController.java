package com.bazarfx.controller;

import com.bazarfx.AppContext;
import com.bazarfx.service.AuthService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class SignupController {

    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private TextField locationField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    @FXML
    private void onSignup() {
        AppContext ctx = AppContext.get();
        try {
            ctx.authService.register(
                    usernameField.getText(), emailField.getText(), phoneField.getText(),
                    locationField.getText(), passwordField.getText());
            ctx.authService.login(usernameField.getText(), passwordField.getText());
            errorLabel.setText("");
            ctx.router.showShell();
        } catch (AuthService.AuthException e) {
            errorLabel.setText(e.getMessage());
        }
    }

    @FXML
    private void onGoToLogin() {
        AppContext.get().router.showAuthScreen("LoginView.fxml", "BazarFX - Login");
    }
}
