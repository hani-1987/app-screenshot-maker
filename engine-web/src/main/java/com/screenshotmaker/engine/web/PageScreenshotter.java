package com.screenshotmaker.engine.web;

import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

/**
 * Captures a full-page (not just viewport) screenshot by resizing the browser window to the
 * page's rendered content size before taking the screenshot. Simpler and more portable across
 * browsers than driving the Chrome DevTools Protocol directly.
 */
public final class PageScreenshotter {

    private static final int MIN_WIDTH = 1024;
    private static final int MIN_HEIGHT = 600;

    private final int maxFullPageHeight;

    public PageScreenshotter(int maxFullPageHeight) {
        this.maxFullPageHeight = maxFullPageHeight;
    }

    public byte[] capture(WebDriver driver) {
        resizeToFullPage(driver);
        return ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
    }

    private void resizeToFullPage(WebDriver driver) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        long contentWidth = readDimension(js, "scrollWidth");
        long contentHeight = readDimension(js, "scrollHeight");

        int width = (int) Math.max(contentWidth, MIN_WIDTH);
        int height = (int) Math.min(Math.max(contentHeight, MIN_HEIGHT), maxFullPageHeight);
        driver.manage().window().setSize(new Dimension(width, height));
    }

    private long readDimension(JavascriptExecutor js, String property) {
        Object result = js.executeScript(
                "var d = document; return Math.max(" +
                        "d.documentElement." + property + " || 0, " +
                        "d.body ? d.body." + property + " : 0);");
        return result instanceof Number number ? number.longValue() : MIN_HEIGHT;
    }
}
