package com.bazarfx.concurrency;

import com.bazarfx.dao.OrderDao;
import com.bazarfx.model.Order;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class OrderStatusSimulator {
    private static OrderStatusSimulator instance;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final OrderDao orderDao = new OrderDao();

    private OrderStatusSimulator() {}

    public static synchronized OrderStatusSimulator getInstance() {
        if (instance == null) instance = new OrderStatusSimulator();
        return instance;
    }

    public void start() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                List<Order> orders = orderDao.getAllOrders();
                for (Order o : orders) {
                    if ("Pending".equals(o.getStatus())) {
                        orderDao.updateOrderStatus(o.getOrderId(), "Confirmed");
                    } else if ("Confirmed".equals(o.getStatus())) {
                        orderDao.updateOrderStatus(o.getOrderId(), "Shipped");
                    } else if ("Shipped".equals(o.getStatus())) {
                        orderDao.updateOrderStatus(o.getOrderId(), "Delivered");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 8, 10, TimeUnit.SECONDS);
    }

    public void stop() {
        scheduler.shutdownNow();
    }
}
