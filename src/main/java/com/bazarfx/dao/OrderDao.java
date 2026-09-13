package com.bazarfx.dao;

import com.bazarfx.model.Order;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OrderDao {

    public boolean createOrder(Order order) {
        String sql = "INSERT INTO orders (buyer_id, product_id, total_price_bdt, status) VALUES (?, ?, ?, 'Pending')";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, order.getBuyerId());
            pstmt.setInt(2, order.getProductId());
            pstmt.setDouble(3, order.getTotalPriceBdt());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Order> getAllOrders() {
        List<Order> list = new ArrayList<>();
        String sql = "SELECT o.order_id, o.buyer_id, o.product_id, p.title as product_title, o.total_price_bdt, o.status, o.created_at " +
                     "FROM orders o JOIN products p ON o.product_id = p.product_id ORDER BY o.created_at DESC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                list.add(new Order(
                        rs.getInt("order_id"),
                        rs.getInt("buyer_id"),
                        rs.getInt("product_id"),
                        rs.getString("product_title"),
                        rs.getDouble("total_price_bdt"),
                        rs.getString("status"),
                        rs.getTimestamp("created_at")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public void updateOrderStatus(int orderId, String newStatus) {
        String sql = "UPDATE orders SET status = ? WHERE order_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newStatus);
            pstmt.setInt(2, orderId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
