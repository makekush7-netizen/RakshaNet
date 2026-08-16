# RakshaNet — Final Product PRD

**Status:** Consolidation of prior work. The mesh/chat core described in §3 is
**already built and verified on three real phones** (see
`HANDOFF_TO_CLAUDE.md` in the project history) — treat that as ground truth
to preserve, not to rebuild. This doc's job is to specify the visual design
system, the two new features (Courses, Alerts), and how they integrate with
the existing mesh core into one cohesive app.

---

## 0. Context for whoever/whatever builds this

Same builder context as before: 2nd-year B.Tech student, first Android app,
no mentor, AI-assisted ("vibe coder" with real understanding, not blind
copy-paste), hackathon deadline, has Codex Pro. Prior phases already
shipped: a working offline BLE/Nearby mesh with signed packets, TTL relay,
Room storage, and a foreground service — verified on a Redmi Note 10 Pro,
Galaxy J8, and Galaxy A17. A separate teammate is building a flood-risk
classifier service (contract in `HANDOFF_FLOOD_MODEL.md`) — as of last
check, that service's dataset is being corrected (it was training on
self-generated synthetic data; switching to a real-world CSV your teammate
found) and its accuracy claims should not be treated as final until that
retrain lands. Don't block UI work on that service being finished — build
against the contract, integrate the real thing when ready.

**Advisor stance carried into this doc:** flag inconsistencies rather than
silently picking one; state assumptions explicitly rather than guessing
quietly; keep scope realistic for the remaining time.

---

## 1. Design system

Derived from the approved mockups. Codex should treat this as the visual
source of truth for new screens, and — time permitting — restyle the
existing mesh/chat UI to match, without touching the underlying logic.

### Palette
| Role | Approx color | Use |
|---|---|---|
| Primary navy | `#0B3D66`-ish deep blue | Logo, headings, primary text on light bg |
| Teal accent | `#0F6E5C`-ish | Courses/Connect accents, progress, success states |
| Danger red | `#C62E27`-ish | SOS only — reserve red exclusively for emergency, don't dilute it elsewhere |
| Background | very light blue-gray (`#EAF1F5`-ish) | App background |
| Card surfaces | white / very light tint per section (light blue for Courses card, light teal for Connect card) | Home nav cards |

**Rule: red means SOS/emergency, nowhere else in the app.** If red starts
showing up on non-emergency UI, that undermines the one place it needs to
carry urgency.

### Typography & shape
- Rounded, friendly sans-serif for headings (bold), regular weight for body.
- Large corner radii on cards (looks soft/approachable, appropriate for an
  app used under stress).
- Subtle decorative background motifs per section (wave silhouette behind
  SOS/flood content, mountain silhouette behind Connect) — low-opacity,
  non-interactive, purely tonal. Don't let these become a performance or
  asset-management burden; simple flat SVG shapes, not photographic assets.

### Logo
- Shield outline containing a stylized wave — represents both "protection"
  and "flood," works as a general disaster-readiness mark without being
  flood-specific only. No mascot character — deliberately dropped in favor
  of a clean icon system (see §4.2 for why).

### Iconography
- Simple filled/line icons (graduation cap for Courses, people icon for
  Connect, triangle-alert for SOS/Alerts, lock for locked course modules).
  No custom mascot illustration needed for MVP — faster to build, and avoids
  any resemblance-to-existing-IP risk that a Duolingo-style character
  brought up earlier.

---

## 2. Navigation structure (resolving the mockup inconsistency)

**Assumption made here — confirm before Codex builds it:** unify on a
single 4-tab bottom nav:

```
Home · Courses · Connect · Alerts
```

- **Profile/settings** lives behind a header icon (hamburger menu, top
  right — as shown in the Courses mockup), not a 5th bottom tab. This is
  where local-only name/device settings live (no login — see §5).
- **"Map" from the first mockup is reinterpreted as scope-appropriate:**
  not a geographic map (needs network tiles/geocoding, explicitly out of
  scope), but an **offline mesh topology view** — which peers are
  connected, roughly how many hops away, connection health. This is
  genuinely useful, buildable with zero network dependency, and reuses data
  already tracked by `MeshCoordinator`. If a literal geographic map was
  actually wanted, that needs a different, explicit decision — flag this
  back before building either version.
- Home stays as a dashboard with large nav cards (as in the first mockup),
  rather than being folded into the tab bar — it's the natural landing
  screen and mesh-status readout.

---

## 3. Existing mesh/chat core — preserve, restyle only

Do not rebuild. Reference `HANDOFF_TO_CLAUDE.md` for full architecture
(`PacketRouter` / `SelectablePacketRouter` / `MeshCoordinator`, Room schema,
Android Keystore signing identity, foreground service). Restyle the Connect
screen to match §1's palette and the message-bubble layout in the approved
mockup, without altering:
- The `PacketRouter` boundary.
- TTL/dedup relay logic.
- Signed packet validation.

### Refinement to delivery receipts (compatible with existing design, not a
change to it)
The existing architecture already distinguishes:
- **Community mode:** single `✓ mesh` tick — "injected into the mesh,"
  deliberately not claiming group-wide acknowledgement (an open, changing
  group can't honestly be summarized as "seen by everyone").
- **Private mode:** `✓ queued` → `✓✓ delivered` on signed recipient ACK.

The new chat mockup's three-tier receipt row (**Sent** — stored locally,
**Delivered** — to a peer, **Seen** — at least one peer) is compatible with
this, not a contradiction, because it scopes "Seen" to *at least one peer*
rather than *the whole group*. Adopt this three-tier language for the
Community UI specifically:
- **Sent** = written to local Room store.
- **Delivered** = successfully handed to at least one direct mesh neighbor.
- **Seen** = a signed "peer received and displayed" acknowledgement came
  back from at least one peer (not all — never imply full-group
  confirmation).

This is a UI/label refinement on top of existing signed-ack plumbing, not a
new subsystem.

---

## 4. New feature — Courses (training module)

### 4.1 Structure
- Course Dashboard screen: vertical node chain (as in the approved mockup),
  each node = one course module.
- MVP ships **one real module: Flood Readiness**, 1-2 lessons only. Other
  modules (Earthquake, Storm) render as locked nodes with a "Coming soon"
  badge — visible in the tree for completeness/roadmap signaling, not
  functional.
- Progress bar per unlocked module (e.g. "60% complete") backed by local
  lesson-completion state.
- Node connectors: solid + checkmark for completed, dashed + lock icon for
  locked — matches the approved mockup exactly.

### 4.2 Content delivery — no mascot for MVP
Earlier direction considered a Duolingo-style 2D mascot. **Recommendation,
given the approved mockups: drop it for MVP.** The approved design already
reads as warm and approachable through color/shape/copy alone, without a
character — this removes both the IP-adjacent risk (Duolingo's specific
characters were the original inspiration, not something to visually track
close to) and a real chunk of asset-production time. If there's spare time
after core features are solid, a simple original static-pose icon (not a
full animated mascot) could be layered in later — not a blocker for launch.

### 4.3 Lesson format (keep simple)
- Card-based lesson screens: short text/illustration + a multiple-choice
  or tap-to-continue interaction per screen, ending in a short quiz (3-5
  questions) to mark the module complete.
- All content and progress stored **locally only** (Room), no login, no
  cloud sync for MVP — consistent with the no-auth decision in §5. Content
  itself (lesson text/images) can be bundled in the app or a local JSON
  asset — no reason to fetch it over network.
- Ambassador-level unlock: show the concept in the UI (e.g. a distinct
  bubble/badge further down the tree) but **do not implement the actual
  unlock logic or permissions** for this round — visual placeholder only,
  as previously agreed.

---

## 5. New feature — Alerts / SOS

### 5.1 Login/auth — confirmed skip
No account system. Local-only profile (display name, generated on first
launch) — same identity model the mesh core already uses via Android
Keystore. An emergency app should never gate the SOS flow behind sign-in.

### 5.2 SOS flow — assumption stated, confirm before building
**Default assumed here: tapping SOS immediately broadcasts a generic
"need help" signal; category and note are optional, addable in the same
moment or as a fast follow-up** — not required before the broadcast fires.
Rationale: in a real panic moment, minimizing taps-before-broadcast matters
more than getting the category exactly right up front. If you'd rather
require category selection first, say so explicitly — it changes the
interaction flow, not just the visuals.

Flow:
1. Tap SOS (from Home card or Alerts tab).
2. Signal broadcasts immediately as a signed `SOS_ALERT` packet (generic
   category by default).
3. Category chips (flood / earthquake / medical / generic) and a note field
   remain available to refine/add context — refining sends an updated
   packet referencing the original (via the existing `referenceId` field
   already in the signed packet schema).
4. Note field supports typed text or Android on-device speech-to-text
   (`RecognizerIntent`, prefer offline model via `EXTRA_PREFER_OFFLINE` so
   it doesn't silently fail without connectivity).
5. Optional **raw GPS coordinate attach** (toggle, off by default or on —
   decide during build): plain GPS works fully offline via
   `FusedLocationProviderClient`, no network needed for the coordinate
   itself. Do not attempt reverse-geocoding or map display — both need
   network and are out of scope. Attach as raw `lat/lng` in the packet
   payload; a responder can plug that into any map tool once they're back
   online.

### 5.3 SOS_ALERT packet type
New signed packet type, following the existing packet model
(`MeshCoordinator` already handles TTL/dedup/signing — this is a new
`type` value, not new infrastructure):
```json
{
  "id": "uuid",
  "type": "SOS_ALERT",
  "sender_id": "device-identity",
  "category": "GENERIC | FLOOD | EARTHQUAKE | MEDICAL",
  "note": "string, optional",
  "location": { "lat": 0.0, "lng": 0.0 },
  "ttl": 7,
  "timestamp": 1234567890,
  "referenceId": "uuid, optional — links a refinement to the original alert"
}
```
Community-broadcast by default (same relay/dedup rules as chat messages) —
an SOS is exactly the kind of message that should reach the whole mesh, not
be limited to a private recipient.

### 5.4 Alerts tab — dual purpose
The Alerts tab shows both:
- **Outgoing:** the SOS trigger UI (§5.2).
- **Incoming:** a feed of SOS alerts and `GUIDANCE_BROADCAST` messages
  (see §6) received from the mesh, distinct from the general Connect chat
  feed — these are structured, higher-priority, and shouldn't get lost in
  a scrolling conversation.

---

## 6. Flood prediction integration (bridges to the separate ML service)

- Consumes the contract in `HANDOFF_FLOOD_MODEL.md`: `POST /predict`
  returning `risk_level: LOW | MODERATE | SEVERE` per region.
- **As of this doc, that service's training data is being corrected** —
  don't hard-block UI/integration work on it, but don't finalize demo
  numbers/claims from it until the retrain (on the real dataset, not
  self-generated synthetic data) is confirmed done.
- A gateway-node component (small, not a general backend — see prior
  discussion) periodically checks the endpoint when online and, on a
  risk-level change, packages a **pre-written, human-authored** guidance
  message (not a live LLM call — the mesh must work fully offline) into a
  new `GUIDANCE_BROADCAST` signed packet type, injected into the existing
  mesh via `MeshCoordinator` like any other packet.
- Guidance templates: write a small library ahead of time, one per
  hazard/severity pair (flood-moderate, flood-severe, etc.) — calm,
  reviewed text, not generated under demo pressure.

```json
{
  "id": "uuid",
  "type": "GUIDANCE_BROADCAST",
  "sender_id": "gateway-device-identity",
  "hazard": "FLOOD",
  "severity": "SEVERE",
  "message": "pre-written template text",
  "ttl": 7,
  "timestamp": 1234567890
}
```

---

## 7. Build order for Codex

1. **Design system pass:** implement the color/type/shape tokens from §1 as
   reusable Compose theme values — do this once, first, so every screen
   after it is consistent.
2. **Home dashboard:** static nav cards + live mesh-status pill (reuses
   existing `MeshCoordinator` peer count) — low risk, establishes the
   visual anchor.
3. **Restyle Connect/chat** to match §1 and §3's receipt refinement —
   logic untouched, visuals only.
4. **Courses:** dashboard + Flood Readiness module (1-2 lessons + quiz),
   locked placeholder nodes for Earthquake/Storm.
5. **Alerts/SOS:** trigger flow (§5.2-5.3) + incoming feed (§5.4).
6. **Mesh topology view** (the reinterpreted "Map" tab) — lowest priority,
   build only once 2-5 are solid and stable.
7. **Flood-service integration (§6)** — build the gateway glue and
   `GUIDANCE_BROADCAST` handling once the teammate's retrained endpoint is
   confirmed ready; don't block other work waiting on it.

---

## 8. Open items requiring your confirmation before/during build

- Is "Map" meant as offline mesh topology (assumed here) or an actual
  geographic map? These are very different builds.
- Confirm the SOS-tap flow assumption in §5.2 (instant broadcast, refine
  after) vs. requiring category selection first.
- Confirm whether raw-location attach on SOS is on-by-default or an
  explicit opt-in toggle.
- Confirm once the flood-risk model has been retrained on the real dataset
  (§0, §6) — don't present old 99.83%-accuracy figures at demo time.
