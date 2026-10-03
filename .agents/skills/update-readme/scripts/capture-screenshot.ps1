param(
    [Parameter(Mandatory = $true)]
    [string]$ScreenName,

    [string]$DeviceId = "emulator-5554"
)

$ErrorActionPreference = "Stop"

$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
if (-not (Test-Path $adb)) {
    $adbCmd = Get-Command adb -ErrorAction SilentlyContinue
    if ($adbCmd) {
        $adb = $adbCmd.Source
    } else {
        Write-Error "adb.exe not found in LOCALAPPDATA or PATH."
        exit 1
    }
}

# Verify device is connected
$devices = & $adb devices
if ($devices -notmatch $DeviceId) {
    Write-Warning "Device '$DeviceId' not found in adb devices."
    Write-Host "Running devices:`n$devices"
}

# Ensure figure directory exists
$figureDir = Join-Path $PSScriptRoot "..\..\..\..\figure"
$resolvedFigureDir = [System.IO.Path]::GetFullPath($figureDir)
if (-not (Test-Path $resolvedFigureDir)) {
    New-Item -ItemType Directory -Path $resolvedFigureDir -Force | Out-Null
}

$cleanName = $ScreenName.ToLower().Replace(" ", "").Replace("-", "").Replace("_", "")
if (-not $cleanName.EndsWith(".png")) {
    $fileName = "$cleanName.png"
} else {
    $fileName = $cleanName
}

$targetPath = Join-Path $resolvedFigureDir $fileName
$remotePath = "/sdcard/$fileName"

Write-Host "Capturing screenshot on $DeviceId to $remotePath..."
& $adb -s $DeviceId shell screencap -p $remotePath

Write-Host "Pulling screenshot to $targetPath..."
& $adb -s $DeviceId pull $remotePath $targetPath

# Clean remote temporary file
& $adb -s $DeviceId shell rm -f $remotePath

if (Test-Path $targetPath) {
    $item = Get-Item $targetPath
    Write-Host "Successfully captured: $($item.FullName) ($($item.Length) bytes)"
} else {
    Write-Error "Failed to pull screenshot."
}
