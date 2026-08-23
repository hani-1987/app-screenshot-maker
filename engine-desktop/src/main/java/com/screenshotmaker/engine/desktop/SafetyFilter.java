package com.screenshotmaker.engine.desktop;

import java.util.List;
import java.util.Locale;

/**
 * Guards the UI walker against activating controls that could do real damage. Since the walker
 * clicks elements it has never seen before, an unfiltered sweep could just as easily hit "Delete
 * account" as "Next tab" &mdash; this is a case-insensitive substring blocklist checked against an
 * element's accessible name before it is ever clicked.
 *
 * <p>This is deliberately conservative and simple (substring matching, not NLP/intent detection):
 * false positives (skipping a harmless control whose name happens to contain "close") are an
 * acceptable cost for never taking a destructive action automatically.
 */
public final class SafetyFilter {

    private final List<String> blockedPatternsLower;

    public SafetyFilter(List<String> blockedPatterns) {
        this.blockedPatternsLower = blockedPatterns.stream()
                .map(pattern -> pattern.toLowerCase(Locale.ROOT))
                .toList();
    }

    /** True if nothing in the blocklist appears in the element's name. Unnamed elements are allowed. */
    public boolean isSafeToActivate(String elementName) {
        if (elementName == null || elementName.isBlank()) {
            return true;
        }
        String lowerName = elementName.toLowerCase(Locale.ROOT);
        return blockedPatternsLower.stream().noneMatch(lowerName::contains);
    }

    public static List<String> defaultBlockedPatterns() {
        return List.of(
                "delete", "remove", "uninstall", "format", "erase", "destroy", "wipe",
                "exit", "quit", "close", "shutdown", "restart", "reset", "sign out", "log out",
                "send", "submit", "pay", "purchase", "buy", "order", "checkout",
                "print", "empty", "clear all", "factory", "overwrite");
    }
}
