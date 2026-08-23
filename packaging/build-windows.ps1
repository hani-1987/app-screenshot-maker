#Requires -Version 5.1
<#
Builds the Windows native executable (and, if WiX Toolset is installed, an .msi installer)
for Screenshot Maker using jpackage. Must be run on Windows with a JDK 17+ on PATH or via
$env:JAVA_HOME (jpackage ships with the JDK; no separate download needed).

Usage:
  powershell -File packaging\build-windows.ps1
#>

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$cliTarget = Join-Path $root "cli\target"
$stagingDir = Join-Path $PSScriptRoot "windows-input"
$distDir = Join-Path $PSScriptRoot "dist"

Write-Host "==> Building the shaded CLI jar (mvn package)"
Push-Location $root
try {
    & mvn -q -DskipTests package
    if ($LASTEXITCODE -ne 0) { throw "Maven build failed" }
} finally {
    Pop-Location
}

$fatJar = Join-Path $cliTarget "screenshot-maker.jar"
if (-not (Test-Path $fatJar)) {
    throw "Expected shaded jar not found at $fatJar"
}

Write-Host "==> Staging a clean jpackage input directory (fat jar only)"
if (Test-Path $stagingDir) { Remove-Item -Recurse -Force $stagingDir }
New-Item -ItemType Directory -Force -Path $stagingDir | Out-Null
Copy-Item $fatJar -Destination $stagingDir

if (Test-Path $distDir) { Remove-Item -Recurse -Force $distDir }
New-Item -ItemType Directory -Force -Path $distDir | Out-Null

$jpackage = $null
if (Get-Command jpackage -ErrorAction SilentlyContinue) {
    $jpackage = "jpackage"
} else {
    # `java` on PATH is often a thin javapath shim with no sibling tools, so ask the JVM itself
    # where its real home is rather than guessing from the java.exe location. Routed through cmd
    # so stderr merges as plain text instead of tripping PowerShell 5.1's NativeCommandError.
    $javaHomeLine = cmd /c "java -XshowSettings:properties -version 2>&1" | Select-String "java.home"
    if ($javaHomeLine) {
        $javaHome = ($javaHomeLine -split "=", 2)[1].Trim()
        $candidate = Join-Path $javaHome "bin\jpackage.exe"
        if (Test-Path $candidate) { $jpackage = $candidate }
    }
}
if (-not $jpackage) {
    throw "Could not locate jpackage. Install a JDK 17+ and ensure jpackage is on PATH or JAVA_HOME is set."
}

$hasWix = $null -ne (Get-Command candle.exe -ErrorAction SilentlyContinue)
$packageType = if ($hasWix) { "exe" } else { "app-image" }
if (-not $hasWix) {
    Write-Host "==> WiX Toolset not found on PATH; building an app-image (a self-contained folder"
    Write-Host "    with ScreenshotMaker.exe) instead of an .exe/.msi installer. Install WiX 3.11+"
    Write-Host "    and re-run this script to also get an installer."
}

Write-Host "==> Running jpackage (--type $packageType)"
& $jpackage `
    --type $packageType `
    --name ScreenshotMaker `
    --app-version 1.0.0 `
    --vendor "Screenshot Maker" `
    --description "Walks every reachable page of a website, or screen of a Windows application, and captures a screenshot of each one." `
    --input $stagingDir `
    --main-jar screenshot-maker.jar `
    --main-class com.screenshotmaker.cli.Main `
    --dest $distDir `
    --win-console

if ($LASTEXITCODE -ne 0) { throw "jpackage failed" }

Write-Host ""
Write-Host "==> Done. Output in $distDir"
Get-ChildItem $distDir -Recurse -Depth 1 | Select-Object FullName
