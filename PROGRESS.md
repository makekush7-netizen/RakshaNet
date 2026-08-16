# Current Progress

Last updated: 2026-08-16
Overall state: **Phase 3 in progress; bidirectional repair installed, physical retest pending**

## Completed

- Read the full PRD and preserved its scope.
- Confirmed workspace initially contained only `PRD (3).md`; no Android project
  or Git repository existed.
- Recorded physical devices: Redmi Note 10 Pro and Samsung Galaxy J8.
- Audited local tooling:
  - Java/Javac 17.0.12 available.
  - Android SDK platforms 36 and 36.1 available.
  - Android build-tools 35.0.0, 36.0.0, 36.1.0, and 37.0.0 available.
  - adb 37.0.0 available and daemon starts successfully.
  - No device was connected during the audit.
  - No standalone `gradle` command was detected; project must include a wrapper.
- Locked v1 transport to Nearby Connections `P2P_CLUSTER`; see D-001.
- Clarified broadcast semantics and security/TTL model; see D-002 through D-004.
- Created durable agent, strategy, decision, progress, and test records.
- Scaffolded a dependency-light Kotlin Android app (`com.rakshanet.meshchat`)
  with min SDK 26 and compile/target SDK 36.
- Generated and checked in a Gradle 8.13 wrapper.
- Built the Phase 0 debug APK successfully:
  `app/build/outputs/apk/debug/app-debug.apk` (SHA-256
  `997F694384AD6670759E5EF3B7F9EE530D5415059E206D09893938353D068520`).
- Ran `:app:testDebugUnitTest :app:lintDebug`: build passed; there are no unit
  sources yet and lint reports 0 errors / 1 toolchain-version warning.
- Removed the temporary wrapper-generation directory after copying the wrapper.
- Added `scripts/device-check.ps1` for consistent read-only inventory and
  explicit bootstrap APK install/launch across all authorized USB devices.
- Added `DEVICE_TEST_GUIDE.md` with novice-friendly setup and evidence rules.
- Captured the Redmi inventory over authorized ADB: Xiaomi/Redmi M2101K6P
  (`sweetin`), Android 12/API 31, MIUI 13 V13.0.10.0.SKFINXM, arm64-v8a.
- Attempted bootstrap installation on the Redmi; Android returned
  `INSTALL_FAILED_USER_RESTRICTED`, so MIUI's **Install via USB** permission or
  on-device install confirmation must be enabled before retrying.
- Copied the APK successfully to Redmi
  `/sdcard/Download/RakshaNetMesh-debug.apk` as a manual-install fallback. MIUI
  does not expose a compatible exported installer intent to ADB, so the user
  must open that file from Downloads and tap Install.
- Captured the Samsung inventory over authorized ADB: Samsung SM-J810G
  (`j8y18lte`), Android 10/API 29, armv7, build J810GDDU6CUI1.
- Phase 0 exit test passed on 2026-08-16: `adb install -r` successfully
  installed the bootstrap APK and `am start -W` launched
  `com.rakshanet.meshchat/.MainActivity` on both the Samsung and Redmi.
- Implemented the Phase 1 vertical slice: signed packet bodies/envelopes,
  Android Keystore identity, deterministic validation, atomic dedup storage,
  bounded TTL relay, a `PacketRouter` interface, `MockPacketRouter`, Room
  entities/DAOs, and the Compose shared-channel debug UI.
- Built and tested the Phase 1 APK successfully. Seven JVM unit tests pass:
  packet validation (3), authentication/tamper handling (2), and coordinator
  send/relay/dedup behavior (2). Lint has 0 errors; its remaining warnings are
  dependency update/KAPT guidance, deliberately retained while versions stay
  compatible with compile SDK 36 and AGP 8.12.
- Installed the Phase 1 APK on both phones and started its `MainActivity`.
  Android confirmed display on Samsung; both devices were locked/asleep during
  UI hierarchy inspection, so user-visible mock UI interaction is still pending.
- Phase 1 physical mock-UI gate passed on both phones. User evidence shows a
  local "hi" message, verified mock inbound messages, and status
  "Accepted and relayed packet · TTL 6". The UI now labels the displayed value
  as "Received TTL" to distinguish it from the outbound relayed TTL.
- Implemented Phase 2 foreground-service shell: connected-device manifest type,
  API-aware Nearby Devices/notification permission request, start/stop control,
  persistent low-priority notification, and on-screen foreground-service state.
- `:app:testDebugUnitTest :app:assembleDebug :app:lintDebug` passed after the
  Phase 2 change. Added three API-level permission requirement tests. The APK
  was installed on both phones; real service lifecycle verification is pending.
- Android service/notification inspection confirms the foreground service is
  currently real and active on both devices (`isForeground=true`, notification
  ID 1001, channel `mesh_relay_status`, title "RakshaNet relay active").
  This verifies startup; the required 10-minute screen-off/recents survival
  gate remains pending.
- Phase 2 physical exit gate passed on both phones: user reports completing the
  screen-off/background/recents survival test during a walk, and a follow-up
  ADB inspection still showed both services foreground and start-requested.

## In progress

- Phase 3 Nearby Connections single-hop transport behind `PacketRouter`.

## Latest implementation evidence

- Added Google Play services Nearby 19.3.0 with `P2P_CLUSTER`, simultaneous
  advertising/discovery, authenticated endpoint connection requests, payload
  callbacks, and connection lifecycle/status reporting.
- Added bounded binary packet framing and round-trip/malformed-frame tests.
- Added selectable Mock/Nearby transport without exposing transport details to
  mesh core. Nearby mode starts the foreground relay and asks all API-specific
  permissions before discovery.
- 2026-08-16: final Phase 3 pre-hardware command
  `:app:testDebugUnitTest :app:assembleDebug :app:lintDebug` passed. The APK was
  installed and launcher-started on both phones; actual peer discovery/delivery
  remains the next hardware gate.
- First two-phone Phase 3 attempt was partially successful: Redmi-to-Samsung
  delivery worked, but Samsung-to-Redmi did not. Evidence showed Nearby status
  8003 (`STATUS_ALREADY_CONNECTED_TO_ENDPOINT`), a simultaneous discovery race.
  The router now deduplicates connection requests and treats status 8003 as the
  already-connected success case. It also no longer labels a real transport send
  as mock. Build/tests/lint passed and the fix is installed on both phones.
- The follow-up physical attempt still failed Samsung-to-Redmi: Samsung showed
  no peers while Redmi showed one. Live ADB logs showed Nearby encrypted
  Bluetooth keep-alives, proving the connection was still alive. The diagnosis
  was an app bug: `onEndpointLost` (discovery loss) was removing a live endpoint
  from the transport send set. The repair now retains it until `onDisconnected`.
- Messages now render in each phone's durable local receipt order, with local
  receipt time displayed. Sender wall clocks cannot place new received messages
  at the top. Local messages show `✓ queued`, which is a truthful locally saved
  state and deliberately not a claimed recipient delivery receipt.
- 2026-08-16 repair verification: `:app:testDebugUnitTest :app:assembleDebug
  :app:lintDebug` passed (12 JVM tests; lint 0 errors); the repaired APK was
  installed and launcher-started on Samsung and Redmi.
- Research/architecture result is recorded in D-007 and D-008: use receipt
  order now; add HLC, durable outbox, addressed packets, and signed ACKs in
  Phase 3.5.
- Added device-profile UI: a persistent, user-selected display name is used for
  Nearby advertising/connection prompts; changing it restarts Nearby so the
  advertised name updates. Added a confirmation-gated `Clear chat` action that
  removes visible history only from the local device and keeps seen-packet IDs.
  The mock-only injection control is hidden while using Nearby.
- `:app:testDebugUnitTest :app:assembleDebug :app:lintDebug` passed after the
  UI/storage change (12 JVM tests; lint 0 errors). D-009 records the intended
  disaster UX: one explicit network bootstrap, then one-tap automatic joining
  only for verified network members.
- Phase 3's bidirectional hardware gate now passes: user confirmed the repaired
  Nearby path works in both directions and supplied screenshots showing both
  phones receiving the other phone's text. This is recorded as T11-R/T11-F PASS.
- Observed recovery gap during device-name reconnection testing: Redmi showed
  Nearby status 13 and Samsung showed `8012 STATUS_ENDPOINT_IO_ERROR` while a
  pending connection prompt remained. Samsung was later ADB-offline, so no
  paired log could be captured. `8012` is an endpoint read/write failure, not a
  user-input error. No behavior was changed at the user's request; this is the
  concrete trigger for the planned bounded disconnect/backoff/rediscovery state
  machine and durable outbox.
- Implemented D-010 recovery: transient Nearby failure callbacks now clear
  stale endpoint state, schedule 1s/2s/4s bounded jittered retries, restart
  advertising/discovery, fully reset endpoints after three consecutive failures,
  and flush in-memory queued packets after reconnection. Added two deterministic
  `RecoveryPolicyTest` cases. `:app:testDebugUnitTest :app:assembleDebug
  :app:lintDebug` passed with 14 JVM tests and lint 0 errors.
- First physical recovery test found a retry/pairing loop: both phones retried
  simultaneously, displayed unsynchronised prompts, briefly connected, then a
  stale retry restarted the session. Implemented D-011: one name-elected
  requester only, retry cancellation on a valid prompt/connection, 250ms
  stop/start settling delay, and suppression of benign already-running states.
  Added two `ConnectionInitiationPolicyTest` cases. Full build/test/lint passed
  with 16 JVM tests; physical retest remains required.
- Follow-up physical retest partially improved: user observed several fast,
  successful reconnects. A later Redmi Bluetooth-off/on attempt did not pair and
  both screenshots ended at `Nearby transport stopped`. Read-only ADB evidence
  shows Bluetooth currently ON on both devices; Nearby logs on Samsung say
  endpoint discovery is delayed because location permission is missing. Samsung
  package state is inconsistent: fine location granted, coarse location denied.
  No code change was made in response to this observation. Restore Samsung's
  location permission before treating the remaining failure as a router defect.
- User then observed that continuous messaging alone could trigger recovery and
  repeated confirmation prompts. Live logs showed encrypted keep-alives remained
  healthy. Root cause: relaying a received packet while excluding the only
  ingress peer produced zero eligible relay targets, which the transport treated
  as zero connected peers. Added a tested `NO_RELAY_TARGET` state; this no longer
  queues the relay or rebuilds a healthy connection.
- Recovery now distinguishes payload retry from session recovery. A failed
  outgoing payload is mapped back to its packet and retried without disconnecting
  the endpoint. Bluetooth state ON triggers an immediate controlled restart.
- Implemented protocol v2 community/private vertical slice: signed persistent
  peer identities and names, peer announcements, saved peer directory, recipient
  routing, relay-without-display on uninvolved devices, signed delivery ACKs,
  true private-message double ticks, Room v1-to-v2 migration, and Community/peer
  conversation selector UI.
- First contact requires one matching-code confirmation. The verified advertised
  fingerprint is remembered and automatically accepted on later reconnections.
- Full command `:app:testDebugUnitTest :app:assembleDebug :app:lintDebug` passes:
  24 JVM tests, 0 failures, lint 0 errors. APK installed/launched on Redmi and
  Galaxy J8; Room migration and UI launch show no crash. Physical messaging and
  reconnection verification remain required.
- Shareable artifact produced at
  `artifacts/RakshaNet-Mesh-v2-private-community-debug.apk`, 16,756,767 bytes,
  SHA-256 `2233C539D0363A8FF38BD047105FE2CE03F3A4B310C0D2B3FD4D919B5F2C9226`.
- First three-phone admission attempt failed: Redmi and Galaxy J8 retained one
  healthy encrypted Nearby link, while the new Samsung stayed in recovery and
  never appeared as a connected peer. The user supplied screenshots; ADB logs
  on the original pair confirmed continuing keep-alives and active scanning.
- Trusted reconnect T25 passes: the user confirmed the original Redmi/Galaxy J8
  pair now reconnects without repeating matching-code confirmation.
- Root cause in app recovery: a failed new-neighbor request was routed through
  session recovery, whose healthy-peer guard correctly preserved A-B but also
  prevented any retry of C. Added per-endpoint discovered/requested/retry state,
  bounded neighbor retry that never tears down healthy links, and diagnostic
  `RakshaNetNearby` logs.
- Peer announcements now relay with normal TTL. This lets a phone connected via
  one bridge appear as a private target on all mesh members instead of only its
  direct neighbor.
- `:app:testDebugUnitTest :app:assembleDebug :app:lintDebug` passed after the
  repair; the final focused rerun passed 27 JVM tests with 0 failures and rebuilt
  the APK. v2.1 was installed/launched on the USB-connected Redmi and Galaxy J8.
- New shareable artifact:
  `artifacts/RakshaNet-Mesh-v2.1-three-node-retry-debug.apk`, 16,756,767 bytes,
  SHA-256 `0CD1EAB041978CEF99E866CED1FF238679D709A115989A046921298A429E1577`.
- Regression investigation, 2026-08-16: both original phones discovered each
  other but the Galaxy J8 had coarse location denied and Nearby Bluetooth socket
  attempts timed out (`8007`). USB permission repair plus a controlled Bluetooth
  radio reset produced `onConnectionResult(... SUCCESS)` on both phones and
  encrypted keep-alive acknowledgements.
- Fixed two startup/stability defects found during that investigation: the app
  now starts Nearby by default after an app/process restart instead of silently
  reverting to mock mode, and endpoint connection ownership is deduplicated by
  stable advertised identity so BLE/Classic aliases of an already connected
  phone do not create duplicate connection attempts. Full build/test/lint passed
  (27 JVM tests, 0 failures). The revised debug APK is installed on Redmi and
  Galaxy J8; final physical post-install confirmation is pending while the
  Samsung screen is locked.
- Implemented D-015 background ownership: `RakshaNetApplication` exposes one
  process-level `MeshRuntime`; the foreground service now owns real Nearby
  start/stop, and `MainActivity` no longer creates or destroys the router,
  coordinator, identity, or Room database. Added a timeout-bounded recovery wake
  lock and 20s-idle/6s-scan connected discovery duty cycle.
- Full `:app:testDebugUnitTest :app:assembleDebug :app:lintDebug` passes with
  29 JVM tests and 0 failures. The build was installed on Redmi and Galaxy J8;
  both returned successful Nearby connection results and encrypted keep-alives.
  After both activities were sent to the background, the same service processes
  and keep-alives remained healthy for 35 seconds. USB charging kept the displays
  awake, so this is background evidence only, not the unplugged screen-off gate.
- Shareable background-runtime artifact:
  `artifacts/RakshaNet-Mesh-v2.2-service-owned-background-debug.apk`, 16,756,767
  bytes, SHA-256 `BFA1E674FE19F95469A5A3242753A51EB6549E67831668487FA91FF449D97685`.
- User physical verification passed multi-minute screen-off/background delivery:
  after waking the phones, private messages delivered immediately and the
  recipient's signed ACK produced `✓✓`. Community messages intentionally retain
  one `✓ mesh`, because an open broadcast has no fixed recipient set whose ACKs
  could define an all-delivered state.
- Corrected stale connection messaging per D-016. A discovery/advertising error
  can no longer overwrite a healthy connected-peer status, and a failed payload
  retries without forcing session recovery. Full
  `:app:testDebugUnitTest :app:assembleDebug :app:lintDebug` passed: 29 JVM tests,
  0 failures, lint clean.
- v2.3 was installed and launched on USB-connected Redmi and Galaxy J8. Both
  exchanged continuous encrypted Bluetooth keep-alives after reconnect. Third
  phone candidate artifact:
  `artifacts/RakshaNet-Mesh-v2.3-three-phone-candidate-debug.apk`, SHA-256
  `3A3A63752350941F998D0FF5207BD47DEDF672F3423110993E26D6A1D9F3D975`.
- v2.3 three-phone admission retest failed. Samsung 3 repeatedly cycled through
  4/2-second session recovery and never joined, including after all three users
  left and rejoined. The original phones' ADB logs only discovered each other;
  neither reported Samsung 3. Samsung 3 reports Nearby Devices and Notification
  granted, with no Location permission entry. That is expected on Android 12+
  and is not itself evidence of a denied requirement. Its API level and Nearby
  advertising/discovery status code require USB log capture before another fix.
- USB inventory identified Samsung 3 as Samsung Galaxy A17 `SM-A176B`, Android
  15/API 35, build `A176BXXS2AYI4`. All visible Nearby/notification permissions
  were granted and Bluetooth/Wi-Fi/Location services were enabled.
- Root cause captured directly from Play Services Nearby 19.2: advertising and
  discovery were blocked sequentially by 8032 (Wi-Fi state), 8034 (coarse
  Location), and 8036 (fine Location). The manifest had incorrectly capped
  Wi-Fi and Location declarations at Android 12, explaining why Location did
  not appear in Samsung 3 settings. D-017 removes those caps, requests the
  enforced permissions at first launch, and makes setup failures explicit
  instead of retrying forever.
- Full `:app:testDebugUnitTest :app:assembleDebug :app:lintDebug` passes after
  the complete permission repair: 30 JVM tests, 0 failures, lint clean. v2.4
  installed successfully on both USB-connected Samsung phones and the required
  Location grants were applied for the controlled test. Both devices then
  dropped from ADB before post-install discovery could be observed, so physical
  admission remains unverified.
- Artifact: `artifacts/RakshaNet-Mesh-v2.4-android15-permission-fix-debug.apk`,
  16,756,767 bytes, SHA-256
  `5E5FDF0748F1397FE47ABE8720B9DB081B33676DA8116F2F16786396E842D8AC`.
- User subsequently completed the v2.4 physical three-phone retest and reports
  all expected behavior works: fast leave/rejoin, screen-off persistence,
  Community delivery to all, and private delivery only to the selected peer.
  True out-of-range A→B→C bridge/range coverage was intentionally deferred.
- Created `HANDOFF_TO_CLAUDE.md`, a concise handout for the full RakshaNet app
  planning conversation. It records implementation boundaries, physical
  evidence, Android 15 permission lesson, artifact hash, and unproven limits.

## Not started

- Phase 4 real multi-hop.
- Phase 5 OEM onboarding.
- Phase 6 library extraction/release evidence.

## Known blockers and dependencies

- The third Samsung is available, but is not USB-connected for permission/log
  inspection. Physical three-node admission and message tests remain pending.
- Exact Redmi MIUI and Samsung Android versions are intentionally not guessed;
  capture them from `adb` before implementing OEM deep links.

## Immediate next action

When hardware is next available, run the controlled true bridge/range test:
place A and C outside direct range with B as the only bridge, then verify one
private A→C message and its signed acknowledgement traverse B.

## Session update template

Append or revise the sections above after material work. Record:

- files/features changed;
- commands/tests run and their outcomes;
- current phase/gate status;
- blockers requiring user action;
- exactly one immediate next action.
