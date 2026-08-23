package com.screenshotmaker.engine.desktop;

import java.util.HashSet;
import java.util.Set;

/**
 * Remembers which element signatures have already been activated so the walker never clicks the
 * same control twice. Desktop UI automation is inherently single-threaded (one input stream to one
 * application), so this needs no synchronization.
 */
final class VisitedStateTracker {

    private final Set<String> visited = new HashSet<>();

    /** Returns true (and records it) if this signature has not been seen before. */
    boolean markIfNew(String signature) {
        return visited.add(signature);
    }

    int size() {
        return visited.size();
    }
}
