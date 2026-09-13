package com.bazarfx.dao;

import com.bazarfx.model.Product;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductDao {

    public List<Product> getAllActiveProducts() {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT * FROM products WHERE status = 'Active' ORDER BY created_at DESC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                products.add(new Product(
                        rs.getInt("product_id"),
                        rs.getInt("seller_id"),
                        rs.getInt("category_id"),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getDouble("price_bdt"),
                        rs.getString("condition"),
                        rs.getString("status"),
                        rs.getString("location"),
                        rs.getString("image_path"),
                        rs.getInt("view_count")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return products;
    }

    public boolean addProduct(Product p) {
        String sql = "INSERT INTO products (seller_id, category_id, title, description, price_bdt, condition, status, location, image_path) " +
                     "VALUES (?, ?, ?, ?, ?, ?, 'Active', ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, p.getSellerId());
            pstmt.setInt(2, p.getCategoryId());
            pstmt.setString(3, p.getTitle());
            pstmt.setString(4, p.getDescription());
            pstmt.setDouble(5, p.getPriceBdt());
            pstmt.setString(6, p.getCondition());
            pstmt.setString(7, p.getLocation());
            pstmt.setString(8, p.getImagePath());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
