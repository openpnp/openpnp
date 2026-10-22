package org.openpnp.vision.gpu;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Lazily creates GPU objects by key and closes them once they have not been used for a while.
 * Keys must be value objects (records, lists of settings, ...).
 */
public class GpuCache<K, V extends GpuResource> {
    private static final List<WeakReference<GpuCache<?, ?>>> caches = new ArrayList<>();
    private static ScheduledExecutorService sweeper;

    private final long idleNs;
    private final Map<K, Entry<V>> entries = new HashMap<>();

    private static class Entry<V> {
        final V value;
        long lastUsed;

        Entry(V value) {
            this.value = value;
        }
    }

    public GpuCache(long idle, TimeUnit unit) {
        this.idleNs = unit.toNanos(idle);
        register(this);
    }

    public synchronized V get(K key, Function<K, V> create) {
        Entry<V> entry = entries.get(key);
        if (entry == null || entry.value.isClosed()) {
            entry = new Entry<>(create.apply(key));
            entries.put(key, entry);
        }
        entry.lastUsed = System.nanoTime();
        return entry.value;
    }

    public synchronized int size() {
        return entries.size();
    }

    public synchronized void clear() {
        for (Entry<V> entry : entries.values()) {
            entry.value.close();
        }
        entries.clear();
    }

    synchronized void sweep(long now) {
        for (Iterator<Entry<V>> it = entries.values().iterator(); it.hasNext();) {
            Entry<V> entry = it.next();
            if (now - entry.lastUsed > idleNs) {
                entry.value.close();
                it.remove();
            }
        }
    }

    private static synchronized void register(GpuCache<?, ?> cache) {
        caches.add(new WeakReference<>(cache));
        if (sweeper == null) {
            sweeper = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread thread = new Thread(r, "GpuCache sweeper");
                thread.setDaemon(true);
                return thread;
            });
            sweeper.scheduleWithFixedDelay(GpuCache::sweepAll, 10, 10, TimeUnit.SECONDS);
        }
    }

    private static void sweepAll() {
        List<GpuCache<?, ?>> snapshot = new ArrayList<>();
        synchronized (GpuCache.class) {
            for (Iterator<WeakReference<GpuCache<?, ?>>> it = caches.iterator(); it.hasNext();) {
                GpuCache<?, ?> cache = it.next().get();
                if (cache == null) {
                    it.remove();
                }
                else {
                    snapshot.add(cache);
                }
            }
        }
        long now = System.nanoTime();
        for (GpuCache<?, ?> cache : snapshot) {
            cache.sweep(now);
        }
    }
}
