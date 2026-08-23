package com.screenshotmaker.engine.web;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

/**
 * Canonicalizes URLs so equivalent addresses (different case, default port, trailing slash,
 * fragment) collapse to the same frontier entry, and decides whether a candidate link stays on
 * the site being crawled. Pure functions, no I/O.
 */
public final class UrlNormalizer {

    private UrlNormalizer() {
    }

    /** Lower-cases scheme/host, drops default ports and fragments, and strips trailing slashes. */
    public static URI normalize(URI raw) {
        URI absolute = raw.normalize();
        String scheme = absolute.getScheme() == null ? null : absolute.getScheme().toLowerCase(Locale.ROOT);
        String host = absolute.getHost() == null ? null : absolute.getHost().toLowerCase(Locale.ROOT);
        int port = absolute.getPort();
        if (("http".equals(scheme) && port == 80) || ("https".equals(scheme) && port == 443)) {
            port = -1;
        }

        String path = absolute.getPath();
        if (path == null || path.isEmpty()) {
            path = "/";
        } else if (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        try {
            return new URI(scheme, absolute.getUserInfo(), host, port, path, absolute.getQuery(), null);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Cannot normalize URL: " + raw, e);
        }
    }

    public static boolean isCrawlableScheme(URI uri) {
        String scheme = uri.getScheme();
        return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
    }

    /** Whether {@code candidate} belongs to the same site as {@code seed}. */
    public static boolean isSameSite(URI seed, URI candidate, boolean includeSubdomains) {
        String seedHost = seed.getHost();
        String candidateHost = candidate.getHost();
        if (seedHost == null || candidateHost == null) {
            return false;
        }
        seedHost = seedHost.toLowerCase(Locale.ROOT);
        candidateHost = candidateHost.toLowerCase(Locale.ROOT);
        if (seedHost.equals(candidateHost)) {
            return true;
        }
        return includeSubdomains && candidateHost.endsWith("." + seedHost);
    }
}
