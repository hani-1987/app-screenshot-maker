package com.screenshotmaker.cli;

import com.screenshotmaker.core.model.CaptureTargetType;
import com.screenshotmaker.core.spi.CaptureEngine;
import com.screenshotmaker.engine.desktop.DesktopCaptureEngine;
import com.screenshotmaker.engine.web.WebCaptureEngine;

import java.util.Map;

/**
 * Maps a {@link CaptureTargetType} to the engine that handles it. This is the one place in the
 * whole codebase that is allowed to know both engines exist; neither engine module knows about the
 * other, and adding a third engine only means adding one entry here.
 */
final class EngineRegistry {

    private final Map<CaptureTargetType, CaptureEngine> enginesByType = Map.of(
            CaptureTargetType.WEBSITE, new WebCaptureEngine(),
            CaptureTargetType.DESKTOP_APPLICATION, new DesktopCaptureEngine());

    CaptureEngine engineFor(CaptureTargetType type) {
        CaptureEngine engine = enginesByType.get(type);
        if (engine == null) {
            throw new IllegalStateException("No capture engine registered for " + type);
        }
        return engine;
    }
}
