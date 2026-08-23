package com.screenshotmaker.core.model;

import java.time.Duration;

/**
 * The subset of run settings every {@link com.screenshotmaker.core.spi.CaptureEngine} cares about.
 *
 * <p>Engine-specific settings (e.g. whether the web crawler follows subdomains, or which desktop
 * actions the safety filter blocks) intentionally do NOT live here — each engine module defines its
 * own richer config type that implements this interface, so engines stay decoupled from one another's
 * options. The CLI, as the composition root, is the only place that needs to know both concrete types.
 */
public interface CaptureConfig {

    /** Upper bound on the number of screens/pages a run will capture, regardless of how much more exists. */
    int maxItems();

    /** Upper bound on traversal depth (link-follow depth for web, window/dialog nesting for desktop). */
    int maxDepth();

    /** Number of parallel workers the engine may use. */
    int concurrency();

    /** Per-item timeout (page load, element wait, window wait, ...). */
    Duration timeout();
}
