package com.bazarfx;

import com.bazarfx.controller.ShellController;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Thin helper for swapping the root of the primary Stage between FXML screens.
 *
 * Also owns the app's "responsive scaling": every screen's base font-size is
 * recalculated whenever the window is resized, so text, spacing (expressed in
 * `em` in style.css) and controls grow/shrink together instead of staying
 * pinned at a fixed pixel size regardless of window size.
 */
public class SceneRouter {

    private static final double BASE_FONT_PX = 14.0;
    private static final double MIN_SCALE = 0.82;
    private static final double MAX_SCALE = 1.65;

    private final Stage stage;
    private ShellController shellController;

    public SceneRouter(Stage stage) {
        this.stage = stage;
    }

    /** Used for the pre-login screens (Login / Signup), which replace the whole scene. */
    public void showAuthScreen(String fxmlName, String title) {
        Parent root = load(fxmlName);
        Scene scene = new Scene(root, 520, 620);
        scene.getStylesheets().add(getClass().getResource("/com/bazarfx/css/style.css").toExternalForm());
        stage.setTitle(title);
        stage.setMinWidth(420);
        stage.setMinHeight(480);
        stage.setScene(scene);
        bindResponsiveScale(root, scene, 520, 620);
        stage.show();
    }

    /** Loads the persistent app shell (nav bar + content area) once, after login. */
    public void showShell() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/bazarfx/view/ShellView.fxml"));
        Parent shellRoot;
        try {
            shellRoot = loader.load();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load ShellView.fxml", e);
        }
        shellController = loader.getController();
        Scene scene = new Scene(shellRoot, 1180, 760);
        scene.getStylesheets().add(getClass().getResource("/com/bazarfx/css/style.css").toExternalForm());
        stage.setTitle("BazarFX");
        stage.setMinWidth(860);
        stage.setMinHeight(560);
        stage.setScene(scene);
        bindResponsiveScale(shellRoot, scene, 1180, 760);
        stage.show();
        navigate("BrowseView.fxml");
    }

    /** Swaps the content area inside the shell to another screen, by FXML file name. */
    public FXMLLoader navigate(String fxmlName) {
        FXMLLoader fxmlLoader = loader(fxmlName);
        Parent content;
        try {
            content = fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load " + fxmlName, e);
        }
        shellController.setContent(content);
        return fxmlLoader;
    }

    public FXMLLoader loader(String fxmlName) {
        return new FXMLLoader(getClass().getResource("/com/bazarfx/view/" + fxmlName));
    }

    private Parent load(String fxmlName) {
        try {
            return loader(fxmlName).load();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load " + fxmlName, e);
        }
    }

    /**
     * Ties the root node's base font-size to the current window size, relative
     * to the design-time base size for that screen. Because every text and
     * spacing rule in style.css is written in `em`, this single binding makes
     * the whole screen (fonts, paddings, card sizes, button sizes, ...) scale
     * smoothly as the user drags the window smaller or bigger, instead of the
     * UI staying frozen at one fixed pixel size.
     */
    private void bindResponsiveScale(Parent root, Scene scene, double baseWidth, double baseHeight) {
        Runnable update = () -> {
            double widthScale = scene.getWidth() / baseWidth;
            double heightScale = scene.getHeight() / baseHeight;
            double scale = Math.min(widthScale, heightScale);
            scale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, scale));
            root.setStyle("-fx-font-size: " + (BASE_FONT_PX * scale) + "px;");
        };
        scene.widthProperty().addListener((obs, oldVal, newVal) -> update.run());
        scene.heightProperty().addListener((obs, oldVal, newVal) -> update.run());
        update.run();
    }
}
