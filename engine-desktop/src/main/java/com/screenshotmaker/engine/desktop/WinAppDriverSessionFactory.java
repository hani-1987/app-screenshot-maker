package com.screenshotmaker.engine.desktop;

import com.screenshotmaker.core.exception.CaptureException;
import io.appium.java_client.windows.WindowsDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Opens a {@link WindowsDriver} session against a locally running WinAppDriver instance, either by
 * launching an executable or by attaching to an already-running window by title.
 *
 * <p>Attaching follows the standard WinAppDriver recipe: open a session against the special
 * {@code Root} pseudo-app (the desktop itself), locate the target window by its accessible name,
 * read its native window handle, then open a second session scoped to that handle via the
 * {@code appTopLevelWindow} capability.
 */
final class WinAppDriverSessionFactory {

    private WinAppDriverSessionFactory() {
    }

    static WindowsDriver open(DesktopCaptureConfig config, String source) throws CaptureException {
        URL endpoint = toUrl(config.winAppDriverUrl());
        Path asPath = tryResolvePath(source);

        if (isExecutablePath(asPath, source)) {
            return launchByPath(endpoint, asPath, config);
        }
        return attachByWindowTitle(endpoint, source, config);
    }

    private static WindowsDriver launchByPath(URL endpoint, Path exePath, DesktopCaptureConfig config) throws CaptureException {
        DesiredCapabilities capabilities = baseCapabilities();
        capabilities.setCapability("app", exePath.toString());
        try {
            WindowsDriver driver = new WindowsDriver(endpoint, capabilities);
            driver.manage().timeouts().implicitlyWait(config.timeout());
            return driver;
        } catch (Exception e) {
            throw new CaptureException("Could not launch '" + exePath + "' via WinAppDriver at " + endpoint
                    + ". Confirm WinAppDriver.exe is running and Developer Mode is enabled.", e);
        }
    }

    private static WindowsDriver attachByWindowTitle(URL endpoint, String windowTitle, DesktopCaptureConfig config)
            throws CaptureException {
        WindowsDriver rootDriver = null;
        try {
            rootDriver = new WindowsDriver(endpoint, rootCapabilities());
            WebElement targetWindow = rootDriver.findElement(By.name(windowTitle));
            String nativeHandle = targetWindow.getAttribute("NativeWindowHandle");
            String hexHandle = Integer.toHexString(Integer.parseInt(nativeHandle));

            DesiredCapabilities attachCapabilities = baseCapabilities();
            attachCapabilities.setCapability("appTopLevelWindow", hexHandle);
            WindowsDriver appDriver = new WindowsDriver(endpoint, attachCapabilities);
            appDriver.manage().timeouts().implicitlyWait(config.timeout());
            return appDriver;
        } catch (Exception e) {
            throw new CaptureException("Could not attach to a running window titled '" + windowTitle
                    + "' via WinAppDriver at " + endpoint + ". Make sure the application is already running "
                    + "and its window title matches exactly.", e);
        } finally {
            if (rootDriver != null) {
                rootDriver.quit();
            }
        }
    }

    private static DesiredCapabilities rootCapabilities() {
        DesiredCapabilities capabilities = baseCapabilities();
        capabilities.setCapability("app", "Root");
        return capabilities;
    }

    private static DesiredCapabilities baseCapabilities() {
        DesiredCapabilities capabilities = new DesiredCapabilities();
        capabilities.setCapability("platformName", "Windows");
        capabilities.setCapability("deviceName", "WindowsPC");
        return capabilities;
    }

    private static boolean isExecutablePath(Path path, String source) {
        return path != null
                && source.toLowerCase(Locale.ROOT).endsWith(".exe")
                && Files.isRegularFile(path);
    }

    private static Path tryResolvePath(String source) {
        try {
            return Path.of(source);
        } catch (Exception e) {
            return null;
        }
    }

    private static URL toUrl(URI uri) throws CaptureException {
        try {
            return uri.toURL();
        } catch (MalformedURLException e) {
            throw new CaptureException("Invalid WinAppDriver URL: " + uri, e);
        }
    }
}
