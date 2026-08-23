# Screenshot Maker — Requirements

## 1. Purpose

A cross-platform Java tool that automatically walks every reachable part of a target — a website
or a running Windows desktop application — and saves a screenshot of each screen it finds into a
dedicated output folder, along with a manifest describing what was captured.

## 2. Scope

Two independent capture modes, selected per run:

| Mode      | Target                          | Mechanism                                    | Platforms       |
|-----------|----------------------------------|-----------------------------------------------|------------------|
| `web`     | A website (seed URL)             | Same-site link crawl via Selenium WebDriver   | Windows, macOS   |
| `desktop` | A running/launchable Windows app | UI Automation tree walk via WinAppDriver      | Windows only     |

Desktop capture is inherently Windows-only: it automates the Windows UI Automation subsystem via
WinAppDriver, which has no macOS equivalent. This is a platform constraint, not a gap — running
`--mode desktop` on macOS fails fast with an explicit error rather than attempting something that
cannot work. Website capture is unaffected and runs identically on both OSes.

## 3. Functional Requirements

### Capture

- **FR-1** — Given a seed URL, the tool shall discover and visit every page reachable via same-site
  `<a href>` links (breadth-first), up to configurable maximum page count and link-depth limits.
- **FR-2** — For each page visited, the tool shall capture a full-page screenshot (not just the
  visible viewport), by resizing the browser to the page's rendered content size before capturing.
- **FR-3** — The web crawler shall support Chrome, Firefox, and Edge, selectable per run.
- **FR-4** — Given a path to a Windows executable, the tool shall launch it and, given the title of
  an already-running window, shall attach to it, then walk its UI Automation tree.
- **FR-5** — For each screen the desktop walker reaches (the main window, and any dialog/secondary
  window newly opened by an interactive control), the tool shall capture a screenshot.
- **FR-6** — The desktop walker shall only activate a control once (per position in the traversal)
  and shall never activate a control whose accessible name matches a configurable safety blocklist
  (e.g. "delete", "exit", "uninstall", "sign out", "submit payment" — see FR-13).
- **FR-7** — Both engines shall enforce a maximum item count and maximum traversal depth, so a run
  on an arbitrarily large site or application always terminates in bounded time and disk usage.
- **FR-8** — A single item (page or screen) that fails to load/capture shall be recorded as an
  error and shall not abort the rest of the run; the run's exit code shall only indicate hard
  failure when *zero* items were captured.

### Output

- **FR-9** — Screenshots shall be written under a user-specified root output directory, in a
  dedicated per-run subfolder named from the target type, target, and timestamp, so repeated runs
  never overwrite each other.
- **FR-10** — Each run shall produce a machine-readable `manifest.json` (target, timing, every
  captured screen's sequence/label/source/breadcrumb/file, and every recorded error) and a
  human-browsable `index.html` gallery of the same data, written into that run's folder.
- **FR-11** — Screenshot filenames shall encode capture order (zero-padded sequence) and a
  sanitized label, and shall never escape the output directory regardless of what characters
  appear in a page title or URL (no path traversal via crafted input).

### Interface

- **FR-12** — The tool shall be operable entirely from the command line: mode, target, output
  directory, and all traversal/safety limits shall be settable as CLI options with documented
  mode-appropriate defaults.
- **FR-13** — The desktop safety blocklist shall ship with a sensible default set of destructive
  keywords and shall accept additional user-supplied keywords, appended (not replacing) the
  defaults, via a CLI option.
- **FR-14** — The web crawler shall respect `robots.txt` `Disallow` rules for the wildcard
  user-agent by default, with an explicit opt-out flag.
- **FR-15** — On completion, the tool shall print a summary (screens captured, errors, output
  path) to the console and shall exit with a status code suitable for scripting/CI use.

### Distribution

- **FR-16** — The tool shall be distributable as a native executable that runs without requiring a
  separately installed JRE (a self-contained runtime image), on both Windows and macOS.

## 4. Non-Functional Requirements

### Portability

- **NFR-1** — The build (Maven reactor, source) and the website capture engine shall run
  unmodified on Windows and macOS. Desktop capture is Windows-only by design (see §2).
- **NFR-2** — The packaged executable shall be built per-OS via `jpackage` (bundled runtime;
  Windows build produces an app-image/`.exe`, macOS build produces a `.app` bundle) — see
  `packaging/build-windows.ps1` and `packaging/build-macos.sh`.

### Performance

- **NFR-3** — The web crawler shall parallelize page visits across a configurable number of
  browser workers (default 4) sharing a single thread-safe frontier, rather than crawling
  strictly sequentially.
- **NFR-4** — Desktop UI automation shall run single-threaded by design (one input stream to one
  application is a hard constraint of UI automation, not a tunable performance knob).
- **NFR-5** — Every run shall be bounded by explicit max-item and max-depth caps (see FR-7) so
  runtime and disk usage are predictable and cannot grow unbounded on large targets.

### Reliability & Safety

- **NFR-6** — Per-item failures shall degrade gracefully (FR-8) rather than crashing the run.
- **NFR-7** — The desktop engine shall never activate a control matching the safety blocklist
  (FR-6, FR-13), to avoid autonomously triggering destructive actions in the target application.
- **NFR-8** — The web crawler shall stay within the seed's site (and, optionally, its subdomains)
  and shall not follow links to external domains, to avoid an unbounded, unintended crawl of the
  open web.

### Modularity & Maintainability

- **NFR-9** — The codebase shall be split into independently-buildable Maven modules with a strict
  dependency direction: `core` (domain model + engine/storage SPI, zero third-party engine
  dependencies) ← `storage` and each engine module ← `cli` (the only module allowed to depend on
  both engines). Neither engine module may depend on the other.
- **NFR-10** — Adding a new capture engine shall require no changes to `core`, `storage`, or the
  existing engines — only a new module implementing `CaptureEngine` plus one line in the CLI's
  `EngineRegistry`.
- **NFR-11** — Deterministic, dependency-free logic (URL normalization, link filtering, the crawl
  frontier's concurrency/termination logic, filename sanitizing, the safety blocklist, manifest
  rendering) shall be covered by automated unit tests that run without a browser, WinAppDriver, or
  network access.

### Observability

- **NFR-12** — The tool shall log structured, leveled output (SLF4J/Logback) with a `--verbose`
  flag for debug-level detail, and shall keep third-party library logging (Selenium, Apache
  HttpClient) quiet by default.
- **NFR-13** — Every run's manifest (FR-10) shall be sufficient on its own to understand what was
  captured, without needing to re-run the tool or inspect logs.

### Security

- **NFR-14** — The tool shall not store or transmit credentials; it operates entirely locally
  against the target the user specifies.
- **NFR-15** — All filesystem output paths shall be derived through the sanitizer in FR-11; no
  captured label, URL, or window/control name shall ever be used to build a path without going
  through it.

## 5. Known Limitations (by design, not gaps)

- **Robots.txt support is best-effort**, not a full RFC 9309 implementation: wildcard user-agent
  `Disallow` prefix matching only — no `Allow` precedence, `$`/`*` patterns, `Crawl-delay`, or
  sitemap parsing.
- **Desktop traversal is bounded, not exhaustive.** The walker recurses into genuinely new
  top-level windows/dialogs (which it can close to restore prior state), but a control that
  mutates the *current* window in place (a tab switch, an expander, a toggle) is screenshotted
  without further recursion from that mutated state, since there is no general way to "undo" an
  in-place UI change in an arbitrary third-party application. Exhaustive state-machine coverage of
  an arbitrary desktop app is not attempted.
- **Desktop capture requires WinAppDriver running separately** (Microsoft's Windows Application
  Driver service) and Developer Mode enabled on the target Windows machine; this tool does not
  install or manage that service.
- **No Linux support.** Website capture could plausibly run on Linux (same JVM + Selenium stack)
  but is untested and not part of this scope, which was defined as Windows + macOS.
- **The macOS executable cannot be built from this Windows checkout** — `jpackage` must run on the
  target OS. `packaging/build-macos.sh` is provided to be run on a Mac.

## 6. Out of Scope

- Visual diffing / regression comparison between runs.
- OCR or content extraction from captured screenshots.
- Authentication flows (logging into a site or app before crawling) — the seed target is assumed
  reachable/visible without a login step.
- A GUI; this is a command-line tool.
