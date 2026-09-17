package com.bazarfx.util;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Pure-JavaFX image copy + thumbnail generation. Runs on a background thread
 * (see concurrency.ImageProcessor) so the calling code must not touch JavaFX
 * UI nodes directly from here.
 */
public class ImageUtils {

    public static final int THUMBNAIL_SIZE = 200;

    /** Copies the source photo into the product's image folder, returning the stored path. */
    public static String storeOriginal(File source, Path targetDir, String fileNamePrefix) throws IOException {
        Files.createDirectories(targetDir);
        String ext = extensionOf(source.getName());
        Path target = targetDir.resolve(fileNamePrefix + "_original" + ext);
        Files.copy(source.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
        return target.toString();
    }

    /** Generates a square-ish thumbnail next to the original and returns its path. */
    public static String generateThumbnail(String originalPath, Path targetDir, String fileNamePrefix) throws IOException {
        Image original = new Image(new File(originalPath).toURI().toString());
        double scale = THUMBNAIL_SIZE / Math.max(original.getWidth(), original.getHeight());
        int w = Math.max(1, (int) (original.getWidth() * scale));
        int h = Math.max(1, (int) (original.getHeight() * scale));

        WritableImage scaled = new WritableImage(w, h);
        PixelReader reader = original.getPixelReader();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int srcX = Math.min((int) (x / scale), (int) original.getWidth() - 1);
                int srcY = Math.min((int) (y / scale), (int) original.getHeight() - 1);
                scaled.getPixelWriter().setArgb(x, y, reader.getArgb(srcX, srcY));
            }
        }

        Files.createDirectories(targetDir);
        Path target = targetDir.resolve(fileNamePrefix + "_thumb.png");
        ImageIO.write(SwingFXUtils.fromFXImage(scaled, null), "png", target.toFile());
        return target.toString();
    }

    private static String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot >= 0 ? fileName.substring(dot) : ".png";
    }
}
