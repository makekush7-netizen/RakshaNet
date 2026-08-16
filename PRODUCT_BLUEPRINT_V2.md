# RakshaNet V2 Product Blueprint

Last updated: 2026-08-17

## Product statement

RakshaNet helps a neighbourhood prepare before a disaster, coordinate during
one, and carry trusted information across phones when internet access is weak
or absent. The verified nearby mesh is the delivery layer, not the product the
user must operate.

This blueprint supersedes the existing PRD's screen flow and visual direction.
It preserves the packet protocol, routing, storage, signing, foreground service,
trusted reconnect, Android permission fixes, and physical evidence.

## Demo promise

The hackathon demonstration will prove one complete story:

1. An authority starts a clearly labelled **FLOOD DRILL** on the website.
2. One internet-connected phone receives the signed incident event.
3. That phone injects the event into the offline nearby mesh.
4. Another phone receives it without internet and can submit a field update.
5. The connected phone returns mesh updates to the control plane.
6. An AI-assisted situation report groups facts, needs, and contradictions.
7. An authority reviews the report and broadcasts an instruction.
8. Both online and offline phones receive the approved instruction.

The demo must never imply that the current annual Kerala rainfall classifier is
an intraday forecasting system. It is a regional historical-risk demonstration
with explicit limitations.

## Experience principles

- **Situation first:** show what is happening and what the user can do. Never
  lead with transport names, TTL, retry timers, or protocol details.
- **Works before accounts:** local identity and nearby communication remain
  useful without sign-in.
- **Preparedness feels calm:** warm light surfaces, restrained green/yellow
  accents, concise writing, and useful imagery.
- **Emergency is unmistakable:** red-orange is reserved for SOS and critical
  official alerts.
- **Trust is visible:** Authority, Ambassador, Community, and Drill content have
  visibly different labels. Names alone are not trusted identity.
- **One action per card:** cards explain one decision and have one obvious next
  action.
- **No false certainty:** receipts, AI summaries, model risk, mesh reach, and
  peer state say exactly what has been observed.

## Navigation

The primary navigation becomes:

`Home · Community · Learn · Alerts`

“Connect” is removed from the bottom navigation. A small network-health control
in the header opens a sheet containing peer count, online/offline gateway state,
joined network, permissions, and diagnostics. Users should not have to visit a
transport screen before communicating.

## First-run flow

### 1. Welcome

- RakshaNet shield/wave mark.
- One sentence: “Prepare together. Stay connected when networks fail.”
- Two concise illustrated panels: prepare offline; relay trusted updates.

### 2. Local identity

- Choose a display name and area label.
- Explain that identity is stored on this phone.
- Generate the existing signing identity silently.

### 3. Join a community

- Primary: scan an invite QR / enter a short community code.
- Demo: join the visibly labelled `RakshaNet Demo Network`.
- Existing trusted-peer confirmation remains available as a fallback, not the
  normal disaster flow.

### 4. Readiness setup

- Request required Nearby/Location permissions with a plain-language reason.
- Start the relay foreground service.
- Show a three-item readiness checklist, not Android error codes.

## Home

Home has two modes using the same layout.

### Preparedness mode

- Hero card with a restrained flood-readiness photograph or editorial
  illustration and the next useful lesson.
- Readiness progress: lessons completed, offline guide available, network ready.
- “Nearby network” pill: ready, degraded, or action required.
- Compact cards for Community activity and downloaded emergency guidance.
- Prominent but visually contained SOS action.

### Active-incident mode

- Hero becomes an incident card: `FLOOD DRILL`, severity, affected area,
  official update time, and `Open live situation`.
- Three immediate actions: `I am safe`, `Share an update`, `Request help`.
- Latest approved authority instruction pinned under the incident.
- Network pill tells the truth: online gateway, nearby relay, or stored locally.

## Community

Community is an incident room, not a generic chat dump.

- Pinned authority bulletin at the top.
- Filter chips: All, Official, Field updates, Requests, Offers.
- Structured composer: text plus optional update type and coarse location.
- Sender trust label: Authority, Ambassador, Community member, or Unverified.
- Delivery labels use existing evidence: stored, reached a peer, acknowledged.
- Private conversation remains accessible from a member/peer detail sheet.
- Independent device clocks never decide global order; local receipt order is
  retained and official events carry server sequence numbers when online.

## Learn

The learning module uses a calm journey path inspired by the supplied reference,
without copying another product's mascot or visual identity.

- Alternating vertical nodes connected by a dotted/solid route.
- Chapters: Understand flood risk, Prepare your home, Act during flooding,
  Help your community, Ambassador assessment.
- Each chapter contains several short interactions: scenario choice, order the
  steps, spot the hazard, checklist, and quiz.
- Progress, streak-free XP, badges, and downloaded content remain local.
- Ambassador status is not granted by a cosmetic badge. The demo may show an
  `Ambassador candidate` journey; production verification requires an authority
  credential.
- Every safety lesson carries source metadata from NDRF, NDMA, WHO, or another
  named authoritative source.

## Alerts and SOS

- The Alerts tab separates Official advisories, SOS nearby, and My requests.
- SOS sends a generic signed alert immediately, then allows category, note,
  voice, and opt-in raw coordinates as a refinement.
- Critical actions use red-orange; warnings use amber; ordinary UI never uses
  emergency red.
- Drill alerts are visually and textually labelled `DRILL` on every screen and
  in every notification.

## Visual direction

### Palette

| Token | Value | Use |
|---|---|---|
| Canvas | `#F7F6F1` | Warm off-white background |
| Surface | `#FFFFFF` | Primary cards and sheets |
| Ink | `#171A18` | Headings and primary actions |
| Muted ink | `#66706A` | Secondary information |
| Raksha green | `#37A86B` | Progress, healthy mesh, primary positive action |
| Mint | `#DCEEDC` | Supporting cards and selected states |
| Sun | `#F2C94C` | Readiness highlights and warnings |
| Emergency | `#ED4B2F` | SOS and critical alerts only |
| Border | `#E3E6E1` | Quiet separators and card outlines |

### Composition

- Native Compose surfaces with 20–28 dp radii and quiet borders/shadows.
- Compact type hierarchy; avoid oversized headings and decorative all-caps.
- Near-black primary buttons; green for progress/confirmation.
- Flood imagery appears inside hero and lesson cards, never as a full-screen
  background behind text.
- Generate only restrained content illustrations or image cut-outs. Navigation,
  cards, icons, text, and controls remain native UI.
- Use one consistent outline icon family. Do not mix emoji, stock clip-art, and
  generated pseudo-icons.

### Motion

- Use native Compose motion for state changes: 180–240 ms fades/slides, a route
  drawing forward after lesson completion, and a restrained network pulse when
  an event is relayed.
- Incident activation may transition the Home hero and pin the new bulletin; it
  must not use flashing, shaking, or looping alarm animation.
- Respect Android's reduced-motion setting. Animation is feedback, never the
  only indication of severity, connectivity, or completion.
- Add Rive/Lottie only if a specific approved asset materially improves one
  interaction. Do not install an animation dependency merely to decorate cards.

## Explicit non-goals for the demo

- Autonomous emergency decisions or unreviewed AI broadcasts.
- Claims of government integration, production authority identity, or live
  sensor forecasting.
- Best-path mesh routing or guaranteed delivery to every community member.
- A full social network, account system, or public global chat.
- Geographic offline maps unless a separate, tested map package is approved.
