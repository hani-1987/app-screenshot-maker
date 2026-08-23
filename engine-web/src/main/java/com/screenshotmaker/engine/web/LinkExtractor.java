package com.screenshotmaker.engine.web;

import java.net.URI;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Decides whether a link found on a page is worth adding to the crawl frontier: resolves it
 * against the page's URL, drops non-http(s) schemes and links to obvious non-page resources
 * (images, stylesheets, archives, ...), and normalizes what remains.
 */
public final class LinkExtractor {

    private static final Set<String> NON_PAGE_EXTENSIONS = Set.of(
            "pdf", "zip", "rar", "7z", "gz", "tar", "exe", "dmg", "msi", "pkg", "apk",
            "png", "jpg", "jpeg", "gif", "svg", "webp", "ico", "bmp",
            "css", "js", "json", "xml", "rss", "atom",
            "mp4", "mp3", "wav", "avi", "mov", "webm",
            "woff", "woff2", "ttf", "eot", "otf",
            "csv", "xlsx", "xls", "docx", "doc", "pptx", "ppt");

    private LinkExtractor() {
    }

    public static Optional<URI> resolveCrawlable(String href, URI baseUri) {
        if (href == null || href.isBlank()) {
            return Optional.empty();
        }
        String trimmed = href.trim();
        String lowered = trimmed.toLowerCase(Locale.ROOT);
        if (lowered.startsWith("mailto:") || lowered.startsWith("tel:") || lowered.startsWith("javascript:")
                || lowered.startsWith("#")) {
            return Optional.empty();
        }

        try {
            URI resolved = baseUri.resolve(trimmed);
            if (!UrlNormalizer.isCrawlableScheme(resolved)) {
                return Optional.empty();
            }
            URI normalized = UrlNormalizer.normalize(resolved);
            if (hasNonPageExtension(normalized.getPath())) {
                return Optional.empty();
            }
            return Optional.of(normalized);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static boolean hasNonPageExtension(String path) {
        if (path == null) {
            return false;
        }
        int dot = path.lastIndexOf('.');
        if (dot < 0 || dot == path.length() - 1) {
            return false;
        }
        String extension = path.substring(dot + 1).toLowerCase(Locale.ROOT);
        return NON_PAGE_EXTENSIONS.contains(extension);
    }
}
