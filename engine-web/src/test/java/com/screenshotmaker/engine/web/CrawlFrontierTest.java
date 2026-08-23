package com.screenshotmaker.engine.web;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrawlFrontierTest {

    @Test
    void multipleWorkersVisitEveryReachableNodeExactlyOnceThenTerminate() throws InterruptedException {
        // 0 -> 1,2 ; 1 -> 3 ; 2 -> 3,4 ; 3 -> (dead end) ; 4 -> 0 (cycle back to an already-visited node)
        Map<Integer, List<Integer>> graph = Map.of(
                0, List.of(1, 2),
                1, List.of(3),
                2, List.of(3, 4),
                3, List.of(),
                4, List.of(0));

        CrawlFrontier frontier = new CrawlFrontier(nodeUri(0), 100, 10);
        ConcurrentMap<URI, Integer> processedCount = new ConcurrentHashMap<>();

        int workerCount = 4;
        ExecutorService executor = Executors.newFixedThreadPool(workerCount);
        for (int w = 0; w < workerCount; w++) {
            executor.submit(() -> runWorker(frontier, graph, processedCount));
        }
        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));

        assertEquals(5, processedCount.size(), "all 5 reachable nodes should have been processed");
        processedCount.values().forEach(count -> assertEquals(1, count, "no node should be processed twice"));
        assertTrue(frontier.isDone());
    }

    private void runWorker(CrawlFrontier frontier, Map<Integer, List<Integer>> graph, ConcurrentMap<URI, Integer> processedCount) {
        try {
            CrawlFrontier.Entry entry;
            while ((entry = frontier.poll(Duration.ofMillis(100))) != null || !frontier.isDone()) {
                if (entry == null) {
                    continue;
                }
                try {
                    processedCount.merge(entry.url(), 1, Integer::sum);
                    for (int neighbor : graph.get(nodeOf(entry.url()))) {
                        frontier.offer(nodeUri(neighbor), entry.depth() + 1);
                    }
                } finally {
                    frontier.markProcessed(entry);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    void neverOffersMoreThanMaxItems() {
        CrawlFrontier frontier = new CrawlFrontier(nodeUri(0), 3, 10); // seed already consumes 1 slot

        assertTrue(frontier.offer(nodeUri(1), 1));
        assertTrue(frontier.offer(nodeUri(2), 1));
        assertFalse(frontier.offer(nodeUri(3), 1), "cap of 3 total items should already be reached");
    }

    @Test
    void rejectsUrlsBeyondMaxDepthWithoutPermanentlyBlockingThem() {
        CrawlFrontier frontier = new CrawlFrontier(nodeUri(0), 100, 1);

        assertFalse(frontier.offer(nodeUri(5), 2), "depth 2 exceeds the max depth of 1");
        assertTrue(frontier.offer(nodeUri(5), 1), "the same URL at an allowed depth should still be accepted");
    }

    @Test
    void rejectsDuplicateUrls() {
        CrawlFrontier frontier = new CrawlFrontier(nodeUri(0), 100, 10);

        assertTrue(frontier.offer(nodeUri(1), 1));
        assertFalse(frontier.offer(nodeUri(1), 1), "already-visited URL must not be re-enqueued");
    }

    private static URI nodeUri(int node) {
        return URI.create("https://example.com/n" + node);
    }

    private static int nodeOf(URI uri) {
        return Integer.parseInt(uri.getPath().substring(2));
    }
}
