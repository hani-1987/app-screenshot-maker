package com.screenshotmaker.engine.web;

import com.screenshotmaker.core.exception.CaptureException;
import com.screenshotmaker.core.model.CaptureConfig;
import com.screenshotmaker.core.model.CaptureResult;
import com.screenshotmaker.core.model.CaptureTarget;
import com.screenshotmaker.core.model.CaptureTargetType;
import com.screenshotmaker.core.spi.CaptureEngine;
import com.screenshotmaker.core.spi.ScreenshotSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Crawls every same-site page reachable from a seed URL and captures a full-page screenshot of
 * each one, using a pool of browser workers coordinated through {@link CrawlFrontier}.
 */
public final class WebCaptureEngine implements CaptureEngine {

    private static final Logger log = LoggerFactory.getLogger(WebCaptureEngine.class);

    @Override
    public CaptureTargetType supportedType() {
        return CaptureTargetType.WEBSITE;
    }

    @Override
    public CaptureResult capture(CaptureTarget target, CaptureConfig config, ScreenshotSink sink) throws CaptureException {
        if (!(config instanceof WebCaptureConfig webConfig)) {
            throw new CaptureException("WebCaptureEngine requires a WebCaptureConfig, got " + config.getClass());
        }

        URI seed = UrlNormalizer.normalize(parseSeed(target.source()));
        log.info("Starting web crawl from {} (maxItems={}, maxDepth={}, concurrency={})",
                seed, webConfig.maxItems(), webConfig.maxDepth(), webConfig.concurrency());

        CaptureResult.Builder resultBuilder = new CaptureResult.Builder(target);
        CrawlFrontier frontier = new CrawlFrontier(seed, webConfig.maxItems(), webConfig.maxDepth());
        RobotsTxtChecker robotsTxtChecker = new RobotsTxtChecker(webConfig.respectRobotsTxt());

        runWorkerPool(seed, webConfig, frontier, robotsTxtChecker, sink, resultBuilder);

        CaptureResult result = resultBuilder.build();
        log.info("Web crawl finished: {} screen(s), {} error(s)", result.screens().size(), result.errors().size());
        return result;
    }

    private void runWorkerPool(URI seed, WebCaptureConfig config, CrawlFrontier frontier,
                                RobotsTxtChecker robotsTxtChecker, ScreenshotSink sink,
                                CaptureResult.Builder resultBuilder) throws CaptureException {
        ExecutorService executor = Executors.newFixedThreadPool(config.concurrency());
        try {
            List<Future<?>> futures = new ArrayList<>(config.concurrency());
            for (int i = 0; i < config.concurrency(); i++) {
                CrawlWorker worker = new CrawlWorker(seed, config, frontier, robotsTxtChecker, sink, resultBuilder);
                futures.add(executor.submit(worker));
            }
            for (Future<?> future : futures) {
                future.get();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CaptureException("Web capture was interrupted", e);
        } catch (ExecutionException e) {
            throw new CaptureException("A crawl worker failed unexpectedly", e.getCause());
        } finally {
            executor.shutdownNow();
        }
    }

    private URI parseSeed(String source) throws CaptureException {
        String trimmed = source.trim();
        try {
            URI uri = new URI(trimmed);
            if (uri.getScheme() == null) {
                uri = new URI("https://" + trimmed);
            }
            if (!UrlNormalizer.isCrawlableScheme(uri) || uri.getHost() == null) {
                throw new CaptureException("Seed must be an http(s) URL, got: " + source);
            }
            return uri;
        } catch (URISyntaxException e) {
            throw new CaptureException("Invalid seed URL: " + source, e);
        }
    }
}
