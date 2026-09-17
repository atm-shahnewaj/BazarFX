package com.bazarfx.storage;

import com.bazarfx.model.Order;
import com.bazarfx.model.Product;
import com.bazarfx.model.Review;
import com.bazarfx.model.User;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Single point of contact for reading/writing JSON files and images to disk.
 * On startup, each data file is loaded into an in-memory, thread-safe collection;
 * every mutation is written back to disk. A background auto-save thread
 * (see concurrency.AutoSaveService) periodically flushes state as a safety net.
 *
 * users.json / products.json    -> ConcurrentHashMap<id, T>   (safe for concurrent get/put)
 * orders.json / reviews.json    -> List<T>, guarded by an explicit lock for compound ops
 */
public class FileStorageManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path dataDir;
    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final Map<String, Product> products = new ConcurrentHashMap<>();
    private final List<Order> orders = new ArrayList<>();
    private final List<Review> reviews = new ArrayList<>();

    private final ReentrantLock ordersLock = new ReentrantLock();
    private final ReentrantLock reviewsLock = new ReentrantLock();

    public FileStorageManager(String dataDirPath) {
        this.dataDir = Paths.get(dataDirPath);
        try {
            Files.createDirectories(dataDir);
            Files.createDirectories(imagesDir());
        } catch (IOException e) {
            throw new IllegalStateException("Could not create data directory: " + dataDir, e);
        }
        loadAll();
    }

    public Path dataDir() { return dataDir; }
    public Path imagesDir() { return dataDir.resolve("images"); }

    // ---------- Users ----------

    public Map<String, User> usersMap() { return users; }

    public void saveUser(User user) {
        users.put(user.getId(), user);
        writeJson(usersFile(), users);
    }

    // ---------- Products ----------

    public Map<String, Product> productsMap() { return products; }

    public void saveProduct(Product product) {
        products.put(product.getId(), product);
        writeJson(productsFile(), products);
    }

    public void deleteProduct(String productId) {
        products.remove(productId);
        writeJson(productsFile(), products);
    }

    // ---------- Orders ----------

    public List<Order> ordersSnapshot() {
        ordersLock.lock();
        try {
            return new ArrayList<>(orders);
        } finally {
            ordersLock.unlock();
        }
    }

    public void addOrder(Order order) {
        ordersLock.lock();
        try {
            orders.add(order);
            writeJson(ordersFile(), orders);
        } finally {
            ordersLock.unlock();
        }
    }

    /** Persists the current in-memory state of orders (call after mutating an Order in place). */
    public void flushOrders() {
        ordersLock.lock();
        try {
            writeJson(ordersFile(), orders);
        } finally {
            ordersLock.unlock();
        }
    }

    // ---------- Reviews ----------

    public List<Review> reviewsSnapshot() {
        reviewsLock.lock();
        try {
            return new ArrayList<>(reviews);
        } finally {
            reviewsLock.unlock();
        }
    }

    public void addReview(Review review) {
        reviewsLock.lock();
        try {
            reviews.add(review);
            writeJson(reviewsFile(), reviews);
        } finally {
            reviewsLock.unlock();
        }
    }

    /** Replaces one review in place by id (e.g. after flagging it) and persists the list. */
    public void updateReview(Review updated) {
        reviewsLock.lock();
        try {
            for (int i = 0; i < reviews.size(); i++) {
                if (reviews.get(i).getId().equals(updated.getId())) {
                    reviews.set(i, updated);
                    break;
                }
            }
            writeJson(reviewsFile(), reviews);
        } finally {
            reviewsLock.unlock();
        }
    }

    // ---------- Bulk flush (used by AutoSaveService) ----------

    public synchronized void flushAll() {
        writeJson(usersFile(), users);
        writeJson(productsFile(), products);
        ordersLock.lock();
        try {
            writeJson(ordersFile(), orders);
        } finally {
            ordersLock.unlock();
        }
        reviewsLock.lock();
        try {
            writeJson(reviewsFile(), reviews);
        } finally {
            reviewsLock.unlock();
        }
    }

    // ---------- Loading ----------

    private void loadAll() {
        Type userMapType = new TypeToken<Map<String, User>>() {}.getType();
        Map<String, User> loadedUsers = readJson(usersFile(), userMapType);
        if (loadedUsers != null) users.putAll(loadedUsers);

        Type productMapType = new TypeToken<Map<String, Product>>() {}.getType();
        Map<String, Product> loadedProducts = readJson(productsFile(), productMapType);
        if (loadedProducts != null) products.putAll(loadedProducts);

        Type orderListType = new TypeToken<List<Order>>() {}.getType();
        List<Order> loadedOrders = readJson(ordersFile(), orderListType);
        if (loadedOrders != null) orders.addAll(loadedOrders);

        Type reviewListType = new TypeToken<List<Review>>() {}.getType();
        List<Review> loadedReviews = readJson(reviewsFile(), reviewListType);
        if (loadedReviews != null) reviews.addAll(loadedReviews);
    }

    private Path usersFile() { return dataDir.resolve("users.json"); }
    private Path productsFile() { return dataDir.resolve("products.json"); }
    private Path ordersFile() { return dataDir.resolve("orders.json"); }
    private Path reviewsFile() { return dataDir.resolve("reviews.json"); }

    private synchronized <T> void writeJson(Path file, T value) {
        try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            GSON.toJson(value, writer);
        } catch (IOException e) {
            System.err.println("[FileStorageManager] Failed to write " + file + ": " + e.getMessage());
        }
    }

    private synchronized <T> T readJson(Path file, Type type) {
        if (!Files.exists(file)) return null;
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return GSON.fromJson(reader, type);
        } catch (IOException e) {
            System.err.println("[FileStorageManager] Failed to read " + file + ": " + e.getMessage());
            return null;
        }
    }
}
