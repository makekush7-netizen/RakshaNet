# RakshaNet Offline Mesh Module — Handoff

**Date:** 2026-08-16
**Scope:** Standalone Android offline nearby-mesh chat prototype. It does not include cloud, cellular, SOS workflows, training, or dashboards.

## What has been achieved

RakshaNet is a working Kotlin/Jetpack Compose/Room Android offline chat prototype using Google Nearby Connections and a foreground service.

- Devices discover and connect with no internet or cellular service.
- Trusted peers reconnect without repeated confirmation after their first verified code match.
- The mesh remains usable after screens have been off for several minutes; waking a phone does not require an app restart.
- Leave/rejoin reconnect is fast.
- Community mode delivers to the mesh; private mode displays only on the selected recipient.
- Private messages get a real `✓✓ delivered` only after a signed recipient acknowledgement. Community deliberately shows one `✓ mesh`, because it cannot honestly claim acknowledgement from every member of an open changing group.
- Devices have user-set names; local chat can be cleared.

Physical three-phone behavior was confirmed by the user on Redmi Note 10 Pro (Android 12/API 31), Samsung Galaxy J8 (Android 10/API 29), and Samsung Galaxy A17 (Android 15/API 35).

## Current APK

`artifacts/RakshaNet-Mesh-v2.4-android15-permission-fix-debug.apk`

SHA-256: `5E5FDF0748F1397FE47ABE8720B9DB081B33676DA8116F2F16786396E842D8AC`

## Architecture to preserve

```text
Compose UI
    │
Foreground service → process-level MeshRuntime
    ├── Room: messages, dedup, peers, delivery state
    ├── Android Keystore signing identity + device profile
    ├── MeshCoordinator: validation, storage, TTL/dedup relay
    └── SelectablePacketRouter
          ├── NearbyPacketRouter (real transport)
          └── MockPacketRouter (development transport)
```

The `PacketRouter` boundary matters: UI, routing, and storage do not depend directly on Nearby APIs. Preserve it so future transports can replace Nearby without a core rewrite.

## Protocol and delivery semantics

Messages are signed with an Android Keystore ECDSA P-256 identity. A stable sender fingerprint is the identity; user names are display labels only. Signed immutable content includes packet ID, type, sender identity/name, payload, creation time, original TTL, channel, recipient when applicable, and an optional reference ID. Relays decrement only mutable remaining TTL and deduplicate packet IDs.

| Mode | Behavior | Receipt |
|---|---|---|
| Community | Displayed and relayed by every valid mesh node. | `✓ mesh` = injected into mesh. |
| Private | Can relay through others but displays only on sender/recipient. | `✓ queued` = local durable save; `✓✓ delivered` = signed recipient ACK returned. |

This is authenticated integrity and routing privacy, not end-to-end encryption. Relays do not display private packets but can still inspect plaintext.

## Transport/recovery behavior

- Real transport is Google Nearby Connections `P2P_CLUSTER`: offline nearby mesh, not raw BLE-only. Nearby may choose BLE, Bluetooth Classic, Wi-Fi Direct, or another local medium.
- Stable identity prevents duplicate BLE/Classic aliases and chooses one requester per pair, avoiding simultaneous-request races.
- Connected discovery uses short duty-cycle windows instead of continuous scanning to reduce radio contention.
- Recovery is bounded and, where possible, per-neighbor. Adding a new node must not deliberately reset a healthy link.
- The foreground service owns the actual runtime; it is not merely a notification shell.

## Important Android 15 lesson

The Galaxy A17 exposed a real compatibility failure in Play Services Nearby 19.2. It rejected startup sequentially with `8032` (Wi-Fi state), `8034` (coarse Location), and `8036` (fine Location), even though Nearby Devices had been granted. The current v2.4 APK therefore declares Wi-Fi state without an SDK cap and asks for both approximate and precise Location at first launch. Permanent setup errors are shown clearly instead of looping as a fake temporary connection failure.

## Verification record

- Current build command: `:app:testDebugUnitTest :app:assembleDebug :app:lintDebug`
- Result: **PASS** — 30 JVM tests, zero failures, lint clean.
- Physical evidence: two-way messaging, three-phone admission, Community delivery, private targeting, private signed delivery tick, remembered-peer reconnect, leave/rejoin, and multi-minute screen-off persistence.

The detailed source of truth is `PRD (3).md`, `DECISIONS.md`, `EXECUTION_STRATEGY.md`, `PROGRESS.md`, `TEST_MATRIX.md`, and `DEVICE_TEST_GUIDE.md`.

## Not yet proven

- True out-of-range A → B → C relay where B is the only bridge.
- Measured radio range, dense-network performance, best-route selection, or many-peer scale.
- 30-minute OEM idle survival/battery-manager onboarding.
- Process-death durable outbox recovery.
- End-to-end encryption, network membership credentials, rate limiting/spam controls, or group delivery receipts.

## Guidance for the complete RakshaNet app

1. Keep the `PacketRouter` and `MeshCoordinator` boundaries; do not wire feature screens directly to Nearby callbacks.
2. Keep the foreground-service/process-runtime ownership for any relay mode.
3. Model SOS as a new signed packet type, not a special text style. Decide its schema and acknowledgement/escalation policy first. Likely fields: event ID, category, severity, sender identity, creation time, expiry, TTL, and optional consented coarse location/accuracy.
4. Keep local receipt order in UI; independent phone clocks cannot produce a trustworthy global order for concurrent offline events.
5. Before claiming scalable mesh routing, perform the controlled bridge/range test and add topology/routing measurements.
6. For disaster usability, use an explicit network bootstrap (QR/invite/verified code) then one-tap join for known members; do not blindly auto-accept arbitrary nearby phones.

## Recommended next gate

Integrate this module into the complete UI, but before expanding disaster traffic prove one private A→C message and signed acknowledgement traverse B while A and C are outside one another's direct range.
