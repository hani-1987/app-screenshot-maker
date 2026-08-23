package com.screenshotmaker.engine.web;

import com.screenshotmaker.core.model.CaptureError;
import com.screenshotmaker.core.model.CapturedScreen;
import com.screenshotmaker.core.model.CaptureResult;
import com.screenshotmaker.core.spi.ScreenshotSink;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * One BFS worker: owns a single browser instance for its whole lifetime and repeatedly pulls the
 * next page off the shared {@link CrawlFrontier}, screenshots it, and enqueues its same-site links.
 */
final class CrawlWorker implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(CrawlWorker.class);
    private static final Duration POLL_TIMEOUT = Duration.ofMillis(250);

    private final URI seed;
    private final WebCaptureConfig config;
    private final CrawlFrontier frontier;
    private final RobotsTxtChecker robotsTxtChecker;
    private final ScreenshotSink sink;
    private final CaptureResult.Builder resultBuilder;

    CrawlWorker(URI seed, WebCaptureConfig config, CrawlFrontier frontier, RobotsTxtChecker robotsTxtChecker,
                ScreenshotSink sink, CaptureResult.Builder resultBuilder) {
        this.seed = seed;
        this.config = config;
        this.frontier = frontier;
        this.robotsTxtChecker = robotsTxtChecker;
        this.sink = sink;
        this.resultBuilder = resultBuilder;
    }

    @Override
    public void run() {
        WebDriver driver = BrowserFactory.create(config);
        PageScreenshotter screenshotter = new PageScreenshotter(config.maxFullPageHeight());
        try {
            driver.manage().timeouts().pageLoadTimeout(config.timeout());
            drainFrontier(driver, screenshotter);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            driver.quit();
        }
    }

    private void drainFrontier(WebDriver driver, PageScreenshotter screenshotter) throws InterruptedException {
        while (true) {
            CrawlFrontier.Entry entry = frontier.poll(POLL_TIMEOUT);
            if (entry == null) {
                if (frontier.isDone()) {
                    return;
                }
                continue;
            }
            try {
                processEntry(entry, driver, screenshotter);
            } finally {
                frontier.markProcessed(entry);
            }
            if (!config.politenessDelay().isZero()) {
                Thread.sleep(config.politenessDelay().toMillis());
            }
        }
    }

    private void processEntry(CrawlFrontier.Entry entry, WebDriver driver, PageScreenshotter screenshotter) {
        URI url = entry.url();
        if (!robotsTxtChecker.isAllowed(url)) {
            log.debug("Skipping {} (disallowed by robots.txt)", url);
            return;
        }

        try {
            driver.navigate().to(url.toString());
            waitForPageReady(driver);

            byte[] png = screenshotter.capture(driver);
            String title = safeTitle(driver, url);
            int sequence = resultBuilder.nextSequence();
            CapturedScreen screen = new CapturedScreen(sequence, title, url.toString(), List.of(url.getPath()), Instant.now());
            sink.store(screen, png);
            resultBuilder.addScreen(screen);

            if (entry.depth() < config.maxDepth()) {
                enqueueLinks(driver, url, entry.depth());
            }
        } catch (Exception e) {
            log.warn("Failed to capture {}", url, e);
            resultBuilder.addError(CaptureError.now(url.toString(), describe(e)));
        }
    }

    private void waitForPageReady(WebDriver driver) {
        new WebDriverWait(driver, config.timeout()).until(d ->
                "complete".equals(((JavascriptExecutor) d).executeScript("return document.readyState")));
    }

    private void enqueueLinks(WebDriver driver, URI pageUrl, int depth) {
        List<WebElement> anchors;
        try {
            anchors = driver.findElements(By.tagName("a"));
        } catch (Exception e) {
            log.debug("Could not read links on {}: {}", pageUrl, e.toString());
            return;
        }

        for (WebElement anchor : anchors) {
            try {
                String href = anchor.getAttribute("href");
                LinkExtractor.resolveCrawlable(href, pageUrl)
                        .filter(link -> UrlNormalizer.isSameSite(seed, link, config.includeSubdomains()))
                        .ifPresent(link -> frontier.offer(link, depth + 1));
            } catch (Exception e) {
                // A single stale/odd element should not abort link discovery for the rest of the page.
                log.trace("Skipping unreadable link on {}: {}", pageUrl, e.toString());
            }
        }
    }

    private static String safeTitle(WebDriver driver, URI fallback) {
        try {
            String title = driver.getTitle();
            return title == null || title.isBlank() ? fallback.toString() : title;
        } catch (Exception e) {
            return fallback.toString();
        }
    }

    private static String describe(Exception e) {
        String message = e.getMessage();
        return message == null || message.isBlank() ? e.getClass().getSimpleName() : message.lines().findFirst().orElse(message);
    }
}
