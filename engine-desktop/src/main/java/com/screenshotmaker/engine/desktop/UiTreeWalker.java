package com.screenshotmaker.engine.desktop;

import com.screenshotmaker.core.model.CaptureError;
import com.screenshotmaker.core.model.CapturedScreen;
import com.screenshotmaker.core.model.CaptureResult;
import com.screenshotmaker.core.spi.ScreenshotSink;
import io.appium.java_client.windows.WindowsDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Depth-first sweep of a Windows application's UI Automation tree: screenshots the current
 * window, then activates every not-yet-seen, not-blocklisted interactive control it can find.
 *
 * <p>Only genuinely new top-level windows (dialogs, secondary windows) are recursed into and then
 * closed to restore the prior state &mdash; controls that mutate the current window in place (tab
 * switches, expanders, toggles) are screenshotted but not recursed further from, since there is no
 * general way to "undo" an in-place UI change on an arbitrary application. This keeps traversal
 * bounded and predictable rather than attempting exhaustive state-machine coverage.
 */
final class UiTreeWalker {

    private static final Logger log = LoggerFactory.getLogger(UiTreeWalker.class);

    private static final Set<String> INTERACTIVE_CONTROL_TYPES = Set.of(
            "Button", "SplitButton", "MenuItem", "TabItem", "ListItem",
            "RadioButton", "CheckBox", "Hyperlink", "ComboBox");

    private final WindowsDriver driver;
    private final DesktopCaptureConfig config;
    private final ScreenshotSink sink;
    private final CaptureResult.Builder resultBuilder;
    private final SafetyFilter safetyFilter;
    private final VisitedStateTracker visited = new VisitedStateTracker();

    UiTreeWalker(WindowsDriver driver, DesktopCaptureConfig config, ScreenshotSink sink, CaptureResult.Builder resultBuilder) {
        this.driver = driver;
        this.config = config;
        this.sink = sink;
        this.resultBuilder = resultBuilder;
        this.safetyFilter = new SafetyFilter(config.blockedActionPatterns());
    }

    void walk(List<String> rootBreadcrumb) {
        captureScreen(rootBreadcrumb);
        exploreChildren(rootBreadcrumb, 0);
    }

    private void exploreChildren(List<String> breadcrumb, int depth) {
        if (depth >= config.maxDepth() || resultBuilder.screenCount() >= config.maxItems()) {
            return;
        }

        int explored = 0;
        for (WebElement element : findInteractiveElements()) {
            if (explored >= config.maxElementsPerScreen() || resultBuilder.screenCount() >= config.maxItems()) {
                return;
            }
            if (tryActivate(element, breadcrumb, depth)) {
                explored++;
            }
        }
    }

    private boolean tryActivate(WebElement element, List<String> breadcrumb, int depth) {
        String signature;
        String name;
        try {
            signature = ElementSignature.of(breadcrumb, element);
            name = element.getAttribute("Name");
        } catch (Exception e) {
            return false;
        }

        if (!visited.markIfNew(signature)) {
            return false;
        }
        if (!safetyFilter.isSafeToActivate(name)) {
            log.info("Skipping '{}' at {} (matches the safety blocklist)", name, String.join(" > ", breadcrumb));
            return false;
        }

        List<String> childBreadcrumb = append(breadcrumb, displayName(name, element));
        try {
            activateAndCapture(element, breadcrumb, childBreadcrumb, depth);
            return true;
        } catch (StaleElementReferenceException | NoSuchElementException | ElementNotInteractableException e) {
            resultBuilder.addError(CaptureError.now(String.join(" > ", childBreadcrumb),
                    e.getClass().getSimpleName() + ": " + firstLine(e.getMessage())));
            return false;
        } catch (Exception e) {
            resultBuilder.addError(CaptureError.now(String.join(" > ", childBreadcrumb),
                    "Unexpected failure: " + firstLine(e.getMessage())));
            return false;
        }
    }

    private void activateAndCapture(WebElement element, List<String> breadcrumb, List<String> childBreadcrumb, int depth) {
        Set<String> handlesBefore = driver.getWindowHandles();
        String originalHandle = driver.getWindowHandle();

        element.click();
        sleep(config.settleDelay());

        Set<String> newHandles = new LinkedHashSet<>(driver.getWindowHandles());
        newHandles.removeAll(handlesBefore);

        if (newHandles.isEmpty()) {
            // In-place mutation of the current window: capture it, but do not recurse further from
            // a state we cannot reliably undo.
            captureScreen(childBreadcrumb);
            return;
        }

        for (String handle : newHandles) {
            driver.switchTo().window(handle);
            captureScreen(childBreadcrumb);
            exploreChildren(childBreadcrumb, depth + 1);
            closeSecondaryWindow(handle, originalHandle);
        }
        driver.switchTo().window(originalHandle);
    }

    private void closeSecondaryWindow(String handle, String originalHandle) {
        if (handle.equals(originalHandle)) {
            return;
        }
        try {
            driver.switchTo().window(handle);
            driver.close();
        } catch (Exception e) {
            log.debug("Could not close secondary window {}: {}", handle, e.toString());
        }
    }

    private void captureScreen(List<String> breadcrumb) {
        try {
            byte[] png = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            int sequence = resultBuilder.nextSequence();
            String label = breadcrumb.isEmpty() ? "root" : breadcrumb.get(breadcrumb.size() - 1);
            String sourceRef = String.join(" > ", breadcrumb);
            CapturedScreen screen = new CapturedScreen(sequence, label, sourceRef, breadcrumb, Instant.now());
            sink.store(screen, png);
            resultBuilder.addScreen(screen);
        } catch (Exception e) {
            resultBuilder.addError(CaptureError.now(String.join(" > ", breadcrumb), "Screenshot failed: " + firstLine(e.getMessage())));
        }
    }

    private List<WebElement> findInteractiveElements() {
        try {
            List<WebElement> interactive = new ArrayList<>();
            for (WebElement element : driver.findElements(By.xpath("//*"))) {
                try {
                    if (INTERACTIVE_CONTROL_TYPES.contains(element.getTagName())) {
                        interactive.add(element);
                    }
                } catch (Exception ignored) {
                    // Element went stale between enumeration and inspection; skip it.
                }
            }
            return interactive;
        } catch (Exception e) {
            log.debug("Could not enumerate UI elements: {}", e.toString());
            return List.of();
        }
    }

    private static String displayName(String name, WebElement element) {
        if (name != null && !name.isBlank()) {
            return name;
        }
        try {
            return element.getTagName();
        } catch (Exception e) {
            return "element";
        }
    }

    private static List<String> append(List<String> base, String next) {
        List<String> copy = new ArrayList<>(base);
        copy.add(next);
        return copy;
    }

    private static String firstLine(String message) {
        if (message == null || message.isBlank()) {
            return "no details";
        }
        return message.lines().findFirst().orElse(message);
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
