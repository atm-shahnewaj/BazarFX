package com.bazarfx.controller;

import com.bazarfx.AppContext;
import com.bazarfx.service.AuthService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    @FXML private TextField usernameOrEmailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    @FXML
    private void onLogin() {
        AppContext ctx = AppContext.get();
        try {
            ctx.authService.login(usernameOrEmailField.getText(), passwordField.getText());
            errorLabel.setText("");
            ctx.router.showShell();
        } catch (AuthService.AuthException e) {
            errorLabel.setText(e.getMessage());
        }
    }

    @FXML
    private void onGoToSignup() {
        AppContext.get().router.showAuthScreen("SignupView.fxml", "BazarFX - Sign Up");
    }
}
