package com.screenshotmaker.storage;

import java.util.regex.Pattern;

/**
 * Turns arbitrary labels (page titles, URLs, window/control names) into filesystem-safe file and
 * directory names. Pure function, no I/O, so it is trivial to unit test independently of any sink.
 */
public final class FileNameSanitizer {

    private static final Pattern UNSAFE_CHARS = Pattern.compile("[^a-zA-Z0-9._-]+");
    private static final Pattern LEADING_TRAILING_DOTS_DASHES = Pattern.compile("^[.\\-]+|[.\\-]+$");
    private static final int MAX_LENGTH = 80;
    private static final String FALLBACK_NAME = "untitled";

    private FileNameSanitizer() {
    }

    /**
     * Produces a safe file/directory segment: ASCII letters, digits, dot, dash and underscore only,
     * collapsed and length-capped. Never returns blank or a name containing {@code ..} (no path
     * traversal), regardless of input.
     */
    public static String sanitize(String raw) {
        if (raw == null || raw.isBlank()) {
            return FALLBACK_NAME;
        }

        String replaced = UNSAFE_CHARS.matcher(raw.trim()).replaceAll("-");
        String trimmed = LEADING_TRAILING_DOTS_DASHES.matcher(replaced).replaceAll("");
        String collapsed = trimmed.replaceAll("-{2,}", "-");

        if (collapsed.isBlank()) {
            return FALLBACK_NAME;
        }

        String capped = collapsed.length() > MAX_LENGTH ? collapsed.substring(0, MAX_LENGTH) : collapsed;
        return capped.equals(".") || capped.equals("..") ? FALLBACK_NAME : capped;
    }

    /** Zero-padded sequence prefix so files sort in capture order regardless of OS locale rules. */
    public static String sequencePrefix(int sequence) {
        return String.format("%04d", sequence);
    }
}
