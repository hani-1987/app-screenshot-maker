package com.screenshotmaker.core.spi;

import com.screenshotmaker.core.exception.CaptureException;
import com.screenshotmaker.core.model.CaptureConfig;
import com.screenshotmaker.core.model.CaptureResult;
import com.screenshotmaker.core.model.CaptureTarget;

/**
 * Something that can walk all reachable parts of a target and hand each screenshot to a
 * {@link ScreenshotSink}. There is exactly one implementation per {@link com.screenshotmaker.core.model.CaptureTargetType}:
 * the website crawler and the Windows desktop UI walker. Neither implementation depends on the
 * other, and neither depends on how/where results are stored.
 */
public interface CaptureEngine {

    /** Which target type this engine knows how to handle. */
    com.screenshotmaker.core.model.CaptureTargetType supportedType();

    /**
     * Walks the target and stores every screenshot it finds through {@code sink}, up to the
     * limits described by {@code config}.
     *
     * @throws CaptureException if the run could not proceed at all
     */
    CaptureResult capture(CaptureTarget target, CaptureConfig config, ScreenshotSink sink) throws CaptureException;
}
