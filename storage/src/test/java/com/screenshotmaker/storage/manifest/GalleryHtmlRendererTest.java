package com.screenshotmaker.storage.manifest;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GalleryHtmlRendererTest {

    @Test
    void escapesHtmlSpecialCharactersInUserSuppliedContent() {
        ManifestScreenEntry entry = new ManifestScreenEntry(
                1, "<script>alert(1)</script>", "https://example.com?a=1&b=2",
                List.of("Home"), "0001_home.png", Instant.now());
        ManifestDocument document = new ManifestDocument(
                "WEBSITE", "https://example.com", Instant.now(), Instant.now(), 1, 0, List.of(entry), List.of());

        String html = GalleryHtmlRenderer.render(document);

        assertFalse(html.contains("<script>alert(1)</script>"), "raw script tag must not appear unescaped");
        assertTrue(html.contains("&lt;script&gt;"));
        assertTrue(html.contains("a=1&amp;b=2"));
    }

    @Test
    void includesEveryScreenAsAGridCard() {
        ManifestScreenEntry first = new ManifestScreenEntry(1, "Home", "https://example.com/", List.of("Home"), "0001_home.png", Instant.now());
        ManifestScreenEntry second = new ManifestScreenEntry(2, "About", "https://example.com/about", List.of("Home", "About"), "0002_about.png", Instant.now());
        ManifestDocument document = new ManifestDocument(
                "WEBSITE", "https://example.com", Instant.now(), Instant.now(), 2, 0, List.of(first, second), List.of());

        String html = GalleryHtmlRenderer.render(document);

        assertTrue(html.contains("0001_home.png"));
        assertTrue(html.contains("0002_about.png"));
    }
}
