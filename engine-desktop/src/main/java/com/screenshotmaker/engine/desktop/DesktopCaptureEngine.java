package com.screenshotmaker.engine.desktop;

import com.screenshotmaker.core.exception.CaptureException;
import com.screenshotmaker.core.model.CaptureConfig;
import com.screenshotmaker.core.model.CaptureResult;
import com.screenshotmaker.core.model.CaptureTarget;
import com.screenshotmaker.core.model.CaptureTargetType;
import com.screenshotmaker.core.spi.CaptureEngine;
import com.screenshotmaker.core.spi.ScreenshotSink;
import io.appium.java_client.windows.WindowsDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Locale;

/**
 * Walks a Windows application's UI tree via WinAppDriver and captures a screenshot of every
 * screen and dialog the walker safely discovers. See {@link UiTreeWalker} for the traversal
 * strategy and its limits.
 */
public final class DesktopCaptureEngine implements CaptureEngine {

    private static final Logger log = LoggerFactory.getLogger(DesktopCaptureEngine.class);

    @Override
    public CaptureTargetType supportedType() {
        return CaptureTargetType.DESKTOP_APPLICATION;
    }

    @Override
    public CaptureResult capture(CaptureTarget target, CaptureConfig config, ScreenshotSink sink) throws CaptureException {
        if (!(config instanceof DesktopCaptureConfig desktopConfig)) {
            throw new CaptureException("DesktopCaptureEngine requires a DesktopCaptureConfig, got " + config.getClass());
        }
        requireWindows();

        log.info("Attaching to desktop target '{}' via WinAppDriver at {}", target.source(), desktopConfig.winAppDriverUrl());
        WindowsDriver driver = WinAppDriverSessionFactory.open(desktopConfig, target.source());
        CaptureResult.Builder resultBuilder = new CaptureResult.Builder(target);
        try {
            new UiTreeWalker(driver, desktopConfig, sink, resultBuilder).walk(List.of(windowLabel(target.source())));
        } finally {
            try {
                driver.quit();
            } catch (Exception e) {
                log.debug("Ignoring error while closing WinAppDriver session: {}", e.toString());
            }
        }

        CaptureResult result = resultBuilder.build();
        log.info("Desktop walk finished: {} screen(s), {} error(s)", result.screens().size(), result.errors().size());
        return result;
    }

    private void requireWindows() throws CaptureException {
        String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (!osName.contains("win")) {
            throw new CaptureException("Desktop application capture requires Windows UI Automation via WinAppDriver, "
                    + "which only exists on Windows. Use website capture mode on this OS instead.");
        }
    }

    private static String windowLabel(String source) {
        int lastSeparator = Math.max(source.lastIndexOf('\\'), source.lastIndexOf('/'));
        return lastSeparator >= 0 ? source.substring(lastSeparator + 1) : source;
    }
}
