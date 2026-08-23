package com.screenshotmaker.engine.web;

import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

/**
 * Builds a {@link WebDriver} for the browser requested in {@link WebCaptureConfig}. Selenium 4's
 * built-in Selenium Manager resolves and downloads the matching browser driver automatically, so
 * no driver binaries need to be bundled or configured on either Windows or macOS.
 */
public final class BrowserFactory {

    private BrowserFactory() {
    }

    public static WebDriver create(WebCaptureConfig config) {
        WebDriver driver = switch (config.browser()) {
            case CHROME -> new ChromeDriver(chromeOptions(config));
            case FIREFOX -> new FirefoxDriver(firefoxOptions(config));
            case EDGE -> new EdgeDriver(edgeOptions(config));
        };
        driver.manage().window().setSize(new Dimension(config.windowWidth(), config.windowHeight()));
        return driver;
    }

    private static ChromeOptions chromeOptions(WebCaptureConfig config) {
        ChromeOptions options = new ChromeOptions();
        if (config.headless()) {
            options.addArguments("--headless=new");
        }
        options.addArguments(
                "--disable-gpu",
                "--hide-scrollbars",
                "--force-device-scale-factor=1",
                "--window-size=" + config.windowWidth() + "," + config.windowHeight());
        return options;
    }

    private static FirefoxOptions firefoxOptions(WebCaptureConfig config) {
        FirefoxOptions options = new FirefoxOptions();
        if (config.headless()) {
            options.addArguments("-headless");
        }
        return options;
    }

    private static EdgeOptions edgeOptions(WebCaptureConfig config) {
        EdgeOptions options = new EdgeOptions();
        if (config.headless()) {
            options.addArguments("--headless=new");
        }
        options.addArguments("--disable-gpu", "--hide-scrollbars");
        return options;
    }
}
