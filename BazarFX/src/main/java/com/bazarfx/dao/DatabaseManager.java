package com.bazarfx.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:bazarfx.db";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    public static void initializeDatabase() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");

            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "user_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "username TEXT UNIQUE NOT NULL," +
                    "email TEXT UNIQUE NOT NULL," +
                    "phone TEXT, location TEXT, password_hash TEXT NOT NULL, rating_avg REAL DEFAULT 0.0);");

            stmt.execute("CREATE TABLE IF NOT EXISTS categories (" +
                    "category_id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT UNIQUE NOT NULL);");

            stmt.execute("CREATE TABLE IF NOT EXISTS products (" +
                    "product_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "seller_id INTEGER NOT NULL, category_id INTEGER NOT NULL," +
                    "title TEXT NOT NULL, description TEXT, price_bdt REAL NOT NULL," +
                    "condition TEXT NOT NULL, status TEXT DEFAULT 'Active', location TEXT," +
                    "image_path TEXT, view_count INTEGER DEFAULT 0, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "FOREIGN KEY (seller_id) REFERENCES users(user_id)," +
                    "FOREIGN KEY (category_id) REFERENCES categories(category_id));");

            stmt.execute("CREATE TABLE IF NOT EXISTS orders (" +
                    "order_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "buyer_id INTEGER NOT NULL, product_id INTEGER NOT NULL," +
                    "total_price_bdt REAL NOT NULL, status TEXT DEFAULT 'Pending'," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "FOREIGN KEY (buyer_id) REFERENCES users(user_id)," +
                    "FOREIGN KEY (product_id) REFERENCES products(product_id));");

            seedInitialData(stmt);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void seedInitialData(Statement stmt) throws SQLException {
        stmt.execute("INSERT OR IGNORE INTO categories (category_id, name) VALUES (1, 'Electronics'), (2, 'Vehicles'), (3, 'Property');");
        stmt.execute("INSERT OR IGNORE INTO users (user_id, username, email, phone, location, password_hash, rating_avg) " +
                "VALUES (1, 'Shahnewaj', 'shahnewaj@kuet.ac.bd', '+8801700000000', 'Khulna', 'hashed_pass_123', 4.8);");

        stmt.execute("INSERT OR IGNORE INTO products (product_id, seller_id, category_id, title, description, price_bdt, condition, status, location, image_path, view_count) " +
                "VALUES (101, 1, 1, 'Vintage Canon Camera', '35mm SLR camera in mint condition.', 15500, 'Used', 'Active', 'Dhaka', 'https://picsum.photos/id/250/300/200', 42);");
        stmt.execute("INSERT OR IGNORE INTO products (product_id, seller_id, category_id, title, description, price_bdt, condition, status, location, image_path, view_count) " +
                "VALUES (102, 1, 1, 'ASUS TUF Gaming Laptop', 'i7, RTX 3060, 16GB RAM.', 85000, 'Used', 'Active', 'Khulna', 'https://picsum.photos/id/0/300/200', 110);");
        stmt.execute("INSERT OR IGNORE INTO products (product_id, seller_id, category_id, title, description, price_bdt, condition, status, location, image_path, view_count) " +
                "VALUES (103, 1, 3, 'Ergonomic Wooden Desk', 'Solid oak desk for workstation.', 6200, 'New', 'Active', 'Rajshahi', 'https://picsum.photos/id/20/300/200', 19);");
    }
}
