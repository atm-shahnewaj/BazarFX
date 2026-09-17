package com.bazarfx.concurrency;

import com.bazarfx.model.ListingStatus;
import com.bazarfx.model.Product;
import com.bazarfx.storage.FileStorageManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lightweight in-memory keyword index over active listings. Rebuilt incrementally on a
 * dedicated background thread whenever a listing is added or edited, so keyword search
 * stays fast without ever blocking the JavaFX Application Thread. Access to the shared
 * index map is synchronized because a rebuild (write) can overlap with a search (read).
 */
public class SearchIndex {

    // token -> set of product ids containing that token
    private final Map<String, Set<String>> index = new ConcurrentHashMap<>();
    private final Object rebuildLock = new Object();
    private final FileStorageManager storage;
    private Thread rebuildThread;

    public SearchIndex(FileStorageManager storage) {
        this.storage = storage;
    }

    /** Kicks off a full rebuild on a dedicated background Runnable/Thread. Non-blocking. */
    public void rebuildAsync() {
        rebuildThread = new Thread(this::rebuildNow, "search-index-rebuild");
        rebuildThread.setDaemon(true);
        rebuildThread.start();
    }

    private void rebuildNow() {
        synchronized (rebuildLock) {
            Map<String, Set<String>> fresh = new HashMap<>();
            for (Product p : storage.productsMap().values()) {
                if (p.getStatus() != ListingStatus.ACTIVE) continue;
                for (String token : tokenize(p.getTitle() + " " + p.getDescription())) {
                    fresh.computeIfAbsent(token, t -> new HashSet<>()).add(p.getId());
                }
            }
            index.clear();
            fresh.forEach((token, ids) -> index.put(token, ConcurrentHashMap.newKeySet(ids.size())));
            fresh.forEach((token, ids) -> index.get(token).addAll(ids));
        }
    }

    public List<String> search(String keyword) {
        if (keyword == null || keyword.isBlank()) return Collections.emptyList();
        synchronized (rebuildLock) {
            Set<String> ids = index.getOrDefault(keyword.trim().toLowerCase(), Collections.emptySet());
            return new ArrayList<>(ids);
        }
    }

    private static List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        for (String word : text.toLowerCase().split("[^a-z0-9]+")) {
            if (!word.isBlank()) tokens.add(word);
        }
        return tokens;
    }
}
