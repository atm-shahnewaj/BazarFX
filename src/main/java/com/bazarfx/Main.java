package com.bazarfx;

import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Application entry point. Wires up AppContext (storage + services + concurrency
 * subsystems), starts the background services, and shows the login screen.
 */
public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        AppContext ctx = AppContext.init("data");
        ctx.router = new SceneRouter(primaryStage);
        ctx.startBackgroundServices();

        ctx.router.showAuthScreen("LoginView.fxml", "BazarFX - Login");

        primaryStage.setOnCloseRequest(e -> ctx.shutdownBackgroundServices());
    }

    @Override
    public void stop() {
        AppContext.get().shutdownBackgroundServices();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
