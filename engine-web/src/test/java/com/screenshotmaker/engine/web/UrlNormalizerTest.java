package com.screenshotmaker.engine.web;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UrlNormalizerTest {

    @Test
    void dropsDefaultPortsAndFragmentsAndLowercasesHost() {
        URI normalized = UrlNormalizer.normalize(URI.create("HTTPS://Example.COM:443/Path#section"));
        assertEquals(URI.create("https://example.com/Path"), normalized);
    }

    @Test
    void keepsNonDefaultPorts() {
        URI normalized = UrlNormalizer.normalize(URI.create("http://example.com:8080/path"));
        assertEquals(8080, normalized.getPort());
    }

    @Test
    void stripsTrailingSlashExceptForRoot() {
        assertEquals("/a/b", UrlNormalizer.normalize(URI.create("https://example.com/a/b/")).getPath());
        assertEquals("/", UrlNormalizer.normalize(URI.create("https://example.com")).getPath());
        assertEquals("/", UrlNormalizer.normalize(URI.create("https://example.com/")).getPath());
    }

    @Test
    void equivalentUrlsNormalizeToTheSameValue() {
        URI a = UrlNormalizer.normalize(URI.create("https://example.com/page/"));
        URI b = UrlNormalizer.normalize(URI.create("https://EXAMPLE.com:443/page#top"));
        assertEquals(a, b);
    }

    @Test
    void isSameSiteRespectsSubdomainFlag() {
        URI seed = URI.create("https://example.com/");
        URI subdomain = URI.create("https://blog.example.com/post");
        URI other = URI.create("https://not-example.com/");

        assertTrue(UrlNormalizer.isSameSite(seed, subdomain, true));
        assertFalse(UrlNormalizer.isSameSite(seed, subdomain, false));
        assertFalse(UrlNormalizer.isSameSite(seed, other, true));
    }

    @Test
    void onlyHttpAndHttpsAreCrawlable() {
        assertTrue(UrlNormalizer.isCrawlableScheme(URI.create("https://example.com")));
        assertTrue(UrlNormalizer.isCrawlableScheme(URI.create("http://example.com")));
        assertFalse(UrlNormalizer.isCrawlableScheme(URI.create("mailto:a@example.com")));
        assertFalse(UrlNormalizer.isCrawlableScheme(URI.create("ftp://example.com")));
    }
}
