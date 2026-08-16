[CmdletBinding()]
param(
    [switch]$InstallBootstrap,
    [string]$ApkPath = "app\build\outputs\apk\debug\app-debug.apk"
)

$ErrorActionPreference = "Stop"
$adb = Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"
$packageName = "com.rakshanet.meshchat"
$launchComponent = "$packageName/.MainActivity"

if (-not (Test-Path -LiteralPath $adb)) {
    throw "adb was not found at $adb. Install Android SDK Platform-Tools first."
}

$deviceLines = & $adb devices | Select-Object -Skip 1
$authorizedSerials = @(
    $deviceLines |
        Where-Object { $_ -match "^([^\s]+)\s+device$" } |
        ForEach-Object { $Matches[1] }
)
$unauthorizedLines = @($deviceLines | Where-Object { $_ -match "\s+unauthorized$" })

if ($unauthorizedLines.Count -gt 0) {
    Write-Warning "An unauthorized phone is connected. Unlock it and accept the USB debugging RSA prompt."
}

if ($authorizedSerials.Count -eq 0) {
    [Console]::Error.WriteLine(
        "No authorized Android phone detected. Connect an unlocked phone with USB debugging enabled."
    )
    exit 2
}

function Get-Prop {
    param(
        [Parameter(Mandatory)] [string]$Serial,
        [Parameter(Mandatory)] [string]$Name
    )

    (& $adb -s $Serial shell getprop $Name).Trim()
}

$inventory = foreach ($serial in $authorizedSerials) {
    [pscustomobject]@{
        Serial       = $serial
        Manufacturer = Get-Prop -Serial $serial -Name "ro.product.manufacturer"
        Brand        = Get-Prop -Serial $serial -Name "ro.product.brand"
        Model        = Get-Prop -Serial $serial -Name "ro.product.model"
        Device       = Get-Prop -Serial $serial -Name "ro.product.device"
        Android      = Get-Prop -Serial $serial -Name "ro.build.version.release"
        Api          = Get-Prop -Serial $serial -Name "ro.build.version.sdk"
        Miui         = Get-Prop -Serial $serial -Name "ro.miui.ui.version.name"
        OneUi        = Get-Prop -Serial $serial -Name "ro.build.version.oneui"
        Abi          = Get-Prop -Serial $serial -Name "ro.product.cpu.abi"
        Build        = Get-Prop -Serial $serial -Name "ro.build.fingerprint"
    }
}

$inventory | Format-List

if (-not $InstallBootstrap) {
    Write-Output "Inventory complete. Re-run with -InstallBootstrap to install and launch the debug APK."
    exit 0
}

$resolvedApk = (Resolve-Path -LiteralPath $ApkPath).Path
foreach ($device in $inventory) {
    Write-Output "Installing bootstrap APK on $($device.Model) [$($device.Serial)]..."
    & $adb -s $device.Serial install -r $resolvedApk
    if ($LASTEXITCODE -ne 0) {
        throw "APK installation failed on $($device.Serial)."
    }

    Write-Output "Launching $packageName on $($device.Model)..."
    & $adb -s $device.Serial shell am start -W -n $launchComponent
    if ($LASTEXITCODE -ne 0) {
        throw "App launch failed on $($device.Serial)."
    }
}

Write-Output "Install/launch commands completed. Confirm the bootstrap message on every phone."
