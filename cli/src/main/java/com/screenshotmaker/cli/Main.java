package com.screenshotmaker.cli;

import picocli.CommandLine;

/**
 * Process entry point. Kept separate from {@link ScreenshotMakerCommand} so option parsing and
 * orchestration stay free of process-lifecycle concerns (exit codes, pre-parse log setup).
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        if (hasVerboseFlag(args)) {
            // Must be set before logback's context initializes, hence the raw pre-scan
            // instead of waiting for picocli to parse --verbose.
            System.setProperty("SCREENSHOTMAKER_LOG_LEVEL", "DEBUG");
        }

        CommandLine commandLine = new CommandLine(new ScreenshotMakerCommand());

        if (args.length == 0) {
            // --mode and --target are required, so running with no arguments would otherwise fail
            // instantly. That's exactly what happens when this .exe is double-clicked from
            // Explorer rather than run from a terminal, so show usage and wait instead of letting
            // the console window flash and vanish before anyone can read the error.
            commandLine.usage(System.out);
            waitForKeyPressIfLaunchedByDoubleClick();
            System.exit(2);
        }

        int exitCode = commandLine.execute(args);
        System.exit(exitCode);
    }

    private static void waitForKeyPressIfLaunchedByDoubleClick() {
        System.out.println();
        System.out.println("This is a command-line tool - it needs to be run from a terminal with the");
        System.out.println("options above, for example:");
        System.out.println("  ScreenshotMaker.exe --mode web --target https://example.com");
        System.out.println();
        System.out.print("Press Enter to close this window...");
        try {
            System.in.read();
        } catch (java.io.IOException e) {
            // Nothing sensible to do here; just close.
        }
    }

    private static boolean hasVerboseFlag(String[] args) {
        for (String arg : args) {
            if ("-v".equals(arg) || "--verbose".equals(arg)) {
                return true;
            }
        }
        return false;
    }
}
