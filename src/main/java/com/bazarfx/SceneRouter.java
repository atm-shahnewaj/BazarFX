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
 * A single Scene is created once and reused for the whole application lifetime;
 * every screen change just swaps that Scene's root node (scene.setRoot(...)) instead
 * of replacing the Scene object itself. Calling stage.setScene(new Scene(...)) on every
 * navigation - which is what this class used to do - is what caused the "window drops
 * out of maximized / shrinks back to a small size" bug: swapping the Scene forces the
 * Stage to re-run its layout pass against that new Scene's own preferred size, which
 * on several platforms silently clears the maximized flag. Reusing one Scene avoids the
 * problem entirely and the window's size/maximized state now survives every navigation.
 *
 * Also owns the app's "responsive scaling": every screen's base font-size is
 * recalculated whenever the window is resized, so text, spacing (expressed in
 * `em` in style.css) and controls grow/shrink together instead of staying
 * pinned at a fixed pixel size regardless of window size.
 */
public class SceneRouter {

    private static final double BASE_FONT_PX = 14.0;

    // Auth screens (login/signup) use a tighter scale band than the main shell: their
    // forms have a fixed amount of content, so letting the font balloon past ~1.15x on a
    // maximized window was what made Sign Up overflow its own layout and pop a scrollbar
    // that never went away.
    private static final double AUTH_MIN_SCALE = 0.88;
    private static final double AUTH_MAX_SCALE = 1.15;
    private static final double SHELL_MIN_SCALE = 0.82;
    private static final double SHELL_MAX_SCALE = 1.45;

    private final Stage stage;
    private Scene scene;
    private ShellController shellController;

    public SceneRouter(Stage stage) {
        this.stage = stage;
    }

    /** Used for the pre-login screens (Login / Signup), which replace the whole scene root. */
    public void showAuthScreen(String fxmlName, String title) {
        Parent root = load(fxmlName);
        stage.setTitle(title);
        stage.setMinWidth(440);
        stage.setMinHeight(560);
        setRoot(root, "/com/bazarfx/css/style.css", 480, 700, AUTH_MIN_SCALE, AUTH_MAX_SCALE);
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
        stage.setTitle("BazarFX");
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        setRoot(shellRoot, "/com/bazarfx/css/style.css", 1180, 760, SHELL_MIN_SCALE, SHELL_MAX_SCALE);
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

    /** Creates the shared Scene on first use (preserving whatever size/maximized state the
     *  Stage already has), or just swaps its root and stylesheet on every call after that. */
    private void setRoot(Parent root, String stylesheet, double baseWidth, double baseHeight, double minScale, double maxScale) {
        if (scene == null) {
            scene = new Scene(root, baseWidth, baseHeight);
            stage.setScene(scene);
        } else {
            scene.setRoot(root);
        }
        String css = getClass().getResource(stylesheet).toExternalForm();
        if (!scene.getStylesheets().contains(css)) {
            scene.getStylesheets().setAll(css);
        }
        bindResponsiveScale(root, scene, baseWidth, baseHeight, minScale, maxScale);
    }

    /**
     * Ties the root node's base font-size to the current window size, relative
     * to the design-time base size for that screen. Because every text and
     * spacing rule in style.css is written in `em`, this single binding makes
     * the whole screen (fonts, paddings, card sizes, button sizes, ...) scale
     * smoothly as the user drags the window smaller or bigger, instead of the
     * UI staying frozen at one fixed pixel size.
     */
    private void bindResponsiveScale(Parent root, Scene scene, double baseWidth, double baseHeight,
                                      double minScale, double maxScale) {
        Runnable update = () -> {
            double widthScale = scene.getWidth() / baseWidth;
            double heightScale = scene.getHeight() / baseHeight;
            double scale = Math.min(widthScale, heightScale);
            scale = Math.max(minScale, Math.min(maxScale, scale));
            root.setStyle("-fx-font-size: " + (BASE_FONT_PX * scale) + "px;");
        };
        scene.widthProperty().addListener((obs, oldVal, newVal) -> update.run());
        scene.heightProperty().addListener((obs, oldVal, newVal) -> update.run());
        update.run();
    }
}
