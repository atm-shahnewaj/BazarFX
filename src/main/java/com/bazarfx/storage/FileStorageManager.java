package com.bazarfx.storage;

import com.bazarfx.model.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Single point of contact for the app's SQLite database and the local images folder.
 * Despite the historical class name (kept so the rest of the codebase - AppContext,
 * every service, several controllers - didn't need to change), this is a genuine
 * relational persistence layer: users / products / orders / reviews / messages live
 * in real SQLite tables (see {@link #createSchema(Connection)}), with product photos
 * and order line-items each broken out into their own child table (product_images,
 * order_items) related back to their parent by foreign key - a proper 1:N relationship,
 * not a JSON blob.
 *
 * A small in-memory cache (ConcurrentHashMap for users/products, synchronized lists for
 * orders/reviews/messages) sits in front of the database so the rest of the app can keep
 * reading data the way it always did (usersMap(), productsMap(), ...) without a DB round
 * trip on every read; every mutation writes straight through to SQLite so nothing is lost,
 * and the whole cache is rebuilt from the database on startup.
 */
public class FileStorageManager {

    private final Path dataDir;
    private final Connection connection;
    private final Object dbLock = new Object();

    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final Map<String, Product> products = new ConcurrentHashMap<>();
    private final List<Order> orders = new ArrayList<>();
    private final List<Review> reviews = new ArrayList<>();
    private final List<Message> messages = new ArrayList<>();

    public FileStorageManager(String dataDirPath) {
        this.dataDir = Paths.get(dataDirPath);
        try {
            Files.createDirectories(dataDir);
            Files.createDirectories(imagesDir());
        } catch (IOException e) {
            throw new IllegalStateException("Could not create data directory: " + dataDir, e);
        }
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dataDir.resolve("bazarfx.db"));
            try (Statement st = connection.createStatement()) {
                st.execute("PRAGMA foreign_keys = ON;");
            }
            createSchema(connection);
        } catch (ClassNotFoundException | SQLException e) {
            throw new IllegalStateException("Could not open SQLite database", e);
        }
        loadAll();
        if (users.isEmpty()) {
            DemoDataSeeder.seed(this);
        }
    }

    public Path dataDir() { return dataDir; }
    public Path imagesDir() { return dataDir.resolve("images"); }

    // ================= Schema =================

    private void createSchema(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "user_id TEXT PRIMARY KEY, username TEXT UNIQUE NOT NULL, email TEXT UNIQUE NOT NULL, " +
                    "phone TEXT, location TEXT, password_hash TEXT NOT NULL, password_salt TEXT NOT NULL, " +
                    "profile_photo_path TEXT, seller_rating_avg REAL DEFAULT 0, seller_rating_count INTEGER DEFAULT 0, " +
                    "created_at INTEGER, wishlist TEXT DEFAULT '')");

            st.execute("CREATE TABLE IF NOT EXISTS products (" +
                    "product_id TEXT PRIMARY KEY, seller_id TEXT NOT NULL, title TEXT NOT NULL, description TEXT, " +
                    "category TEXT NOT NULL, price REAL NOT NULL, condition TEXT NOT NULL, status TEXT NOT NULL, " +
                    "view_count INTEGER DEFAULT 0, created_at INTEGER, updated_at INTEGER, " +
                    "FOREIGN KEY (seller_id) REFERENCES users(user_id) ON DELETE CASCADE)");

            // 1:N relationship - every photo (original or generated thumbnail) a product has.
            st.execute("CREATE TABLE IF NOT EXISTS product_images (" +
                    "image_id INTEGER PRIMARY KEY AUTOINCREMENT, product_id TEXT NOT NULL, kind TEXT NOT NULL, " +
                    "position INTEGER NOT NULL, path TEXT NOT NULL, " +
                    "FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE)");

            st.execute("CREATE TABLE IF NOT EXISTS orders (" +
                    "order_id TEXT PRIMARY KEY, buyer_id TEXT NOT NULL, seller_id TEXT NOT NULL, " +
                    "total_price REAL NOT NULL, status TEXT NOT NULL, created_at INTEGER, last_status_change_at INTEGER, " +
                    "FOREIGN KEY (buyer_id) REFERENCES users(user_id), FOREIGN KEY (seller_id) REFERENCES users(user_id))");

            // 1:N relationship - every line item that belongs to an order.
            st.execute("CREATE TABLE IF NOT EXISTS order_items (" +
                    "item_id INTEGER PRIMARY KEY AUTOINCREMENT, order_id TEXT NOT NULL, product_id TEXT, " +
                    "title_snapshot TEXT, price_snapshot REAL, quantity INTEGER, " +
                    "FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE)");

            st.execute("CREATE TABLE IF NOT EXISTS reviews (" +
                    "review_id TEXT PRIMARY KEY, order_id TEXT, buyer_id TEXT NOT NULL, seller_id TEXT NOT NULL, " +
                    "rating INTEGER NOT NULL, comment TEXT, created_at INTEGER, flagged INTEGER DEFAULT 0, " +
                    "FOREIGN KEY (buyer_id) REFERENCES users(user_id), FOREIGN KEY (seller_id) REFERENCES users(user_id))");

            st.execute("CREATE TABLE IF NOT EXISTS messages (" +
                    "message_id TEXT PRIMARY KEY, product_id TEXT NOT NULL, buyer_id TEXT NOT NULL, seller_id TEXT NOT NULL, " +
                    "sender_id TEXT NOT NULL, body TEXT NOT NULL, sent_at INTEGER, is_read INTEGER DEFAULT 0, " +
                    "FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE)");
        }
    }

    // ================= Users =================

    public Map<String, User> usersMap() { return users; }

    public void saveUser(User user) {
        users.put(user.getId(), user);
        synchronized (dbLock) {
            String sql = "INSERT INTO users (user_id, username, email, phone, location, password_hash, password_salt, " +
                    "profile_photo_path, seller_rating_avg, seller_rating_count, created_at, wishlist) VALUES (?,?,?,?,?,?,?,?,?,?,?,?) " +
                    "ON CONFLICT(user_id) DO UPDATE SET username=excluded.username, email=excluded.email, phone=excluded.phone, " +
                    "location=excluded.location, password_hash=excluded.password_hash, password_salt=excluded.password_salt, " +
                    "profile_photo_path=excluded.profile_photo_path, seller_rating_avg=excluded.seller_rating_avg, " +
                    "seller_rating_count=excluded.seller_rating_count, wishlist=excluded.wishlist";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, user.getId());
                ps.setString(2, user.getUsername());
                ps.setString(3, user.getEmail());
                ps.setString(4, user.getPhone());
                ps.setString(5, user.getLocation());
                ps.setString(6, user.getPasswordHash());
                ps.setString(7, user.getPasswordSalt());
                ps.setString(8, user.getProfilePhotoPath());
                ps.setDouble(9, user.getSellerRatingAvg());
                ps.setInt(10, user.getSellerRatingCount());
                ps.setLong(11, user.getCreatedAt());
                ps.setString(12, String.join(",", user.getWishlistProductIds()));
                ps.executeUpdate();
            } catch (SQLException e) {
                System.err.println("[Storage] Failed to save user: " + e.getMessage());
            }
        }
    }

    // ================= Products =================

    public Map<String, Product> productsMap() { return products; }

    public void saveProduct(Product product) {
        products.put(product.getId(), product);
        synchronized (dbLock) {
            String sql = "INSERT INTO products (product_id, seller_id, title, description, category, price, condition, " +
                    "status, view_count, created_at, updated_at) VALUES (?,?,?,?,?,?,?,?,?,?,?) " +
                    "ON CONFLICT(product_id) DO UPDATE SET title=excluded.title, description=excluded.description, " +
                    "category=excluded.category, price=excluded.price, condition=excluded.condition, status=excluded.status, " +
                    "view_count=excluded.view_count, updated_at=excluded.updated_at";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, product.getId());
                ps.setString(2, product.getSellerId());
                ps.setString(3, product.getTitle());
                ps.setString(4, product.getDescription());
                ps.setString(5, product.getCategory() == null ? null : product.getCategory().name());
                ps.setDouble(6, product.getPrice());
                ps.setString(7, product.getCondition() == null ? null : product.getCondition().name());
                ps.setString(8, product.getStatus().name());
                ps.setInt(9, product.getViewCount());
                ps.setLong(10, product.getCreatedAt());
                ps.setLong(11, product.getUpdatedAt());
                ps.executeUpdate();
            } catch (SQLException e) {
                System.err.println("[Storage] Failed to save product: " + e.getMessage());
                return;
            }
            saveProductImages(product);
        }
    }

    /** Replaces every photo/thumbnail row for this product - simplest correct way to keep the
     *  child table in sync whenever a listing's photo list changes. */
    private void saveProductImages(Product product) {
        try (PreparedStatement del = connection.prepareStatement("DELETE FROM product_images WHERE product_id = ?")) {
            del.setString(1, product.getId());
            del.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[Storage] Failed to clear product images: " + e.getMessage());
            return;
        }
        String insertSql = "INSERT INTO product_images (product_id, kind, position, path) VALUES (?,?,?,?)";
        try (PreparedStatement ins = connection.prepareStatement(insertSql)) {
            int pos = 0;
            for (String path : product.getPhotoPaths()) {
                ins.setString(1, product.getId());
                ins.setString(2, "PHOTO");
                ins.setInt(3, pos++);
                ins.setString(4, path);
                ins.addBatch();
            }
            pos = 0;
            for (String path : product.getThumbnailPaths()) {
                ins.setString(1, product.getId());
                ins.setString(2, "THUMB");
                ins.setInt(3, pos++);
                ins.setString(4, path);
                ins.addBatch();
            }
            ins.executeBatch();
        } catch (SQLException e) {
            System.err.println("[Storage] Failed to save product images: " + e.getMessage());
        }
    }

    public void deleteProduct(String productId) {
        products.remove(productId);
        synchronized (dbLock) {
            try (PreparedStatement ps = connection.prepareStatement("DELETE FROM products WHERE product_id = ?")) {
                ps.setString(1, productId);
                ps.executeUpdate();
            } catch (SQLException e) {
                System.err.println("[Storage] Failed to delete product: " + e.getMessage());
            }
        }
    }

    // ================= Orders =================

    public List<Order> ordersSnapshot() {
        synchronized (orders) {
            return new ArrayList<>(orders);
        }
    }

    public void addOrder(Order order) {
        synchronized (orders) {
            orders.add(order);
        }
        persistOrder(order);
    }

    /** Persists the current in-memory state of one order (call after mutating an Order in place). */
    public void flushOrders() {
        List<Order> snapshot = ordersSnapshot();
        for (Order order : snapshot) {
            persistOrder(order);
        }
    }

    private void persistOrder(Order order) {
        synchronized (dbLock) {
            String sql = "INSERT INTO orders (order_id, buyer_id, seller_id, total_price, status, created_at, last_status_change_at) " +
                    "VALUES (?,?,?,?,?,?,?) ON CONFLICT(order_id) DO UPDATE SET status=excluded.status, " +
                    "last_status_change_at=excluded.last_status_change_at";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, order.getId());
                ps.setString(2, order.getBuyerId());
                ps.setString(3, order.getSellerId());
                ps.setDouble(4, order.getTotalPrice());
                ps.setString(5, order.getStatus().name());
                ps.setLong(6, order.getCreatedAt());
                ps.setLong(7, order.getLastStatusChangeAt());
                ps.executeUpdate();
            } catch (SQLException e) {
                System.err.println("[Storage] Failed to save order: " + e.getMessage());
                return;
            }
            try (PreparedStatement del = connection.prepareStatement("DELETE FROM order_items WHERE order_id = ?")) {
                del.setString(1, order.getId());
                del.executeUpdate();
            } catch (SQLException e) {
                System.err.println("[Storage] Failed to clear order items: " + e.getMessage());
                return;
            }
            String insertSql = "INSERT INTO order_items (order_id, product_id, title_snapshot, price_snapshot, quantity) VALUES (?,?,?,?,?)";
            try (PreparedStatement ins = connection.prepareStatement(insertSql)) {
                for (OrderItem item : order.getItems()) {
                    ins.setString(1, order.getId());
                    ins.setString(2, item.getProductId());
                    ins.setString(3, item.getProductTitleSnapshot());
                    ins.setDouble(4, item.getPriceSnapshot());
                    ins.setInt(5, item.getQuantity());
                    ins.addBatch();
                }
                ins.executeBatch();
            } catch (SQLException e) {
                System.err.println("[Storage] Failed to save order items: " + e.getMessage());
            }
        }
    }

    // ================= Reviews =================

    public List<Review> reviewsSnapshot() {
        synchronized (reviews) {
            return new ArrayList<>(reviews);
        }
    }

    public void addReview(Review review) {
        synchronized (reviews) {
            reviews.add(review);
        }
        persistReview(review);
    }

    public void updateReview(Review updated) {
        synchronized (reviews) {
            for (int i = 0; i < reviews.size(); i++) {
                if (reviews.get(i).getId().equals(updated.getId())) {
                    reviews.set(i, updated);
                    break;
                }
            }
        }
        persistReview(updated);
    }

    private void persistReview(Review review) {
        synchronized (dbLock) {
            String sql = "INSERT INTO reviews (review_id, order_id, buyer_id, seller_id, rating, comment, created_at, flagged) " +
                    "VALUES (?,?,?,?,?,?,?,?) ON CONFLICT(review_id) DO UPDATE SET rating=excluded.rating, " +
                    "comment=excluded.comment, flagged=excluded.flagged";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, review.getId());
                ps.setString(2, review.getOrderId());
                ps.setString(3, review.getBuyerId());
                ps.setString(4, review.getSellerId());
                ps.setInt(5, review.getRating());
                ps.setString(6, review.getComment());
                ps.setLong(7, review.getCreatedAt());
                ps.setInt(8, review.isFlagged() ? 1 : 0);
                ps.executeUpdate();
            } catch (SQLException e) {
                System.err.println("[Storage] Failed to save review: " + e.getMessage());
            }
        }
    }

    // ================= Messages (buyer <-> seller chat) =================

    public List<Message> messagesSnapshot() {
        synchronized (messages) {
            return new ArrayList<>(messages);
        }
    }

    public void addMessage(Message message) {
        synchronized (messages) {
            messages.add(message);
        }
        synchronized (dbLock) {
            String sql = "INSERT INTO messages (message_id, product_id, buyer_id, seller_id, sender_id, body, sent_at, is_read) " +
                    "VALUES (?,?,?,?,?,?,?,?)";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, message.getId());
                ps.setString(2, message.getProductId());
                ps.setString(3, message.getBuyerId());
                ps.setString(4, message.getSellerId());
                ps.setString(5, message.getSenderId());
                ps.setString(6, message.getBody());
                ps.setLong(7, message.getSentAt());
                ps.setInt(8, message.isRead() ? 1 : 0);
                ps.executeUpdate();
            } catch (SQLException e) {
                System.err.println("[Storage] Failed to save message: " + e.getMessage());
            }
        }
    }

    // ================= Bulk flush (used by AutoSaveService) =================

    /** Everything here already writes straight through to SQLite, so a periodic flush only
     *  needs to re-persist the mutable, in-place-edited collections (orders' evolving status). */
    public void flushAll() {
        flushOrders();
    }

    // ================= Loading =================

    private void loadAll() {
        synchronized (dbLock) {
            try (Statement st = connection.createStatement()) {
                try (ResultSet rs = st.executeQuery("SELECT * FROM users")) {
                    while (rs.next()) {
                        User u = new User(rs.getString("user_id"), rs.getString("username"), rs.getString("email"),
                                rs.getString("phone"), rs.getString("location"), rs.getString("password_hash"), rs.getString("password_salt"));
                        u.setProfilePhotoPath(rs.getString("profile_photo_path"));
                        u.setSellerRatingAvg(rs.getDouble("seller_rating_avg"));
                        u.setSellerRatingCount(rs.getInt("seller_rating_count"));
                        u.setCreatedAt(rs.getLong("created_at"));
                        String wishlist = rs.getString("wishlist");
                        if (wishlist != null && !wishlist.isBlank()) {
                            u.getWishlistProductIds().addAll(List.of(wishlist.split(",")));
                        }
                        users.put(u.getId(), u);
                    }
                }

                try (ResultSet rs = st.executeQuery("SELECT * FROM products")) {
                    while (rs.next()) {
                        Product p = new Product();
                        p.setId(rs.getString("product_id"));
                        p.setSellerId(rs.getString("seller_id"));
                        p.setTitle(rs.getString("title"));
                        p.setDescription(rs.getString("description"));
                        p.setCategory(Category.valueOf(rs.getString("category")));
                        p.setPrice(rs.getDouble("price"));
                        p.setCondition(Condition.valueOf(rs.getString("condition")));
                        p.setStatus(ListingStatus.valueOf(rs.getString("status")));
                        p.setViewCount(rs.getInt("view_count"));
                        p.setCreatedAt(rs.getLong("created_at"));
                        p.setUpdatedAt(rs.getLong("updated_at"));
                        products.put(p.getId(), p);
                    }
                }
                try (ResultSet rs = st.executeQuery("SELECT * FROM product_images ORDER BY product_id, kind, position")) {
                    while (rs.next()) {
                        Product p = products.get(rs.getString("product_id"));
                        if (p == null) continue;
                        if ("PHOTO".equals(rs.getString("kind"))) {
                            p.getPhotoPaths().add(rs.getString("path"));
                        } else {
                            p.getThumbnailPaths().add(rs.getString("path"));
                        }
                    }
                }

                try (ResultSet rs = st.executeQuery("SELECT * FROM orders")) {
                    while (rs.next()) {
                        Order o = new Order();
                        o.setId(rs.getString("order_id"));
                        o.setBuyerId(rs.getString("buyer_id"));
                        o.setSellerId(rs.getString("seller_id"));
                        o.setTotalPrice(rs.getDouble("total_price"));
                        o.setStatus(OrderStatus.valueOf(rs.getString("status")));
                        o.setCreatedAt(rs.getLong("created_at"));
                        o.setLastStatusChangeAt(rs.getLong("last_status_change_at"));
                        orders.add(o);
                    }
                }
                try (ResultSet rs = st.executeQuery("SELECT * FROM order_items ORDER BY item_id")) {
                    while (rs.next()) {
                        String orderId = rs.getString("order_id");
                        Order order = orders.stream().filter(o -> o.getId().equals(orderId)).findFirst().orElse(null);
                        if (order == null) continue;
                        order.getItems().add(new OrderItem(rs.getString("product_id"), rs.getString("title_snapshot"),
                                rs.getDouble("price_snapshot"), rs.getInt("quantity")));
                    }
                }
                orders.sort(Comparator.comparingLong(Order::getCreatedAt));

                try (ResultSet rs = st.executeQuery("SELECT * FROM reviews")) {
                    while (rs.next()) {
                        Review r = new Review(rs.getString("review_id"), rs.getString("order_id"), rs.getString("buyer_id"),
                                rs.getString("seller_id"), rs.getInt("rating"), rs.getString("comment"));
                        r.setCreatedAt(rs.getLong("created_at"));
                        r.setFlagged(rs.getInt("flagged") != 0);
                        reviews.add(r);
                    }
                }

                try (ResultSet rs = st.executeQuery("SELECT * FROM messages ORDER BY sent_at")) {
                    while (rs.next()) {
                        Message m = new Message(rs.getString("message_id"), rs.getString("product_id"), rs.getString("buyer_id"),
                                rs.getString("seller_id"), rs.getString("sender_id"), rs.getString("body"));
                        m.setSentAt(rs.getLong("sent_at"));
                        m.setRead(rs.getInt("is_read") != 0);
                        messages.add(m);
                    }
                }
            } catch (SQLException e) {
                System.err.println("[Storage] Failed to load data: " + e.getMessage());
            }
        }
    }
}
