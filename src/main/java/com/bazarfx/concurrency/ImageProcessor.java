package com.bazarfx.concurrency;

import com.bazarfx.model.Product;
import com.bazarfx.storage.FileStorageManager;
import com.bazarfx.util.ImageUtils;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Thread pool that offloads photo resize/thumbnail work off the JavaFX Application Thread.
 * A listing is published immediately with its original photos; thumbnails pop in once the
 * background task completes, without freezing the "Sell an Item" form.
 */
public class ImageProcessor {

    public interface Callback {
        void onThumbnailsReady(Product product, List<String> thumbnailPaths);
        void onFailure(Product product, Exception e);
    }

    private final ExecutorService pool = Executors.newFixedThreadPool(
            Math.max(2, Runtime.getRuntime().availableProcessors() / 2));
    private final FileStorageManager storage;

    public ImageProcessor(FileStorageManager storage) {
        this.storage = storage;
    }

    /** Submits background processing for a newly-listed product's photos; returns immediately. */
    public Future<List<String>> processAsync(Product product, List<File> sourcePhotos, Callback callback) {
        return pool.submit(() -> {
            try {
                Path targetDir = storage.imagesDir().resolve(product.getId());
                List<String> originals = new ArrayList<>();
                List<String> thumbnails = new ArrayList<>();

                int i = 0;
                for (File source : sourcePhotos) {
                    String prefix = "photo" + (i++);
                    String originalPath = ImageUtils.storeOriginal(source, targetDir, prefix);
                    String thumbPath = ImageUtils.generateThumbnail(originalPath, targetDir, prefix);
                    originals.add(originalPath);
                    thumbnails.add(thumbPath);
                }

                product.setPhotoPaths(originals);
                product.setThumbnailPaths(thumbnails);
                storage.saveProduct(product);

                if (callback != null) callback.onThumbnailsReady(product, thumbnails);
                return thumbnails;
            } catch (Exception e) {
                if (callback != null) callback.onFailure(product, e);
                throw e;
            }
        });
    }

    public void shutdown() {
        pool.shutdown();
    }
}
