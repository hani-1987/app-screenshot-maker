package com.screenshotmaker.cli;

import com.screenshotmaker.core.exception.CaptureException;
import com.screenshotmaker.core.model.CaptureConfig;
import com.screenshotmaker.core.model.CaptureResult;
import com.screenshotmaker.core.model.CaptureTarget;
import com.screenshotmaker.core.model.CaptureTargetType;
import com.screenshotmaker.engine.desktop.DesktopCaptureConfig;
import com.screenshotmaker.engine.desktop.SafetyFilter;
import com.screenshotmaker.engine.web.WebCaptureConfig;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Command-line surface: parses options into a {@link CaptureTarget} and mode-specific
 * {@link CaptureConfig}, then hands both to {@link CaptureRunner}. Holds no capture logic itself.
 */
@Command(
        name = "screenshot-maker",
        mixinStandardHelpOptions = true,
        version = "screenshot-maker 1.0.0",
        description = "Walks every reachable page of a website, or every reachable screen of a running "
                + "Windows application, and saves a screenshot of each one.")
public final class ScreenshotMakerCommand implements Callable<Integer> {

    enum Mode { web, desktop }

    enum BrowserOption { chrome, firefox, edge }

    @Option(names = {"-m", "--mode"}, required = true, description = "Capture mode: ${COMPLETION-CANDIDATES}")
    Mode mode;

    @Option(names = {"-t", "--target"}, required = true,
            description = "Web: the seed URL to crawl. Desktop: a path to an .exe to launch, "
                    + "or the exact title of an already-running window to attach to.")
    String target;

    @Option(names = {"-o", "--output"}, defaultValue = "./screenshots",
            description = "Root folder screenshots are written under (default: ${DEFAULT-VALUE}).")
    Path output;

    @Option(names = "--max-items", defaultValue = "-1", description = "Max screens to capture (default: mode-specific).")
    int maxItems;

    @Option(names = "--max-depth", defaultValue = "-1", description = "Max traversal depth (default: mode-specific).")
    int maxDepth;

    @Option(names = "--concurrency", defaultValue = "-1", description = "Parallel browser workers, web mode only (default: 4).")
    int concurrency;

    @Option(names = "--timeout-seconds", defaultValue = "-1", description = "Per-item timeout in seconds (default: mode-specific).")
    int timeoutSeconds;

    @Option(names = "--include-subdomains", description = "Web: also crawl subdomains of the seed host.")
    boolean includeSubdomains;

    @Option(names = "--ignore-robots-txt", description = "Web: ignore robots.txt Disallow rules.")
    boolean ignoreRobotsTxt;

    @Option(names = "--headed", description = "Web: show the browser window instead of running headless.")
    boolean headed;

    @Option(names = "--browser", defaultValue = "chrome", description = "Web: browser engine: ${COMPLETION-CANDIDATES}")
    BrowserOption browser;

    @Option(names = "--winappdriver-url", defaultValue = "http://127.0.0.1:4723", description = "Desktop: WinAppDriver endpoint.")
    URI winAppDriverUrl;

    @Option(names = "--settle-delay-ms", defaultValue = "-1",
            description = "Desktop: delay after each click before screenshotting, in ms (default: 400).")
    int settleDelayMillis;

    @Option(names = "--blocked", split = ",",
            description = "Desktop: extra comma-separated blocklist keywords, appended to the built-in safety list.")
    List<String> extraBlockedPatterns = List.of();

    @Option(names = {"-v", "--verbose"}, description = "Enable debug logging.")
    boolean verbose;

    private final CaptureRunner captureRunner = new CaptureRunner();

    @Override
    public Integer call() {
        try {
            CaptureTargetType targetType = mode == Mode.web ? CaptureTargetType.WEBSITE : CaptureTargetType.DESKTOP_APPLICATION;
            CaptureTarget captureTarget = new CaptureTarget(targetType, target);
            CaptureConfig config = buildConfig(targetType);

            RunSummary summary = captureRunner.run(captureTarget, config, output);
            printSummary(summary);
            return summary.result().hasErrors() && summary.result().screens().isEmpty() ? 1 : 0;
        } catch (CaptureException | IOException e) {
            System.err.println("Capture failed: " + e.getMessage());
            return 2;
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid configuration: " + e.getMessage());
            return 2;
        }
    }

    private CaptureConfig buildConfig(CaptureTargetType type) {
        return type == CaptureTargetType.WEBSITE ? buildWebConfig() : buildDesktopConfig();
    }

    private WebCaptureConfig buildWebConfig() {
        WebCaptureConfig defaults = WebCaptureConfig.defaults();
        return new WebCaptureConfig(
                positiveOrDefault(maxItems, defaults.maxItems()),
                nonNegativeOrDefault(maxDepth, defaults.maxDepth()),
                positiveOrDefault(concurrency, defaults.concurrency()),
                timeoutSeconds > 0 ? Duration.ofSeconds(timeoutSeconds) : defaults.timeout(),
                includeSubdomains,
                !ignoreRobotsTxt,
                !headed,
                defaults.politenessDelay(),
                defaults.windowWidth(),
                defaults.windowHeight(),
                defaults.maxFullPageHeight(),
                toEngineBrowser(browser));
    }

    private DesktopCaptureConfig buildDesktopConfig() {
        DesktopCaptureConfig defaults = DesktopCaptureConfig.defaults();
        if (concurrency > 1) {
            System.err.println("Note: desktop capture always runs with concurrency=1 (one input stream to the "
                    + "target application); ignoring --concurrency=" + concurrency);
        }
        List<String> blockedPatterns = new ArrayList<>(SafetyFilter.defaultBlockedPatterns());
        blockedPatterns.addAll(extraBlockedPatterns);

        return new DesktopCaptureConfig(
                positiveOrDefault(maxItems, defaults.maxItems()),
                nonNegativeOrDefault(maxDepth, defaults.maxDepth()),
                1,
                timeoutSeconds > 0 ? Duration.ofSeconds(timeoutSeconds) : defaults.timeout(),
                winAppDriverUrl,
                settleDelayMillis > 0 ? Duration.ofMillis(settleDelayMillis) : defaults.settleDelay(),
                defaults.maxElementsPerScreen(),
                blockedPatterns);
    }

    private static WebCaptureConfig.Browser toEngineBrowser(BrowserOption option) {
        return switch (option) {
            case chrome -> WebCaptureConfig.Browser.CHROME;
            case firefox -> WebCaptureConfig.Browser.FIREFOX;
            case edge -> WebCaptureConfig.Browser.EDGE;
        };
    }

    private void printSummary(RunSummary summary) {
        CaptureResult result = summary.result();
        System.out.println();
        System.out.println("Captured " + result.screens().size() + " screen(s), " + result.errors().size() + " error(s).");
        System.out.println("Output: " + summary.outputDirectory().toAbsolutePath());
        if (result.hasErrors()) {
            System.out.println("First few errors:");
            result.errors().stream().limit(5)
                    .forEach(error -> System.out.println("  - " + error.sourceRef() + ": " + error.message()));
        }
    }

    private static int positiveOrDefault(int value, int fallback) {
        return value > 0 ? value : fallback;
    }

    private static int nonNegativeOrDefault(int value, int fallback) {
        return value >= 0 ? value : fallback;
    }
}
