package com.bazarfx.service;

import com.bazarfx.model.Category;
import com.bazarfx.model.Order;
import com.bazarfx.model.Product;
import com.bazarfx.model.User;
import com.bazarfx.storage.FileStorageManager;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Marketplace-wide analytics. The heavy report (top categories / top-rated sellers /
 * busiest week) splits the dataset across an ExecutorService worker pool and combines
 * partial Future<> results — a direct use of the Future/Callable concurrency package,
 * distinct from the scheduled/daemon-thread concurrency used elsewhere in the app.
 */
public class ReportService {

    private final FileStorageManager storage;

    public ReportService(FileStorageManager storage) {
        this.storage = storage;
    }

    public static class SellerDashboardStats {
        public int totalListings;
        public double simulatedRevenue;
        public int totalViews;
    }

    public SellerDashboardStats sellerDashboard(String sellerId) {
        SellerDashboardStats stats = new SellerDashboardStats();
        List<Product> listings = storage.productsMap().values().stream()
                .filter(p -> p.getSellerId().equals(sellerId)).collect(Collectors.toList());
        stats.totalListings = listings.size();
        stats.totalViews = listings.stream().mapToInt(Product::getViewCount).sum();
        stats.simulatedRevenue = storage.ordersSnapshot().stream()
                .filter(o -> o.getSellerId().equals(sellerId))
                .mapToDouble(Order::getTotalPrice).sum();
        return stats;
    }

    public static class MarketplaceReport {
        public Map<Category, Long> topCategories;
        public List<Map.Entry<String, Double>> topRatedSellers; // username -> avg rating
        public Map<String, Long> busiestWeek; // ISO week -> order count
    }

    /**
     * Computes the three marketplace-wide breakdowns in parallel using an ExecutorService,
     * combining each Future's partial result. Executor is shut down before returning.
     */
    public MarketplaceReport generateMarketplaceReport() throws InterruptedException, ExecutionException {
        List<Product> products = new ArrayList<>(storage.productsMap().values());
        List<User> users = new ArrayList<>(storage.usersMap().values());
        List<Order> orders = storage.ordersSnapshot();

        ExecutorService pool = Executors.newFixedThreadPool(3);
        try {
            Future<Map<Category, Long>> categoriesFuture = pool.submit(() ->
                    products.stream().collect(Collectors.groupingBy(Product::getCategory, Collectors.counting())));

            Future<List<Map.Entry<String, Double>>> ratedSellersFuture = pool.submit(() ->
                    users.stream()
                            .filter(u -> u.getSellerRatingCount() > 0)
                            .sorted((a, b) -> Double.compare(b.getSellerRatingAvg(), a.getSellerRatingAvg()))
                            .limit(10)
                            .map(u -> Map.entry(u.getUsername(), u.getSellerRatingAvg()))
                            .collect(Collectors.toList()));

            Future<Map<String, Long>> busiestWeekFuture = pool.submit(() ->
                    orders.stream().collect(Collectors.groupingBy(
                            o -> isoWeekKey(o.getCreatedAt()), Collectors.counting())));

            MarketplaceReport report = new MarketplaceReport();
            report.topCategories = categoriesFuture.get();
            report.topRatedSellers = ratedSellersFuture.get();
            report.busiestWeek = busiestWeekFuture.get();
            return report;
        } finally {
            pool.shutdown();
        }
    }

    /** Plain-text/CSV export of a marketplace report for the "export" action on the Reports screen. */
    public String exportAsCsv(MarketplaceReport report) {
        StringBuilder sb = new StringBuilder();
        sb.append("Category,Count\n");
        report.topCategories.forEach((cat, count) -> sb.append(cat).append(',').append(count).append('\n'));
        sb.append("\nSeller,AvgRating\n");
        report.topRatedSellers.forEach(e -> sb.append(e.getKey()).append(',').append(e.getValue()).append('\n'));
        sb.append("\nWeek,OrderCount\n");
        report.busiestWeek.forEach((week, count) -> sb.append(week).append(',').append(count).append('\n'));
        return sb.toString();
    }

    private static String isoWeekKey(long epochMillis) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(epochMillis);
        int week = cal.get(Calendar.WEEK_OF_YEAR);
        int year = cal.get(Calendar.YEAR);
        return year + "-W" + week;
    }
}
