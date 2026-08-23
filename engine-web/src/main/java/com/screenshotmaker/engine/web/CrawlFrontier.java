package com.screenshotmaker.engine.web;

import java.net.URI;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread-safe BFS work queue shared by every crawl worker: deduplicates URLs, enforces the
 * max-items and max-depth caps, and knows when the whole crawl is finished.
 *
 * <p>Completion is tracked with an in-flight counter rather than just "queue is empty", because a
 * worker that has taken an entry off the queue may still enqueue that page's links; the crawl is
 * only really done once nothing is queued <em>and</em> no worker could possibly add more work.
 */
public final class CrawlFrontier {

    private final Set<URI> visited = ConcurrentHashMap.newKeySet();
    private final BlockingQueue<Entry> queue = new LinkedBlockingQueue<>();
    private final AtomicInteger totalOffered = new AtomicInteger(0);
    private final AtomicInteger inFlight = new AtomicInteger(0);
    private final int maxItems;
    private final int maxDepth;

    public CrawlFrontier(URI seed, int maxItems, int maxDepth) {
        this.maxItems = maxItems;
        this.maxDepth = maxDepth;
        offer(seed, 0);
    }

    /** Attempts to enqueue a URL at the given depth; returns false if rejected (seen, too deep, over cap). */
    public boolean offer(URI url, int depth) {
        if (depth > maxDepth || totalOffered.get() >= maxItems) {
            return false;
        }
        if (!visited.add(url)) {
            return false;
        }
        if (totalOffered.incrementAndGet() > maxItems) {
            visited.remove(url);
            return false;
        }
        inFlight.incrementAndGet();
        queue.add(new Entry(url, depth));
        return true;
    }

    /** Waits up to {@code timeout} for an entry; returns null if none arrived in time. */
    public Entry poll(Duration timeout) throws InterruptedException {
        return queue.poll(timeout.toMillis(), TimeUnit.MILLISECONDS);
    }

    /** Must be called exactly once per entry returned by {@link #poll}, after all its children have been offered. */
    public void markProcessed(Entry entry) {
        inFlight.decrementAndGet();
    }

    /** True once no entry is queued and no worker could possibly enqueue another. */
    public boolean isDone() {
        return queue.isEmpty() && inFlight.get() == 0;
    }

    public int visitedCount() {
        return visited.size();
    }

    public record Entry(URI url, int depth) {
    }
}
