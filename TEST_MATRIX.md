# Test Matrix

Last updated: 2026-08-16

Statuses: `PASS`, `FAIL`, `BLOCKED`, `NOT RUN`, `PARTIAL`.

| ID | Phase | Test | Environment | Status | Evidence / note |
|---|---:|---|---|---|---|
| T00 | 0 | Debug APK assembles | Local CLI | PASS | `:app:assembleDebug`; SHA-256 `997F...68520` |
| T01 | 0 | App installs and launches | Redmi Note 10 Pro | PASS | `adb install -r` and `am start -W` passed on 2026-08-16 |
| T02 | 0 | App installs and launches | Samsung Galaxy J8 | PASS | `adb install -r` and `am start -W` passed on 2026-08-16 |
| T03 | 1 | Packet validation and size limits | JVM unit test | PASS | 3 `PacketRulesTest` cases passed |
| T04 | 1 | Canonical sign/verify and tamper rejection | JVM unit test | PASS | 2 `PacketAuthenticatorTest` cases passed |
| T05 | 1 | Dedup is atomic under duplicate arrivals | JVM unit test | PASS | Covered by coordinator duplicate-arrival test using atomic in-memory store |
| T06 | 1 | TTL decrement/drop and relay exclusion | JVM unit test | PASS | Relay test asserts TTL 3→2 and ingress-peer exclusion |
| T07 | 1 | Mock send/incoming appears once in UI | Redmi + Samsung | PASS | User screenshots show local send, verified mock inbound messages, and TTL-6 relay status on both |
| T08 | 2 | Permission denial/retry does not crash | Both phones | NOT RUN | User test |
| T09 | 2 | Service survives background/screen-off/recents 10 min | Redmi | PASS | User completed walk/background/recents test; follow-up `dumpsys` is foreground/start-requested |
| T10 | 2 | Service survives background/screen-off/recents 10 min | Samsung | PASS | User completed walk/background/recents test; follow-up `dumpsys` is foreground/start-requested |
| T11 | 3 | Offline A->B and B->A delivery | Both phones | PARTIAL | Redmi→Samsung passed; Samsung→Redmi failed in first attempt due duplicate-request race; fix installed, retest pending |
| T11-R | 3 | Samsung-to-Redmi repair retest | Both phones | PASS | User confirmed the repaired bidirectional path works after the patched APK was installed; screenshots show each phone receiving the other's text on 2026-08-16. |
| T11-F | 3 | Final two-way Nearby gate | Both phones | PASS | Supersedes the initial partial row: user confirmed successful two-way delivery and supplied screenshots on 2026-08-16. |
| T20 | 3 | Device name persists and appears in a Nearby prompt | Both phones | NOT RUN | User names both phones, rejoins Nearby, and verifies the remote label in the confirmation prompt. |
| T21 | 1 | Clear local visible chat without re-enabling old packet display | Both phones | NOT RUN | User test; local clear must not remove messages from the other phone. |
| T22 | 3.5 | Transient endpoint failure recovers without app restart | Both phones | PARTIAL | Several fast reconnects were observed. Later retest failed after Redmi Bluetooth off/on; Samsung Nearby log reports delayed discovery because coarse location is denied. Restore permission and rerun before assigning a router failure. |
| T23 | 3.5 | Recovery produces one synchronized connection prompt | Both phones | NOT RUN | Regression test after observed retry loop: devices have distinct names; after reconnection there must be one matching-code prompt, not competing prompts/restarts. |
| T24 | 3.5 | One-peer relay exclusion does not trigger recovery | JVM unit test | PASS | `TargetSelectorTest`: excluding the sole ingress reports `NO_RELAY_TARGET`, distinct from no connected peer. |
| T25 | 3.5 | Trusted fingerprint reconnect auto-accepts | Redmi + Samsung | PASS | User reports and screenshots confirm the previously verified Redmi and Galaxy J8 reconnect without requesting confirmation again. |
| T26 | 3.5 | Private Samsung-to-Samsung only | 3 phones | PASS | User reports the v2.4 three-phone retest works as expected: individual mode delivers only to the selected peer. |
| T27 | 3.5 | Private Samsung-to-Redmi only | 3 phones | PASS | User reports the v2.4 three-phone retest works as expected: individual mode delivers only to the selected peer. |
| T28 | 3.5 | Samsung community broadcast reaches both peers | 3 phones | PASS | User reports the v2.4 three-phone retest works as expected: Community mode delivers to all. |
| T29 | 3.5 | Protocol-v2 validation/routing/ACK suite | JVM unit test | PASS | Full suite: 24 tests, 0 failures; covers signed v2 frame, private target display suppression, relay, ACK and delivery state. |
| T30 | 3.5 | Third phone joins an existing two-phone mesh | 3 phones | PASS | User completed v2.4 retest after Android 15 permission repair and reports all expected three-phone behaviors work. |
| T31 | 3.5 | Per-peer recovery preserves a healthy link | JVM unit test | PASS | `ConnectionRecoveryPolicyTest` covers failed second-neighbor retry, isolated session recovery, and elected-remote waiting; full focused suite 27 tests, 0 failures. |
| T32 | 3.5 | Restart defaults to Nearby, not mock | Redmi + Galaxy J8 | PASS | Revised app launched both service-owned transports automatically; both produced successful connection results and encrypted keep-alives without tapping Join network. |
| T33 | 3.5 | Activity background does not stop live mesh | Redmi + Galaxy J8 | PASS | Both activities sent to Home; service processes stayed foreground/start-requested and encrypted keep-alives continued for 35 seconds. |
| T34 | 3.5 | Unplugged 10-minute screen-off delivery | Redmi + Galaxy J8 | PARTIAL | User reports multi-minute screen-off followed by immediate delivery and private signed double tick without rejoining. Exact 10-minute duration was not confirmed, so the full-duration gate remains open. |
| T35 | 3.5 | Private delivery/ACK after screen-off | Redmi + Galaxy J8 | PASS | User woke the phones after several minutes screen-off; individual messages delivered immediately and showed `✓✓`. |
| T36 | 3.5 | Healthy link is not mislabeled by discovery retry | Local + Redmi/Galaxy J8 | PARTIAL | D-016 change passed full 29-test build/lint suite; v2.3 installed and encrypted keep-alives verified on both phones. User-facing banner behavior awaits observation during a transient scan failure. |
| T37 | 3.5 | Android 15 permission gate starts Nearby | Galaxy A17 | PASS | v2.4 declares/requests the observed Wi-Fi/coarse/fine Location requirements and user subsequently reports successful three-phone admission and messaging. |
| T12 | 3 | Disconnect/reconnect after radio toggle | Both phones | NOT RUN | User test |
| T13 | 3 | Authentication codes match; mismatch can be rejected | Both phones | NOT RUN | User test |
| T14 | 4 | Three-node relay with no A-C direct link | 3 phones | NOT RUN | All three phones now admit and message, but user has not yet performed the required out-of-direct-range bridge/range test. |
| T15 | 4 | Simulated topology relay/dedup | Automated | NOT RUN | Does not replace T14 |
| T16 | 5 | 30-minute idle/background survival | Redmi | NOT RUN | After MIUI onboarding |
| T17 | 5 | 30-minute idle/background survival | Samsung | NOT RUN | |
| T18 | 5 | Process death and reopen recovery | Both phones | NOT RUN | User test |
| T19 | 6 | Clean full build/test/lint | Local CLI | NOT RUN | |

## Local build evidence

- 2026-08-15: `gradlew.bat :app:assembleDebug` -- PASS (33 tasks executed).
- 2026-08-15: `gradlew.bat :app:testDebugUnitTest :app:lintDebug` -- PASS;
  unit-test task had no sources at bootstrap, lint had 0 errors and 1 warning.
- 2026-08-15: rebuilt after manifest/resource fixes -- PASS in 6 seconds.
- Remaining lint warning: Gradle 8.13 is behind the newest reported 8.x wrapper
  version. It is non-blocking and deliberately deferred until dependency setup.
- 2026-08-15: `scripts/device-check.ps1` correctly reported no authorized phone
  while `adb devices -l` returned an empty device list; physical tests remain
  blocked rather than inferred.
- 2026-08-15: final repeated `adb devices -l` check again returned no devices;
  Phase 0 cannot pass until the phones are connected and authorized.
- 2026-08-16: Redmi authorized and inventory captured. Two ADB install attempts
  returned `INSTALL_FAILED_USER_RESTRICTED`; APK push to Downloads passed, but
  visual/manual installation remains required.
- 2026-08-16: Galaxy J8 remained absent from `adb devices -l` and the inspected
  Windows PnP list.
- 2026-08-16: both phones became authorized. `scripts/device-check.ps1
  -InstallBootstrap` installed and launched the app on both devices. Samsung
  launch: 1.324 s; Redmi launch: 1.707 s.
- 2026-08-16: `:app:testDebugUnitTest :app:assembleDebug :app:lintDebug` --
  PASS; 7 JVM tests passed and lint reported 0 errors. Phase 1 APK installed
  on both devices. Samsung Android log recorded `MainActivity` displayed.
- 2026-08-16: user-provided screenshots passed the Phase 1 UI gate on both
  phones. They show the "hi" local message, mock inbound bubbles, and
  "Accepted and relayed packet · TTL 6".
- 2026-08-16: Phase 2 build/test/lint gate passed locally; foreground-service
  shell APK installed on both phones. Hardware permission/lifecycle tests remain
  unrun.
- 2026-08-16: Android `dumpsys` confirmed `MeshForegroundService` is
  `isForeground=true` with its persistent status notification on both devices.
  This is startup evidence only, not yet the 10-minute recents-survival pass.
- 2026-08-16: user confirmed completing the 10-minute screen-off/background/
  recents test on both devices; follow-up `dumpsys` still showed each foreground
  service active and start-requested. Phase 2 exit gate passed.
- 2026-08-16: Phase 3 pre-hardware build passed: 12 JVM tests, debug APK, and
  lint all passed; the Nearby-capable APK was installed on both phones.
- 2026-08-16: first Nearby hardware test found one-way delivery and status 8003
  (`STATUS_ALREADY_CONNECTED_TO_ENDPOINT`). Router race fix passed build/test/
  lint and is installed; T11 remains partial until both directions pass.
- 2026-08-16: user retest confirmed Samsung-to-Redmi still failed. Live ADB logs
  showed Nearby encrypted-Bluetooth keep-alives despite Samsung's empty peer
  state. Fixed the app's discovery-loss/connection-loss conflation. Full local
  command `:app:testDebugUnitTest :app:assembleDebug :app:lintDebug` passed
  (12 tests, lint 0 errors); repaired APK installed/launched on both devices.
  This is not a physical pass until Samsung-to-Redmi is observed.
- 2026-08-16: first three-phone admission attempt failed while the original pair
  remained healthy. Implemented independent per-neighbor retry and relayed peer
  announcements. Full test/build/lint passed, final focused suite passed 27 tests
  with 0 failures, and v2.1 was installed on the two USB-connected phones. T30
  remains FAIL until the new APK physically admits the third Samsung.
- 2026-08-16: original-pair regression investigation restored Samsung coarse
  location permission and reset both Bluetooth radios. Nearby then reported
  successful connection resolution and encrypted keep-alives on both devices.
  Added automatic Nearby startup and stable-peer duplicate suppression; full
  build/test/lint passed (27 tests, 0 failures), but T32 awaits final unlocked
  device confirmation.
- 2026-08-16: service-owned runtime, bounded recovery wake lock and connected
  discovery duty cycling passed full build/test/lint (29 tests, 0 failures).
  Both physical phones connected successfully. T32 and T33 pass; T34 remains
  NOT RUN because USB power prevented a true screen-off/idle test.
- 2026-08-16: user subsequently verified multi-minute unplugged screen-off
  recovery with immediate private delivery and signed double ticks. T35 passes;
  T34 remains partial only because the exact 10-minute duration was not stated.
  The truthful-status v2.3 repair passed 29 JVM tests/build/lint, was installed
  on both USB devices, and both exchanged encrypted Bluetooth keep-alives.

## Device inventory (capture with adb)

| Device | Manufacturer | Model | Android | API | Build | ABI | Inventory status |
|---|---|---|---|---:|---|---|---|
| Redmi Note 10 Pro | Xiaomi / Redmi | M2101K6P (`sweetin`) | 12 / MIUI 13 | 31 | V13.0.10.0.SKFINXM | arm64-v8a | PASS |
| Samsung Galaxy J8 | Samsung | SM-J810G (`j8y18lte`) | 10 | 29 | J810GDDU6CUI1 | armeabi-v7a | PASS |
| Samsung Galaxy A17 | Samsung | SM-A176B (`a17x`) | 15 | 35 | A176BXXS2AYI4 | arm64-v8a | PASS |
