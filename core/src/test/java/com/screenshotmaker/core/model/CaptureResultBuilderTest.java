package com.screenshotmaker.core.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CaptureResultBuilderTest {

    @Test
    void nextSequenceIsUniqueUnderConcurrentAccess() throws InterruptedException {
        CaptureResult.Builder builder = new CaptureResult.Builder(new CaptureTarget(CaptureTargetType.WEBSITE, "https://example.com"));
        int workers = 16;
        int perWorker = 200;
        ExecutorService executor = Executors.newFixedThreadPool(workers);
        AtomicInteger[] slots = new AtomicInteger[workers * perWorker + 1];
        for (int i = 0; i < slots.length; i++) {
            slots[i] = new AtomicInteger(0);
        }

        for (int w = 0; w < workers; w++) {
            executor.submit(() -> {
                for (int i = 0; i < perWorker; i++) {
                    int sequence = builder.nextSequence();
                    slots[sequence].incrementAndGet();
                }
            });
        }
        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));

        assertEquals(workers * perWorker, builder.nextSequence() - 1);
        IntStream.range(1, slots.length - 1).forEach(i -> assertEquals(1, slots[i].get(), "sequence " + i + " should be handed out exactly once"));
    }

    @Test
    void buildCapturesEverythingAdded() {
        CaptureTarget target = new CaptureTarget(CaptureTargetType.WEBSITE, "https://example.com");
        CaptureResult.Builder builder = new CaptureResult.Builder(target);

        builder.addScreen(new CapturedScreen(1, "Home", "https://example.com/", List.of("Home"), java.time.Instant.now()));
        builder.addError(CaptureError.now("https://example.com/broken", "timed out"));

        CaptureResult result = builder.build();

        assertEquals(target, result.target());
        assertEquals(1, result.screens().size());
        assertTrue(result.hasErrors());
        assertEquals(1, result.errors().size());
    }
}
