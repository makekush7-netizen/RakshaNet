# Execution Strategy

Last updated: 2026-08-16

## Outcome

Deliver an installable standalone Android app in which the Redmi Note 10 Pro
and Samsung Galaxy J8 discover one another and exchange signed broadcast text
messages with internet and cellular data disabled. The core must be reusable
behind a `PacketRouter` boundary and ready for later three-device relay testing.

## Delivery principles

- Build a thin vertical slice first: model -> router -> use case -> UI -> test.
- Keep hardware-free logic deterministic and heavily unit tested.
- Gate Android radio, lifecycle, permissions, and OEM claims on real-device
  evidence only.
- Use one protocol limit set initially: UTF-8 text <= 2 KiB, default/max TTL 7,
  and bounded dedup retention. Tune only from evidence.
- Never silently downgrade security checks to get a demo passing.

## Work breakdown and gates

### Phase 0 -- Bootstrap and device inventory

1. Create Gradle Android project and wrapper.
2. Build a minimal debug APK from the command line.
3. Run JVM/unit lint checks available without a device.
4. Connect each phone with USB debugging and capture manufacturer, model,
   Android version, API level, ABI, and battery-optimization state.
5. Install and launch the minimal APK on both phones.

Gate: local debug build passes; user/device evidence confirms install and launch
on both phones.

### Phase 1 -- Core plus mock vertical slice

1. Define versioned immutable `PacketBody`, mutable `PacketEnvelope`, and
   validation limits.
2. Implement canonical serialization, ECDSA signing/verifying, sender
   fingerprint derivation, and test-safe key abstraction.
3. Implement Room entities/DAOs for messages, seen packet IDs, and peers.
4. Implement `PacketRouter`, `MockPacketRouter`, and a mesh coordinator that
   validates -> verifies -> atomically deduplicates -> stores -> emits -> relays.
5. Add Compose shared-channel UI, service status, peer count, send action, and
   debug incoming-packet action for mock builds.
6. Unit-test validation, signatures, TTL boundaries, dedup races, relay rules,
   malformed data, and persistence. Add instrumentation tests only where Room or
   Android Keystore behavior cannot be faithfully covered on the JVM.

Gate: automated Phase 1 suite passes and mock send/receive behavior is observed
on at least one phone.

### Phase 2 -- Foreground service shell

1. Add version-aware permissions. Android 12+ requests Nearby Devices
   (`SCAN`, `ADVERTISE`, `CONNECT`); older Android follows the location rules
   required by the actual Nearby dependency and tested OS.
2. Declare `FOREGROUND_SERVICE` and, for target 34+, the
   `FOREGROUND_SERVICE_CONNECTED_DEVICE` permission plus
   `foregroundServiceType="connectedDevice"`.
3. Start only from a visible user action after prerequisites are granted; call
   foreground promotion immediately with a persistent status notification.
4. Expose explicit stopped/starting/active/error state in UI and notification.

Gate: on both phones the service survives background, screen-off, and recents
swipe for 10 minutes; status remains accurate. User performs this test.

### Phase 3 -- Nearby single-hop transport

1. Implement `NearbyPacketRouter` with simultaneous advertising/discovery using
   one stable service ID and `P2P_CLUSTER`.
2. Present and compare authentication digits before connection acceptance.
3. Frame versioned packets into BYTES payloads; reject oversize/unknown versions.
4. Track endpoint connection state, backpressure, retries, and structured errors.
5. Replace mock via dependency selection without UI/core changes.

Gate: in airplane mode (Bluetooth/Wi-Fi manually enabled if Android disables
them), A->B and B->A messages arrive, duplicates do not appear, reconnection
works after toggling Bluetooth, and no internet is available. User performs this
test on both phones.

### Phase 3R -- Session recovery

1. Treat Nearby endpoint I/O, disconnection, and failed payload transfer as
   recoverable: remove stale state and retry discovery with bounded backoff.
2. Queue packets during a temporary no-peer period and flush them on reconnect.
3. Reset all endpoints only after repeated consecutive failures; never require a
   force-stop for ordinary radio churn.

Gate: with the app left open, temporarily break a two-phone link, restore it,
and observe automatic reconnect plus delivery of a queued message. This does not
replace the later durable-outbox/process-death test.

### Phase 4 -- Application-level multi-hop

1. Relay each valid, verified, newly persisted broadcast to connected peers
   except the ingress peer with remaining TTL decremented.
2. Confirm duplicate arrival through multiple neighbors produces one message.
3. Add telemetry/debug view for packet ID, ingress peer, TTL, relay/drop reason.

Gate: a real three-device A->B->C topology passes with A and C unable to connect
directly. With only two devices, automated topology simulation can pass but this
phase remains `PARTIAL`, never fully passed.

### Phase 3.5 -- Reliable delivery protocol and conversation UX

1. Make outgoing packets durable: queue them before sending, drain when a peer
   connection becomes available, retry with bounded backoff, and survive process
   restart.
2. Add a protocol-v2 immutable recipient fingerprint and `DELIVERY_ACK` that
   references the original packet ID. Relays forward packets/acks but only the
   intended recipient displays the text.
3. Add hybrid logical-clock fields for causal ordering. Continue showing local
   receipt time, because offline device clocks are not authoritative.
4. Add per-message state in Room/UI: `queued` (one tick), `delivered` (two
   ticks only after signed ACK), retrying, and expired/failed with a reason.
5. Add a peer/identity picker so a third phone can choose Samsung's persistent
   key fingerprint rather than a temporary Nearby endpoint.
6. Add network bootstrap (verified QR/invite), membership persistence, and a
   one-tap `Join network` flow that auto-reconnects only to verified members.
   Keep a separately labelled, rate-limited public broadcast mode if needed.

Gate: automated tests cover clock skew, dedup, outbox restart/retry, signed ACK
validation, and accidental display suppression; two phones pass both directions
with queue/reconnect; physical three-device addressed delivery remains required.

Current v2 vertical slice status: peer identity/name announcements, addressed
private routing, community broadcast, signed ACKs, Room migration, and UI target
selection are implemented. The in-process retry queue is implemented; the
Room-backed process-death outbox and HLC remain pending. Hardware gates T25-T28
must pass before calling the v2 slice complete.

Three-node admission repair status: the first physical attempt formed only the
original A-B link. Per-peer retry now operates independently from session-wide
recovery, and membership announcements relay across bridges. Automated tests,
assembly, and lint pass; a physical three-phone retest is the active gate.

### Phase 5 -- OEM survival onboarding

1. Request the standard battery-optimization exemption only with clear user
   rationale and only when mesh is enabled.
2. Add a best-effort Redmi/Xiaomi autostart/battery settings route after exact
   MIUI version is known; fall back to app settings when an intent is unavailable.
3. Add Samsung guidance only if device testing demonstrates a need.
4. Persist enough state to recover safely after process death; do not promise
   immortal background execution.

Gate: user completes a 30-minute screen-off/background run on both phones and a
process-death/reopen recovery check; results are recorded per device.

### Phase 6 -- Reusable packaging and release evidence

1. Review dependency direction, then extract proven core contracts/logic into an
   Android library module if the API boundary is stable.
2. Produce debug APK, test reports, architecture README, setup guide, known
   limitations, and reproducible device-test checklist.
3. Run full clean build and regression matrix.

Gate: artifacts are reproducible and every claim maps to a recorded passing test
or a clearly named limitation.

## Immediate workstep

Install the v2.1 APK on the third phone and verify it joins without breaking the
existing pair. Then run the three addressed/community tests T26-T28; do not call
three-node admission or multi-hop complete without observed device evidence.

## User/device handoff protocol

When physical testing is required, provide:

1. the exact APK path or Android Studio action;
2. which phone(s) to connect and whether USB debugging is needed;
3. exact taps/settings and expected result;
4. an `adb`/Logcat capture command when useful;
5. a short result template (`PASS/FAIL`, phone, Android version, observation).
