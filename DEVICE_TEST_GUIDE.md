# Physical Device Test Guide

The primary test devices are the Redmi Note 10 Pro and Samsung Galaxy J8. BLE,
Nearby Connections, foreground-service survival, and OEM background behavior
must not be marked passed until observed on these phones.

## One-time USB setup

1. On each phone, enable Developer Options by tapping the build/MIUI version
   seven times in **Settings > About phone**.
2. Enable **USB debugging** in Developer Options.
3. Connect with a data-capable USB cable, unlock the phone, and choose **File
   transfer** if Android shows a USB-mode prompt.
4. Accept the **Allow USB debugging?** RSA prompt and select **Always allow from
   this computer**.
5. Leave both phones unlocked for the initial inventory/install run.

## Agent-run Phase 0 check

From the workspace root:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\device-check.ps1
powershell -ExecutionPolicy Bypass -File .\scripts\device-check.ps1 -InstallBootstrap
```

The first command is read-only and captures the exact device/OS inventory. The
second installs the already-built debug APK and launches its main activity.

Expected screen text:

```text
RakshaNet Mesh bootstrap is running
```

Record each phone as `PASS` or `FAIL` in `TEST_MATRIX.md`; on failure, capture
the complete command output and relevant Logcat instead of guessing.

## Later radio-test baseline

For the offline delivery gate, disable mobile data and internet access on both
phones. Airplane mode behavior varies by OEM, so manually re-enable Bluetooth
and Wi-Fi after enabling airplane mode if the test calls for it. Verify lack of
internet separately; Nearby may use local Wi-Fi transport without internet.
