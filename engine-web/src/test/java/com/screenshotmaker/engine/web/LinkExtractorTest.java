package com.screenshotmaker.engine.web;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LinkExtractorTest {

    private final URI base = URI.create("https://example.com/section/page");

    @Test
    void resolvesRelativeLinksAgainstTheBasePage() {
        Optional<URI> resolved = LinkExtractor.resolveCrawlable("../other", base);
        assertTrue(resolved.isPresent());
        assertEquals("/other", resolved.get().getPath());
    }

    @Test
    void rejectsNonHttpSchemes() {
        assertTrue(LinkExtractor.resolveCrawlable("mailto:test@example.com", base).isEmpty());
        assertTrue(LinkExtractor.resolveCrawlable("tel:+1234567890", base).isEmpty());
        assertTrue(LinkExtractor.resolveCrawlable("javascript:void(0)", base).isEmpty());
    }

    @Test
    void rejectsFragmentOnlyLinks() {
        assertTrue(LinkExtractor.resolveCrawlable("#top", base).isEmpty());
    }

    @Test
    void rejectsBlankOrNullHref() {
        assertTrue(LinkExtractor.resolveCrawlable(null, base).isEmpty());
        assertTrue(LinkExtractor.resolveCrawlable("   ", base).isEmpty());
    }

    @Test
    void rejectsObviousNonPageResources() {
        assertTrue(LinkExtractor.resolveCrawlable("/images/logo.png", base).isEmpty());
        assertTrue(LinkExtractor.resolveCrawlable("/downloads/report.pdf", base).isEmpty());
        assertTrue(LinkExtractor.resolveCrawlable("/styles/site.css", base).isEmpty());
    }

    @Test
    void acceptsOrdinaryPageLinks() {
        Optional<URI> resolved = LinkExtractor.resolveCrawlable("/about-us", base);
        assertTrue(resolved.isPresent());
        assertEquals("https://example.com/about-us", resolved.get().toString());
    }
}
