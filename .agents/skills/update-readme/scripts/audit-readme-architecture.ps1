<#
.SYNOPSIS
    Audits the current codebase against README.md to detect discrepancies in
    architecture, tech stack versions, modules, and screenshot coverage.
#>

$repoRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot "..\..\..\.."))
$readmePath = Join-Path $repoRoot "README.md"
$tomlPath = Join-Path $repoRoot "gradle\libs.versions.toml"
$settingsPath = Join-Path $repoRoot "settings.gradle.kts"
$navKeysPath = Join-Path $repoRoot "app\src\main\java\com\smiledev\rafiq_quran\NavigationKeys.kt"
$figureDir = Join-Path $repoRoot "figure"

Write-Host "================================================="
Write-Host "  Rafiq Architecture & README Audit"
Write-Host "=================================================`n"

if (-not (Test-Path $readmePath)) {
    Write-Error "README.md not found at $readmePath"
    exit 1
}

$readmeContent = Get-Content $readmePath -Raw

# 1. Check Modules
Write-Host "[1/4] Checking Multi-Module Configuration..."
if (Test-Path $settingsPath) {
    $settingsContent = Get-Content $settingsPath -Raw
    $moduleMatches = [regex]::Matches($settingsContent, 'include\("(:[^"]+)"\)')
    $modules = $moduleMatches | ForEach-Object { $_.Groups[1].Value }
    foreach ($mod in $modules) {
        if ($readmeContent -match [regex]::Escape($mod)) {
            Write-Host "  [OK] Module $mod documented in README.md" -ForegroundColor Green
        } else {
            Write-Host "  [MISSING] Module $mod is in settings.gradle.kts but NOT documented in README.md!" -ForegroundColor Yellow
        }
    }
}

# 2. Check Key Dependency Versions
Write-Host "`n[2/4] Checking Tech Stack Versions..."
if (Test-Path $tomlPath) {
    $tomlContent = Get-Content $tomlPath -Raw
    $versions = @{
        "kotlin" = 'kotlin\s*=\s*"([^"]+)"'
        "hilt" = 'hilt\s*=\s*"([^"]+)"'
        "room" = 'room\s*=\s*"([^"]+)"'
        "maplibre" = 'maplibre\s*=\s*"([^"]+)"'
        "androidGradlePlugin" = 'androidGradlePlugin\s*=\s*"([^"]+)"'
    }

    foreach ($key in $versions.Keys) {
        if ($tomlContent -match $versions[$key]) {
            $ver = $matches[1]
            if ($readmeContent -match [regex]::Escape($ver)) {
                Write-Host "  [OK] $key version $ver matches README.md" -ForegroundColor Green
            } else {
                Write-Host "  [OUTDATED] $key is $ver in libs.versions.toml but NOT found in README.md!" -ForegroundColor Yellow
            }
        }
    }
}

# 3. Check Screen Routes vs Key Features in README
Write-Host "`n[3/4] Checking Navigation Routes & Feature Coverage..."
if (Test-Path $navKeysPath) {
    $navContent = Get-Content $navKeysPath -Raw
    $navMatches = [regex]::Matches($navContent, '@Serializable\s+(?:data\s+object|data\s+class)\s+([A-Za-z0-9]+)')
    $navKeys = $navMatches | ForEach-Object { $_.Groups[1].Value }
    Write-Host "  Detected $($navKeys.Count) routes in NavigationKeys.kt."
    foreach ($nav in $navKeys) {
        # Check if screen/feature is mentioned in README
        $simpleName = $nav -replace "Detail$", "" -replace "List$", "" -replace "Books$", "" -replace "Search$", ""
        if ($readmeContent -match $simpleName -or $readmeContent -match $nav) {
            Write-Host "  [OK] Route $nav mentioned in README" -ForegroundColor Green
        } else {
            Write-Host "  [NOTICE] Route $nav might be missing from Key Features in README.md" -ForegroundColor Cyan
        }
    }
}

# 4. Check Figure Screenshots
Write-Host "`n[4/4] Checking Screenshot Files in figure/ vs README.md..."
if (Test-Path $figureDir) {
    $pngFiles = Get-ChildItem -Path $figureDir -Filter "*.png"
    foreach ($file in $pngFiles) {
        $ref = "figure/$($file.Name)"
        if ($readmeContent -match [regex]::Escape($ref)) {
            Write-Host "  [OK] Screenshot $($file.Name) embedded in README.md" -ForegroundColor Green
        } else {
            Write-Host "  [UNREFERENCED] Screenshot $($file.Name) exists in figure/ but is not linked in README.md" -ForegroundColor Yellow
        }
    }
}

Write-Host "`nAudit complete."
