# Architecture Decision Log

Last updated: 2026-08-16

## D-022: Emergency traffic uses structured signed packet types

- **Status:** Accepted; automated verification passed, physical verification pending
- **Choice:** Add `SOS_ALERT`, `SOS_UPDATE`, and `GUIDANCE_BROADCAST` packet
  types with strict structured payload codecs. Alerts reuse the existing packet
  signature, UUID dedup, TTL relay, and Room storage boundary rather than
  encoding emergency semantics as ordinary chat text.
- **Why:** Emergency traffic needs priority rendering, refinement references,
  validation, and independent retention without polluting conversations.
- **Consequence:** Every phone must run protocol-v2-capable RakshaNet to display
  the new types. Older builds safely reject unknown types; ordinary v2 chat
  remains compatible.

## D-023: Topology reports observations, not optimal routes

- **Status:** Accepted
- **Choice:** Derive the latest observed hop count from original and remaining
  TTL and display it as an observation. Do not label it shortest, fastest, or
  authoritative routing state.
- **Why:** Flood relay and transient Nearby links do not currently exchange
  weighted routing tables or measure end-to-end path quality.
- **Consequence:** The screen is useful operational telemetry. Best-path routing
  requires a later protocol with link metrics, freshness, and loop-safe route
  selection.

## D-024: Full-app persistence advances through explicit Room migrations

- **Status:** Accepted
- **Choice:** Advance the imported schema to version 4 through explicit
  1→2→3→4 migrations, adding course progress and observed peer hops without
  destructive fallback.
- **Why:** Existing testers must keep their identity-adjacent mesh history and
  course progress across upgrades.
- **Consequence:** Every future schema change must include migration tests and
  must not use destructive migration in production builds.

## D-018: Full-app navigation and topology interpretation

- **Status:** Accepted
- **Choice:** Use Home, Courses, Connect, and Alerts bottom tabs. Interpret Map
  as an offline mesh-topology view, not geographic tiles.
- **Why:** It remains useful offline and exposes real mesh state without an
  undeclared map/geocoding dependency.

## D-019: SOS broadcasts before refinement

- **Status:** Accepted
- **Choice:** The primary SOS action immediately emits a generic signed alert.
  Category, note, voice text, and optional coordinates are follow-up refinements
  referencing the original packet.
- **Why:** Emergency time-to-signal is more important than mandatory metadata.

## D-020: SOS location is explicit opt-in

- **Status:** Accepted
- **Choice:** Raw GPS attachment defaults off. No reverse geocoding or map is
  required. Permission is requested only when the user enables attachment.
- **Why:** This minimizes permission friction and unintended location disclosure.

## D-021: Flood integration is contract-first and disabled by default

- **Status:** Accepted
- **Choice:** Build typed/fake gateway plumbing and reviewed guidance templates,
  but do not call or advertise the real model until its retrained endpoint and
  response contract are verified.
- **Why:** UI work should not depend on the teammate, and unverified model claims
  must not enter an emergency product or demo.

## D-001: Nearby Connections is the v1 real transport

- **Status:** Accepted
- **Choice:** Google Nearby Connections using `Strategy.P2P_CLUSTER`.
- **Why:** The goal is a reliable offline two-phone vertical slice on a first
  Android project. Nearby removes most raw GATT role/connection complexity and
  its cluster topology permits devices to accept and initiate multiple peer
  connections, which is compatible with application-level relay.
- **Consequence:** The app works without internet or cellular service, but it is
  not technically BLE-only. Play Services may select BLE, Bluetooth Classic, or
  Wi-Fi mechanisms. Product language must say "offline nearby mesh" unless raw
  BLE is later implemented and selected.
- **Escape hatch:** `PacketRouter` remains transport-neutral so a raw
  `BlePacketRouter` can replace `NearbyPacketRouter` without changing core logic.

## D-002: v1 chat is broadcast, not addressed direct messaging

- **Status:** Accepted
- **Choice:** Every valid unseen text packet is displayed locally and relayed
  while its TTL remains positive.
- **Why:** The PRD packet has no recipient or room identifier. Adding private or
  addressed messaging would introduce identity, discovery, and UX decisions that
  are outside the current proof-of-concept.
- **Consequence:** UI wording and tests must describe a shared nearby channel.

## D-003: Signed content is immutable; TTL is bounded routing metadata

- **Status:** Accepted
- **Choice:** Sign a canonical immutable body containing protocol version,
  packet ID, type, sender ID, payload, creation time, and original TTL. Carry the
  current remaining TTL outside that signature. Reject values outside the local
  protocol maximum and decrement before relay.
- **Why:** A relay must change remaining TTL, which would invalidate an
  origin-only signature if TTL were included as a mutable signed field.
- **Consequence:** The original content is tamper-evident, but a malicious relay
  can alter remaining TTL. A hard local cap plus UUID dedup bounds normal loops;
  cryptographically enforced hop paths are deliberately out of v1 scope.

## D-004: Identity is a key fingerprint, not a trusted real-world identity

- **Status:** Accepted
- **Choice:** Generate an ECDSA P-256 signing key in Android Keystore. Derive
  `senderId` from SHA-256 of the encoded public key. Include the public key with
  packets; verify its fingerprint and the signature before accepting a packet.
- **Why:** This prevents content tampering and casual impersonation of an
  already-seen pseudonymous device without requiring a server or certificate
  authority.
- **Consequence:** Anyone can still join with a new key. Signatures alone do not
  provide authorization, Sybil resistance, or spam prevention. Payload limits,
  per-peer rate limits, and connection limits are also required.

## D-005: Android project baseline

- **Status:** Accepted
- **Choice:** Kotlin, Jetpack Compose, coroutines/Flow, Room, single app module
  initially; minimum SDK 26 and compile/target SDK 36 using the installed SDK.
- **Why:** This is a modern, testable stack; API 26 comfortably covers likely
  OS versions of both test phones while avoiding unnecessary legacy complexity.
- **Verification required:** Exact device API levels must be read with `adb`.
  If either phone is below API 26, lower `minSdk` before device installation.
- **Consequence:** Package boundaries will be designed for extraction, but an
  Android library module is deferred until behavior is proven (PRD Phase 6).

## D-006: No automatic connection acceptance without user-visible verification

- **Status:** Accepted
- **Choice:** Show Nearby's authentication digits on both phones and require the
  user to accept matching codes for first-time connections. Remembering trusted
  peers may be added later.
- **Why:** Blind auto-accept enables an arbitrary nearby device to connect and
  inject traffic before packet-level safeguards apply.

## D-007: Receipt order is the v1 display order; wall time is presentation only

- **Status:** Accepted
- **Choice:** Store and render messages in the local device's durable receipt
  order (`receivedAtMs`), and show that local receipt time to the user. Do not
  order the shared channel by an arbitrary sender's wall clock.
- **Why:** There is no trusted clock server in an offline mesh. A phone with an
  incorrect time can otherwise place a newly received message before messages
  already shown. A distributed system cannot establish a meaningful global order
  for truly concurrent events merely from independently-set wall clocks.
- **Consequence:** Different devices may render concurrent broadcast events in
  different orders. The next protocol revision will carry a hybrid logical
  clock for causal ordering; it will not pretend to make unrelated concurrent
  messages objectively ordered.

## D-008: Direct delivery and delivery ticks require an application protocol

- **Status:** Implemented in protocol v2; physical verification pending
- **Choice:** Keep the current shared broadcast channel as the Phase 3 vertical
  slice. Introduce addressed messages in protocol v2 with an immutable recipient
  key fingerprint, durable outbox, deduplicated relay, and a signed delivery
  acknowledgement referencing the original packet ID.
- **Why:** A Nearby endpoint ID is temporary transport state, not a stable user
  identity. A successful Nearby send only proves hand-off to the transport; it
  does not prove that the intended phone stored the message. A double tick must
  therefore be driven by the recipient's signed acknowledgement, not by a UI
  guess.
- **Consequence:** `✓ queued` means locally persisted. `✓✓ delivered` will be
  shown only after the sender receives the signed acknowledgement. Relays can
  route addressed packets but must not display messages intended for another
  identity. This is direct routing, not confidentiality: relays can still read
  plaintext until end-to-end encryption is added.

## D-009: One-tap network join after explicit network bootstrap

- **Status:** Accepted
- **Choice:** A device gets a user-chosen display name for nearby prompts, but
  the name is not trusted identity. The first join to a named community mesh
  will use an explicit bootstrap (QR/invite or verified short authentication
  string) to establish a network membership credential. After that, the user
  taps `Join network` once and the foreground service automatically accepts and
  reconnects only to valid members of that same network.
- **Why:** Per-peer prompts are unusable under disaster conditions. Blindly
  auto-accepting every nearby device makes an open mesh trivially spammed and
  permits relay resource exhaustion. Membership verification preserves an
  operational one-tap flow without treating a mutable device name as security.
- **Consequence:** The current confirmation screen remains the temporary v1
  bootstrap. An explicit `open public broadcast` mode may later auto-join, but
  must be visibly marked untrusted and use strict payload/rate/connection caps.

## D-010: Recover transient Nearby failures without app restart

- **Status:** Accepted
- **Choice:** Treat endpoint I/O errors, disconnections, failed requests, and
  failed byte-payload transfers as transient by default. Remove stale endpoint
  state, queue packets in memory, restart advertising/discovery after bounded
  exponential backoff with jitter, and flush queued packets when a connection
  succeeds. Escalate to a full endpoint reset after three consecutive failures.
- **Why:** Nearby radio connections are inherently transient, especially across
  different OEM Bluetooth stacks. Requiring the user to force-stop/reopen the
  app would defeat the point of a disaster mesh.
- **Consequence:** This first recovery layer survives session/radio errors while
  the process remains alive. It does not yet survive process death: protocol v2
  must replace the in-memory queue with a Room-backed durable outbox.

## D-011: Elect one Nearby connection initiator per named pair

- **Status:** Accepted
- **Choice:** Both devices advertise and discover, but only the endpoint whose
  distinct human-readable name sorts first starts `requestConnection`. Pending
  recovery is cancelled as soon as a valid connection prompt begins or a
  connection succeeds.
- **Why:** Simultaneous post-disconnect requests created overlapping connection
  prompts and allowed a delayed retry to restart discovery after a connection
  had succeeded. That produced the observed retry/pairing loop.
- **Consequence:** Devices must have distinct display names in the current v1
  prototype. Protocol v2 will replace this temporary name-based election with
  a persistent cryptographic identity, so duplicate human labels remain usable.

## D-012: Stable identity drives reconnect trust and connection election

- **Status:** Accepted
- **Choice:** Advertise `human name | signing-key fingerprint`. Use the stable
  fingerprint, not the human name or transient Nearby endpoint ID, to elect one
  connection requester and remember a manually verified peer. After the first
  matching-code approval, subsequent radio reconnections auto-accept that
  fingerprint. A signed peer announcement validates the identity at packet level.
- **Why:** Nearby endpoint IDs and authentication codes change by session, while
  names can collide or change. Requiring manual confirmation after every radio
  interruption is unusable; trusting a name alone is unsafe.
- **Consequence:** First contact still requires matching-code verification. The
  current pre-payload auto-accept relies on the advertised fingerprint claim;
  signed packets prove it after connection. A future network-membership invite
  should authenticate before consuming a connection slot.

## D-013: Protocol v2 provides community and addressed private conversations

- **Status:** Accepted; automated tests pass, physical three-phone test pending
- **Choice:** Signed packet bodies now include sender display name, optional
  recipient fingerprint, channel, and optional referenced packet ID. Community
  messages have no recipient and display on every node. Private messages are
  relayed by the mesh but displayed only by sender and intended recipient. The
  recipient emits a signed delivery acknowledgement, producing a real double tick.
- **Why:** Temporary Nearby endpoints cannot identify a person across reconnects
  or hops. Addressing and acknowledgements belong above the transport boundary.
- **Consequence:** This is routing privacy, not encryption: relay devices do not
  display private packets but could inspect plaintext. End-to-end encryption is
  still out of scope. Community delivery has no global all-members receipt.

## D-014: Recover one failed neighbor without resetting healthy mesh links

- **Status:** Accepted; automated verification passed, three-phone retest pending
- **Choice:** Track discovered, requested, and connected Nearby endpoints
  independently. Retry a failed elected neighbor with per-endpoint backoff while
  preserving all healthy connections and continuing discovery/advertising.
  Perform session-wide recovery only when the device has no usable peer.
- **Why:** The first three-phone attempt exposed a two-node lock: after one link
  was healthy, a failed attempt to add the third phone was suppressed by the
  session recovery guard and was never retried.
- **Consequence:** A temporary failure adding phone C no longer disconnects A-B.
  Signed peer announcements also relay with normal TTL so a peer connected via
  one bridge becomes selectable for private messaging throughout the mesh.

## D-015: The foreground service owns the mesh runtime

- **Status:** Accepted; multi-minute physical screen-off delivery passed
- **Choice:** A process-level `MeshRuntime` owns Room, identity, coordinator and
  transports. `MeshForegroundService` starts/stops Nearby; `MainActivity` only
  renders state and sends commands. After connecting, discovery changes from
  continuous scanning to 6-second windows separated by 20-second idle periods.
  Recovery/startup may hold a non-reference-counted partial wake lock for at
  most 45 seconds; a live connection never holds it permanently.
- **Why:** The earlier service only displayed a notification while the actual
  router belonged to the activity, so “relay active” did not prove background
  connectivity. Continuous discovery also increases radio contention and the
  risk of breaking established Nearby connections.
- **Consequence:** UI/background transitions no longer stop routing. Process
  death still requires the pending Room-backed durable outbox to preserve
  unsent packets; OEM battery onboarding and the unplugged 10/30-minute tests
  remain mandatory.

## D-016: A healthy endpoint is authoritative over transient scan/send errors

- **Status:** Accepted
- **Choice:** While at least one Nearby endpoint remains connected, discovery or
  advertising failures must not replace the UI with a whole-session
  `Connection interrupted` warning. A payload-send failure retries that payload
  without immediately tearing down the endpoint. The connected-peer count is
  the authoritative user-facing state until Nearby reports endpoint loss.
- **Why:** Physical testing showed encrypted keep-alives and instant message
  delivery while a stale recovery countdown was displayed. Discovery is for
  finding additional neighbors; its transient failure does not mean an existing
  encrypted link has failed.
- **Consequence:** The banner now reports the live peer count during healthy
  links and recovery remains scoped to the failed operation. Actual endpoint
  loss still enters bounded retry.

## D-017: Permission requirements follow physical Play Services enforcement

- **Status:** Accepted; Android 15 startup retest pending
- **Choice:** Declare Wi-Fi state permissions without an SDK cap and request
  both approximate and precise Location alongside Nearby Devices on every
  supported Android version. Treat Nearby status 8032, 8034 and 8036 as
  permanent setup errors with explicit UI text rather than transient recovery.
- **Why:** USB logs from the Galaxy A17 on Android 15 with Play Services Nearby
  19.2 first reported missing Wi-Fi state (8032), then coarse Location (8034),
  then fine Location (8036). All three calls occurred before discovery could
  start. The previous manifest capped these permissions at Android 12, so the
  runtime request could never present Location on that phone.
- **Consequence:** First launch may show both Nearby Devices and Location on
  modern Android. This is broader than current generic Android guidance but is
  required by the actual supported device/library combination. A permanent
  setup failure no longer produces an endless 4/2-second retry loop.
