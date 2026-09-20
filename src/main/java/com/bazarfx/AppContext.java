package com.bazarfx;

import com.bazarfx.concurrency.*;
import com.bazarfx.notification.OrderStatusNotification;
import com.bazarfx.service.*;
import com.bazarfx.storage.FileStorageManager;

/**
 * Simple hand-rolled service locator (no DI framework) so FXML controllers, which JavaFX
 * instantiates for us via reflection, can reach the shared storage/service/concurrency
 * singletons without constructor injection. Built once in Main.start() and read everywhere else.
 */
public class AppContext {

    private static AppContext instance;

    public final FileStorageManager storage;
    public final AuthService authService;
    public final ProductService productService;
    public final OrderService orderService;
    public final ReviewService reviewService;
    public final ReportService reportService;
    public final MessagingService messagingService;

    public final ImageProcessor imageProcessor;
    public final SearchIndex searchIndex;
    public final OrderStatusSimulator orderStatusSimulator;
    public final NotificationPoller notificationPoller;
    public final AutoSaveService autoSaveService;
    public final ReportGenerator reportGenerator;

    public SceneRouter router; // set by Main once the primary Stage exists
    public com.bazarfx.controller.ShellController shellController; // set by ShellController.initialize()

    private AppContext(String dataDir) {
        storage = new FileStorageManager(dataDir);

        authService = new AuthService(storage);
        productService = new ProductService(storage);
        orderService = new OrderService(storage);
        reviewService = new ReviewService(storage);
        reportService = new ReportService(storage);
        messagingService = new MessagingService(storage);

        imageProcessor = new ImageProcessor(storage);
        searchIndex = new SearchIndex(storage);
        notificationPoller = new NotificationPoller(event -> {
            System.out.println("[Notification] " + event.getMessage());
            if (shellController != null) shellController.showToast(event);
        });
        orderStatusSimulator = new OrderStatusSimulator(orderService, order ->
                notificationPoller.publish(new OrderStatusNotification(order)));
        autoSaveService = new AutoSaveService(storage);
        reportGenerator = new ReportGenerator(reportService);
    }

    public static AppContext init(String dataDir) {
        if (instance == null) instance = new AppContext(dataDir);
        return instance;
    }

    public static AppContext get() {
        if (instance == null) throw new IllegalStateException("AppContext not initialized yet");
        return instance;
    }

    public void startBackgroundServices() {
        searchIndex.rebuildAsync();
        notificationPoller.start(3000);
        orderStatusSimulator.start(15);
        autoSaveService.start(30);
        ExchangeRateFetcher.getInstance().start();
    }

    public void shutdownBackgroundServices() {
        imageProcessor.shutdown();
        orderStatusSimulator.stop();
        notificationPoller.stop();
        autoSaveService.stop();
        reportGenerator.shutdown();
        ExchangeRateFetcher.getInstance().stop();
    }
}
