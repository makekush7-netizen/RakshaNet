# PRD — Offline BLE Mesh Chat Module (RakshaNet foundation)

## 0. Read this first (context for whoever/whatever is picking this up)

This doc is being handed to an AI coding agent (Codex) as the starting context for
a fresh session. It captures a design conversation that already happened, so the
agent should **not re-litigate settled decisions** below without a specific new
reason — but should push back the same way if something in a later prompt
contradicts them or looks like it'll cause problems down the line.

### Who's building this
- 2nd-year B.Tech student, tier-3 college, no mentor, self-taught via AI tools.
- "Medium-level vibe coder" — can read and follow code in preferred languages,
  understands the underlying tech reasonably well, but has **never built an
  Android app before** and doesn't know Android Studio.
- Has shipped two solid hackathon-grade projects in 2-3 day sprints before
  (a voice-agent product and a desktop AI-agent product), both AI-assisted, so
  is capable of moving fast and integrating AI-generated code — just new to
  the mobile/BLE domain specifically.
- Has Codex Pro (weekly credits) as primary code-generation tool.
- Has **two real Android phones** available for testing (brands not yet
  confirmed — first task is to identify them, since OEM matters a lot here,
  see §5).
- Wants this module to be genuinely reusable for a future, larger project
  (RakshaNet — a disaster-management app with training/gamification,
  offline mesh alerting, and guidance phases) but is deliberately scoping
  **this** effort down to just the mesh/chat module, standalone, working
  end-to-end on two phones.

### How to work with this person
- They explicitly do not want a "yes sir" assistant — they want an advisor
  that pushes back when something is a bad idea, flags risk, and corrects
  wrong assumptions, even if it means disagreeing with a previous AI's advice.
- Prefer explaining the *why* behind a technical requirement, not just the
  instruction — they're trying to learn the domain, not just get output.
- They have limited time (not "a week"), so scope aggressively: working
  vertical slices over broad partial coverage.
- Don't assume Android Studio / Gradle / adb familiarity. Give exact steps
  when a new tool is introduced.

---

## 1. Problem & Scope

Build a **standalone, reusable Android module**: two (eventually N) phones
discover each other and exchange text messages over Bluetooth Low Energy
(BLE), with **no internet, no cellular, no central server**. Multi-hop relay
(TTL-based) so a message can reach a phone outside direct BLE range via an
intermediate device.

This is explicitly step one of a larger disaster-management concept
(RakshaNet: training/gamification phase + this mesh phase + a future
prediction/guidance phase), but **this PRD only covers the mesh/chat module**.
Do not scope-creep into training UI, SOS-specific schemas, or the responder
dashboard unless asked.

### Out of scope for this phase
- Strong end-to-end encryption / confidentiality (explicitly deprioritized —
  see §2.5 for the reasoning and what we're doing *instead*).
- iOS.
- The Duolingo-style training module.
- The prediction/guidance engine.
- Cloud/gateway sync to a web dashboard.

---

## 2. Architectural decisions already made (don't relitigate without reason)

### 2.1 Build from scratch, don't fork/extract bitchat
Earlier advice suggested extracting bitchat's BLE mesh engine into this
project. **Decision: reject that for this builder specifically.** Reading and
correctly repurposing someone else's non-trivial concurrent BLE/GATT code as
a first Android project, first BLE project, with no mentor, is higher-risk
than writing a minimal version from scratch. Bitchat's routing logic can be
referenced later once the basics are understood, not copied now.

### 2.2 Nearby Connections API vs raw BLE — open decision, lean toward Nearby Connections for v1
Two viable transports:
- **Raw BLE** (`BluetoothLeAdvertiser`/`BluetoothLeScanner`/GATT): full
  control over TTL/hop logic and packet format, but much more manifest/
  permission/lifecycle surface area for a first-timer.
- **Google Nearby Connections API** (Play Services): handles discovery +
  transport (auto-picks BLE/Bluetooth Classic/Wi-Fi Direct) for you. Less
  low-level control, but dramatically less boilerplate for a beginner.
  Requires Play Services — fine for the Indian Android market, not a real
  constraint.

**Recommendation carried into this doc: start with Nearby Connections for the
first working end-to-end demo, since the priority is a working reusable
module fast.** Raw BLE can be a v2 swap-in later using the same
transport-abstraction pattern below. Codex should confirm this trade-off
explicitly with the user before committing, since it's not fully locked in.

### 2.3 Transport abstraction (keep this regardless of BLE vs Nearby Connections)
Separate the app into:
- **Transport layer** — a `PacketRouter` interface with `sendPacket()` /
  `onPacketReceived()`. Two implementations: `MockPacketRouter` (simulated,
  no hardware, for solo dev/testing) and the real transport
  (`NearbyPacketRouter` or `BleTransportRouter`).
- **Core/business logic layer** — packet model, TTL/dedup/relay logic, local
  storage. Should not know or care which transport is underneath.
- **UI layer** — chat screens, peer list, service-status indicator.

This lets one person develop and demo most of the app without touching real
hardware every cycle, and swap transports later without a rewrite.

### 2.4 Packet format
```json
{
  "id": "uuid",
  "type": "TEXT_MESSAGE",
  "sender_id": "device-local-id",
  "payload": "string",
  "ttl": 7,
  "timestamp": 1234567890
}
```
- `id` used for dedup (a device must not re-relay a packet it's already seen).
- `ttl` decremented per hop; packet dropped at 0.
- `type` is an enum, kept generic/extensible on purpose since the future
  RakshaNet SOS schema will add more types — don't hardcode assumptions about
  a single message type into the router/storage layer.

### 2.5 Security posture — integrity/anti-spam, not confidentiality
Decision: **skip heavy end-to-end encryption** (reasonable for this scope,
content isn't sensitive, and it reduces complexity for a first build).
**But do not skip authenticity/integrity** — these solve a different problem
than encryption and are still needed:
- Each device generates a local keypair on first launch.
- Outgoing packets are signed; relaying devices can verify signature and
  drop/deduplicate garbage or flood traffic.
- Rationale: an unauthenticated open mesh is trivially spammable/spoofable
  by anyone in BLE range (fake alerts, flood-based denial of service against
  the relay network) — this is a trust/robustness problem, not a privacy
  problem, and matters even under a "no confidentiality needed" philosophy.

### 2.6 Foreground service is mandatory, not optional polish
BLE mesh relay requires the app to keep running with the screen off/app
backgrounded — same mechanism YouTube/Maps use (a foreground service with a
persistent notification), not something Android grants apps by default.

Targeting Android 14 (API 34) specifically requires (confirmed against
current Android docs, verify again if a lot of time has passed):
- `foregroundServiceType="connectedDevice"` declared on the service in the
  manifest.
- The `FOREGROUND_SERVICE_CONNECTED_DEVICE` manifest permission.
- At least one of `BLUETOOTH_CONNECT` / `BLUETOOTH_ADVERTISE` /
  `BLUETOOTH_SCAN` granted as a **runtime** permission *before* the service
  is started, or the OS throws a hard exception.
- A persistent, low-priority notification showing live mesh status (e.g.
  "RakshaNet active — 2 peers nearby") — also useful as a debugging signal.

### 2.7 OEM background-kill handling is a first-class problem, not a user-error edge case
Stock Android's official exemption
(`ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`) is necessary but **not
sufficient** on MIUI (Xiaomi/Redmi/Poco), ColorOS (Oppo/Realme), and
FuntouchOS (Vivo) — these layer a second, proprietary "Autostart"/battery
manager on top of stock Android with no public API, only brand- and
OS-version-specific settings deep-links. Plan:
1. Request the standard battery-optimization exemption (official cross-brand
   API).
2. Detect `Build.MANUFACTURER`; for the aggressive OEMs, show a short
   explainer screen deep-linking to that brand's autostart settings (pull
   current intents from a maintained reference like dontkillmyapp.com rather
   than guessing — these change between OS versions).
3. Treat both as **best-effort**, not guaranteed — the app must tolerate the
   service dying anyway (store-and-forward already does this) and must
   clearly show current service status on-screen so failures are visible
   during testing and to the user.

Action item: confirm the brand of both test phones early — if either is
Xiaomi/Oppo/Vivo/Realme, that's the first OEM flow to build and verify.

---

## 3. System sketch

```
┌─────────────────────────────────────────┐
│                UI Layer                  │
│  Chat screen · Peer list · Service status│
├─────────────────────────────────────────┤
│              Core Logic Layer            │
│  MeshPacket model · TTL/dedup/relay      │
│  Room DB (packet log, dedup, peers)      │
│  Signing/verification                    │
├─────────────────────────────────────────┤
│             Transport Layer              │
│  PacketRouter interface                  │
│   ├─ MockPacketRouter (dev/testing)      │
│   └─ NearbyPacketRouter / BleTransport   │
│       Router (real hardware)             │
├─────────────────────────────────────────┤
│         Foreground Service Shell          │
│  Keeps transport alive in background     │
│  Persistent status notification          │
│  OEM battery/autostart onboarding        │
└─────────────────────────────────────────┘
```

---

## 4. Execution strategy (phased, test-gated — do not move to the next phase until the current one is verified on real hardware where noted)

**Phase 0 — Environment setup**
- Install Android Studio (only needed for: opening the project, hitting Run,
  reading Logcat — not for hand-writing code).
- Enable Developer Options + USB debugging on both phones.
- Confirm both phones' brand/OS version (needed for §2.7).
- *Exit test:* an empty/template app installs and launches on both phones via
  USB + Run button.

**Phase 1 — Core data layer + Mock transport (no hardware needed)**
- `MeshPacket` model, `PacketRouter` interface, `MockPacketRouter`.
- Room DB for packet storage + dedup by `id`.
- Signing/verification logic (keypair generation, sign, verify).
- Basic chat UI wired to the mock transport (send a message, see it appear,
  simulate an "incoming" packet via a debug button).
- *Exit test:* full send/receive/dedup/TTL-decrement logic demonstrably
  correct using only the mock — no phone-to-phone step yet.

**Phase 2 — Foreground service shell**
- Foreground service with correct Android-14 `connectedDevice` type,
  permissions, and status notification (§2.6).
- Runtime permission request flow (`ActivityResultContracts
  .RequestMultiplePermissions`) walking the user through granting
  Bluetooth permissions before the service starts.
- *Exit test (real devices):* service visibly stays alive when app is
  backgrounded / screen off / after swipe-from-recents, on both phones, for
  at least several minutes.

**Phase 3 — Real transport, single hop, two devices**
- Implement `NearbyPacketRouter` (or `BleTransportRouter` if going raw BLE).
- Wire it in behind the same `PacketRouter` interface — UI/core logic should
  not need to change.
- *Exit test (real devices):* Phone A sends a text message, Phone B receives
  it with no internet/cellular active on either device.

**Phase 4 — Multi-hop relay + TTL**
- Add relay logic: a device that isn't the intended recipient re-broadcasts
  an unseen packet with `ttl - 1`, drops at `ttl == 0`.
- *Exit test:* with a 3rd device (borrow one temporarily if needed) confirm
  A → B → C relay works when A and C are out of direct range but both in
  range of B. If a 3rd device genuinely isn't available, this can be
  partially verified by simulating "out of range" via toggling Bluetooth off
  on the direct link and confirming relay logic activates correctly.

**Phase 5 — OEM battery/autostart onboarding**
- Standard exemption request + brand-specific autostart deep-link flow
  (§2.7), gated behind detected `Build.MANUFACTURER`.
- *Exit test (real devices):* on the more aggressive of the two test phones,
  confirm the service survives realistic idle/backgrounded conditions after
  the user completes onboarding, and confirm the on-screen status indicator
  correctly reflects service state if it does get killed.

**Phase 6 — Packaging as a reusable module**
- Once the above is solid, review module boundaries: is `PacketRouter` +
  core logic layer cleanly separable into its own Android library module
  (not just a package) so it can be pulled into the future RakshaNet app
  without copy-pasting? This is the point to actually do that refactor, not
  earlier — premature modularization before the logic is proven working
  would slow this down for no benefit yet.

---

## 5. Testing strategy

- **No BLE testing on emulators** — Android emulators don't reliably emulate
  real BLE radios; don't waste time trying to simulate real device-to-device
  behavior this way. Use `MockPacketRouter` for logic testing without
  hardware, and the two real phones for anything transport-related.
- **Primary dev loop:** USB-connect a phone → Android Studio Run button →
  test on-device → read crash traces in **Logcat** → feed stack trace back to
  Codex if something crashes.
- **Second-device install:** either USB-connect both phones simultaneously
  (Android Studio can target either from the device dropdown) or build an
  APK (`Build → Build Bundle(s)/APK(s) → Build APK(s)`) and `adb install` it
  on the second device, or transfer the APK file directly (USB/WhatsApp to
  self/etc).
- Every phase above has a named exit test — don't proceed past a phase until
  its exit test passes on real hardware where specified.

---

## 6. Known open risks / unresolved questions

- Nearby Connections vs raw BLE is not fully locked in (§2.2) — worth a
  quick explicit gut-check before Phase 3 starts, not mid-implementation.
- OEM autostart intents change between OS versions and aren't officially
  documented — treat any specific intent/package name as "best current
  knowledge, verify it still works," not a guaranteed API.
- Real multi-hop relay (Phase 4) is hard to fully verify with only two
  physical devices — plan around borrowing a 3rd device or accepting partial
  verification.
- Signing/verification adds a small amount of complexity — if time is
  extremely tight, this is the one piece that could be cut for a bare demo,
  but should be flagged explicitly as a known gap, not silently dropped.
