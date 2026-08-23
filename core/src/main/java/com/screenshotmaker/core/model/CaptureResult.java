package com.screenshotmaker.core.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The outcome of one capture run: everything that was captured, plus anything that went wrong
 * along the way. Immutable once built.
 */
public record CaptureResult(
        CaptureTarget target,
        Instant startedAt,
        Instant finishedAt,
        List<CapturedScreen> screens,
        List<CaptureError> errors) {

    public CaptureResult {
        screens = List.copyOf(screens);
        errors = List.copyOf(errors);
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    /**
     * Thread-safe accumulator used by engines while a run is in progress (web crawling in
     * particular happens on several worker threads at once).
     */
    public static final class Builder {
        private final CaptureTarget target;
        private final Instant startedAt = Instant.now();
        private final List<CapturedScreen> screens = Collections.synchronizedList(new ArrayList<>());
        private final List<CaptureError> errors = Collections.synchronizedList(new ArrayList<>());
        private final AtomicInteger sequenceCounter = new AtomicInteger(0);

        public Builder(CaptureTarget target) {
            this.target = target;
        }

        /** Atomically hands out the next 1-based sequence number; safe to call from concurrent workers. */
        public int nextSequence() {
            return sequenceCounter.incrementAndGet();
        }

        public Builder addScreen(CapturedScreen screen) {
            screens.add(screen);
            return this;
        }

        public Builder addError(CaptureError error) {
            errors.add(error);
            return this;
        }

        public int screenCount() {
            return screens.size();
        }

        public CaptureResult build() {
            return new CaptureResult(target, startedAt, Instant.now(), screens, errors);
        }
    }
}
