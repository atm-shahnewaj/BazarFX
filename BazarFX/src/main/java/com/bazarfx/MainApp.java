package com.bazarfx;

import com.bazarfx.dao.DatabaseManager;
import com.bazarfx.concurrency.ExchangeRateFetcher;
import com.bazarfx.concurrency.OrderStatusSimulator;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        DatabaseManager.initializeDatabase();

        ExchangeRateFetcher.getInstance().start();
        OrderStatusSimulator.getInstance().start();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/bazarfx/MarketplaceView.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root, 1180, 750);
        scene.getStylesheets().add(getClass().getResource("/com/bazarfx/styles.css").toExternalForm());

        primaryStage.setTitle("BazarFX - Offline-First Desktop Marketplace");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    @Override
    public void stop() throws Exception {
        ExchangeRateFetcher.getInstance().stop();
        OrderStatusSimulator.getInstance().stop();
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
