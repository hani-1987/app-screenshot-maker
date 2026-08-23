# Screenshot Maker

Walks every reachable page of a website, or every reachable screen of a running Windows
application, and saves a screenshot of each one into a dedicated output folder — along with a
`manifest.json` and a browsable `index.html` gallery.

See [requirements.md](requirements.md) for the full functional and non-functional requirements.

## Project layout

```
core/            Domain model + CaptureEngine/ScreenshotSink SPI. No Selenium/Appium dependency.
storage/         Filesystem ScreenshotSink, run-folder naming, manifest.json + index.html writer.
engine-web/      CaptureEngine: concurrent same-site crawler (Selenium), full-page screenshots.
engine-desktop/  CaptureEngine: Windows UI Automation walker (Appium + WinAppDriver).
cli/             Composition root: picocli option parsing, wires an engine to the sink, reports results.
packaging/       jpackage build scripts that produce the native executable (Windows) / app (macOS).
```

Dependency direction is one-way: `core` ← `storage`, `engine-web`, `engine-desktop` ← `cli`. Neither
engine depends on the other; `cli` is the only module allowed to know both exist.

## Prerequisites

- JDK 17 or newer (JDK 21+ recommended) — `jpackage` ships with the JDK, nothing extra to install.
- Maven 3.9+.
- **Desktop mode only, Windows only:** [WinAppDriver](https://github.com/microsoft/WinAppDriver)
  installed and running (`WinAppDriver.exe`), and Developer Mode enabled
  (Settings → Privacy & Security → For developers).

## Build

```
mvn clean package
```

Produces the runnable fat jar at `cli/target/screenshot-maker.jar`.

## Run from the jar

```
java -jar cli/target/screenshot-maker.jar --mode web --target https://example.com --output ./screenshots
```

```
java -jar cli/target/screenshot-maker.jar --mode desktop --target "C:\Program Files\Notepad++\notepad++.exe"
```

Run `--help` for the full option list (max items/depth, concurrency, browser choice, robots.txt
opt-out, WinAppDriver URL, extra safety-blocklist keywords, etc).

## Build the native executable

### Windows

```
powershell -File packaging\build-windows.ps1
```

Produces `packaging/dist/ScreenshotMaker/ScreenshotMaker.exe` — a self-contained app-image with its
own bundled runtime; no separate Java install needed to run it. If the
[WiX Toolset](https://wixtoolset.org/) (v3.11+, `candle.exe`/`light.exe`) is on `PATH`, the script
instead produces a proper `.exe` installer via `--type exe`.

```
packaging\dist\ScreenshotMaker\ScreenshotMaker.exe --mode web --target https://example.com
```

### macOS

`jpackage` cannot cross-build a macOS bundle from Windows, so this step must run **on a Mac**:

```
chmod +x packaging/build-macos.sh
./packaging/build-macos.sh
```

Produces `packaging/dist-macos/ScreenshotMaker.app`. Only `--mode web` is available on macOS —
`--mode desktop` requires WinAppDriver/Windows UI Automation and exits with a clear error if run
there.

```
packaging/dist-macos/ScreenshotMaker.app/Contents/MacOS/ScreenshotMaker --mode web --target https://example.com
```

## Output

Each run creates a new subfolder under `--output` (default `./screenshots`), named
`<type>_<target>_<timestamp>`, containing:

- `0001_<label>.png`, `0002_<label>.png`, ... — one screenshot per captured screen/page, in
  capture order.
- `manifest.json` — machine-readable record of the run (target, timing, every screen's metadata,
  every error).
- `index.html` — a browsable gallery of the same run; open it directly in a browser.

## Testing

```
mvn test
```

Runs the unit test suite (45 tests as of this writing) covering the deterministic logic in every
module — URL normalization/link filtering, the concurrent crawl frontier's dedup/termination
behavior, robots.txt parsing, filename sanitizing, the desktop safety blocklist, and manifest
rendering. These run without a browser, WinAppDriver, or network access. The engines themselves
(actual browser driving, actual WinAppDriver sessions) are exercised by running the CLI against a
real target, as shown above — they need a live browser/WinAppDriver and aren't practical to unit test.

## Known limitations

See [requirements.md §5](requirements.md#5-known-limitations-by-design-not-gaps) — notably:
robots.txt support is best-effort, desktop traversal is bounded rather than exhaustive (see
`UiTreeWalker`'s class doc for exactly why), and desktop capture requires WinAppDriver running
separately.
