package com.screenshotmaker.core.exception;

/**
 * Fatal capture failure: the run could not proceed at all (target unreachable, wrong engine for
 * the target type, driver unavailable, ...). Non-fatal, per-item problems are recorded as
 * {@link com.screenshotmaker.core.model.CaptureError} entries instead of thrown.
 */
public class CaptureException extends Exception {

    public CaptureException(String message) {
        super(message);
    }

    public CaptureException(String message, Throwable cause) {
        super(message, cause);
    }
}
