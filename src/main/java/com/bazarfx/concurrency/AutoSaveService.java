package com.bazarfx.concurrency;

import com.bazarfx.storage.FileStorageManager;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Periodic background thread that flushes all in-memory state to disk with synchronized
 * file writes, so no work is lost even if the app closes unexpectedly (see FileStorageManager
 * for the per-file locking that makes concurrent flushAll() calls safe).
 */
public class AutoSaveService {

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "auto-save");
        t.setDaemon(true);
        return t;
    });

    private final FileStorageManager storage;

    public AutoSaveService(FileStorageManager storage) {
        this.storage = storage;
    }

    public void start(long intervalSeconds) {
        scheduler.scheduleAtFixedRate(storage::flushAll, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
    }

    public void stop() {
        scheduler.shutdown();
        storage.flushAll(); // final save on shutdown
    }
}
