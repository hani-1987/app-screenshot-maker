package com.screenshotmaker.engine.desktop;

import org.openqa.selenium.WebElement;

import java.util.List;

/**
 * Builds a stable-enough identifier for a UI element so the walker can tell "the Settings button
 * on the Home screen" apart from "the Settings button on the Profile screen", even though both
 * might report the same control type and name.
 */
final class ElementSignature {

    private ElementSignature() {
    }

    static String of(List<String> breadcrumb, WebElement element) {
        String controlType = element.getTagName();
        String name = nullToEmpty(element.getAttribute("Name"));
        String automationId = nullToEmpty(element.getAttribute("AutomationId"));
        return String.join(">", breadcrumb) + "::" + controlType + "|" + name + "|" + automationId;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
