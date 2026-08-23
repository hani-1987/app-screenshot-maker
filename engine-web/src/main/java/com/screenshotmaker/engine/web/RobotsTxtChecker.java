package com.screenshotmaker.engine.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Minimal, best-effort {@code robots.txt} support: fetches the file once per host and honours
 * {@code Disallow} rules under {@code User-agent: *}. This is not a full RFC 9309 implementation
 * (no wildcard/{@code $} matching, no {@code Crawl-delay}, no sitemap parsing) &mdash; it exists so
 * the crawler defaults to being a polite citizen, not to be a general-purpose robots.txt parser.
 * Any fetch failure (404, timeout, malformed host) is treated as "everything allowed", per convention.
 */
public final class RobotsTxtChecker {

    private static final Logger log = LoggerFactory.getLogger(RobotsTxtChecker.class);

    private final boolean enabled;
    private final HttpClient httpClient;
    private final Map<String, List<String>> disallowRulesByHost = new ConcurrentHashMap<>();

    public RobotsTxtChecker(boolean enabled) {
        this.enabled = enabled;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public boolean isAllowed(URI url) {
        if (!enabled) {
            return true;
        }
        List<String> disallowPaths = disallowRulesByHost.computeIfAbsent(hostKey(url), key -> fetchDisallowRules(url));
        String path = url.getPath() == null || url.getPath().isEmpty() ? "/" : url.getPath();
        for (String disallowed : disallowPaths) {
            if (!disallowed.isEmpty() && path.startsWith(disallowed)) {
                return false;
            }
        }
        return true;
    }

    private static String hostKey(URI url) {
        return url.getScheme() + "://" + url.getHost() + (url.getPort() > 0 ? ":" + url.getPort() : "");
    }

    private List<String> fetchDisallowRules(URI pageUrl) {
        URI robotsUri = URI.create(hostKey(pageUrl) + "/robots.txt");
        try {
            HttpRequest request = HttpRequest.newBuilder(robotsUri)
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return List.of();
            }
            return parseWildcardDisallowRules(response.body());
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.debug("Could not fetch {} ({}); treating site as fully crawlable", robotsUri, e.toString());
            return List.of();
        }
    }

    static List<String> parseWildcardDisallowRules(String robotsTxt) {
        List<String> rules = new ArrayList<>();
        boolean inWildcardGroup = false;
        for (String rawLine : robotsTxt.split("\\R")) {
            String line = stripComment(rawLine).trim();
            if (line.isEmpty()) {
                continue;
            }
            String[] parts = line.split(":", 2);
            if (parts.length != 2) {
                continue;
            }
            String directive = parts[0].trim().toLowerCase(Locale.ROOT);
            String value = parts[1].trim();
            switch (directive) {
                case "user-agent" -> inWildcardGroup = value.equals("*");
                case "disallow" -> {
                    if (inWildcardGroup) {
                        rules.add(value);
                    }
                }
                default -> { /* ignore Allow/Crawl-delay/Sitemap/etc. */ }
            }
        }
        return rules;
    }

    private static String stripComment(String line) {
        int hash = line.indexOf('#');
        return hash >= 0 ? line.substring(0, hash) : line;
    }
}
