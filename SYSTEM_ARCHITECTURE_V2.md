# RakshaNet V2 System Architecture

Last updated: 2026-08-17

## Architecture outcome

RakshaNet becomes a small monorepo with four deployable parts and shared event
contracts. The existing Android mesh stays in place and is extended through its
current coordinator boundary.

```text
Authority website
      │ create drill / approve broadcast
      ▼
Control-plane API ─────────► Flood model service
      │ ordered cloud events       │ risk assessment
      │                            ▼
      │                    reviewed policy/template
      ▼
Internet-connected Android gateway
      │ signed cloud envelope → signed mesh packet
      ▼
Nearby mesh phones ↔ field updates / requests / acknowledgements
      │
      └──── gateway upload ───► AI-assisted incident summary
                                      │
                                      └── human approval required
```

## Proposed repository structure

```text
RakshaNet/
├── app/                         # Existing Android app; do not move yet
│   └── src/main/java/com/rakshanet/meshchat/
│       ├── core/                # Preserved protocol, crypto, routing, transport
│       ├── data/                # Room and repositories
│       ├── service/             # Foreground mesh runtime
│       ├── gateway/             # Cloud event sync adapter
│       ├── courses/             # Offline curriculum domain
│       ├── incidents/           # New incident/report domain
│       └── ui/                  # V2 feature screens and design system
├── web/                         # Public site + demo/authority console
│   ├── app/
│   │   ├── page.tsx             # Landing page
│   │   ├── download/page.tsx    # APK, QR, version, hash, install guide
│   │   ├── demo/page.tsx        # Labelled disaster simulator
│   │   └── authority/page.tsx   # Reports, AI draft, approve/broadcast
│   └── public/
├── services/
│   ├── control-plane/           # FastAPI incident/event/report API
│   └── flood-model/             # Friend's fetched prediction service
├── contracts/
│   ├── event-envelope.schema.json
│   ├── incident.schema.json
│   ├── field-report.schema.json
│   └── flood-prediction.schema.json
├── demo/
│   ├── scenarios/kerala-flood-drill.json
│   └── DEMO_RUNBOOK.md
├── docs/
│   ├── product/
│   ├── architecture/
│   ├── safety/
│   └── evidence/
└── infra/                       # Deployment manifests added only when selected
```

The remote ML work currently lives at `flood_prediction_service/`. After local
mesh changes are committed and the remote is integrated, it should be moved to
`services/flood-model/` in a dedicated mechanical commit so its history and
ownership remain obvious.

## Android boundaries

### Preserve unchanged

- `PacketRouter` and `SelectablePacketRouter`.
- `MeshCoordinator` validation, TTL, dedup, relay, addressing, and ACK logic.
- Room-backed receipt order and pending resend.
- Android Keystore signing identity.
- `MeshForegroundService` process ownership.
- Trusted reconnect and Android 15 permission handling.

### Add above the mesh core

- `IncidentRepository`: active incident, bulletins, field reports, requests.
- `CloudGateway`: cursor-based download/upload while internet is available.
- `CloudEnvelopeVerifier`: verifies an authority/control-plane signature before
  converting a cloud event into a mesh packet.
- `IncidentPacketMapper`: maps approved incident events to existing structured
  `GUIDANCE_BROADCAST` and new report/request packet payloads.
- `GatewayOutbox`: durable, idempotent uploads from mesh to cloud.
- V2 screens that consume domain state, never Nearby callbacks directly.

## Cloud event contract

Every cloud-originated event is append-only and ordered by the control plane.

```json
{
  "event_id": "uuid",
  "sequence": 42,
  "network_id": "rakshanet-demo",
  "incident_id": "kerala-flood-drill-01",
  "kind": "INCIDENT_STARTED | AUTHORITY_BROADCAST | INCIDENT_ENDED",
  "is_drill": true,
  "created_at": "2026-08-17T10:00:00Z",
  "payload": {},
  "authority_key_id": "demo-authority-v1",
  "signature": "base64"
}
```

- The Android gateway stores the last accepted sequence per network.
- UUID and sequence make polling/retry idempotent.
- A cloud event is never trusted merely because HTTPS returned it; the demo
  authority key is bundled/pinned and the signature is verified.
- Production key rotation and authority onboarding are later work.

## Demo synchronization strategy

For tomorrow's vertical slice, use cursor polling rather than making the demo
depend on a fragile long-lived mobile WebSocket:

- Android gateway polls `GET /v1/events?network_id=...&after_sequence=...` every
  3–5 seconds only while online.
- It immediately persists and injects new verified events into the mesh.
- It uploads durable reports using `POST /v1/reports`; duplicate IDs return the
  existing record.
- The website polls or uses server-sent events for visible dashboard updates.

WebSockets can be added later for latency; polling plus durable cursors is easier
to recover and demonstrate reliably.

## Control-plane API

Minimum endpoints:

| Method | Endpoint | Purpose |
|---|---|---|
| `POST` | `/v1/demo/incidents` | Start a labelled drill |
| `POST` | `/v1/incidents/{id}/broadcasts` | Create authority draft/broadcast |
| `POST` | `/v1/incidents/{id}/close` | End the incident |
| `GET` | `/v1/events` | Ordered gateway event sync |
| `POST` | `/v1/reports` | Idempotent mesh field-report upload |
| `GET` | `/v1/incidents/{id}/reports` | Authority situation feed |
| `POST` | `/v1/incidents/{id}/analysis` | Produce an AI summary draft |
| `GET` | `/health` | Deployment health |

The control plane owns incident state and authority signatures. The flood model
only returns risk evidence; it cannot broadcast directly.

## Flood model integration

Remote commit `5d531fc` contains a FastAPI service with Docker packaging, tests,
model artifacts, and a dashboard. Its actual request/response differs from the
older handoff contract:

- Request requires `rainfall_mm` and optionally monthly/seasonal fields.
- Response returns `flood_probability`, `threshold`, `risk_level`,
  `recommended_action`, and optional `alert_id`.
- The dataset is 118 annual Kerala observations (1901–2018).

The remote documents contain differing automated-test counts and assert dataset
provenance more strongly than has been independently verified in this workspace.
Before the pitch, run the fetched suite once and describe provenance as the
teammate/model card's claim unless a primary source chain is documented.

Integration rules:

1. Keep this service separately deployable.
2. The control plane calls `/predict`; Android does not call it directly.
3. Translate the response through a versioned adapter.
4. Label inputs `DEMO / SIMULATED TELEMETRY`.
5. Treat model output as decision support. A human approves public guidance.
6. Do not describe it as real-time flood prediction or guaranteed warning.

## AI-assisted situation analysis

Input is a bounded set of incident reports, not unrestricted chat history.
Output uses a strict schema:

```json
{
  "incident_id": "string",
  "summary": "string",
  "confirmed_facts": [],
  "urgent_needs": [],
  "locations": [],
  "contradictions": [],
  "unverified_claims": [],
  "recommended_questions": [],
  "source_report_ids": [],
  "generated_at": "ISO-8601"
}
```

- The summary is a draft and displays its source report IDs.
- No AI output directly triggers an emergency broadcast.
- Authority review/edit/approve is mandatory.
- If an LLM key is unavailable, a deterministic grouping/summarization adapter
  preserves the complete demo flow without pretending it is generative AI.

## Deployment recommendation

- `web/`: Vercel or another static/Next.js host.
- `services/control-plane/`: Render/Railway/Fly/Cloud Run with persistent data.
- `services/flood-model/`: Docker-capable host; its existing Dockerfile is the
  starting point.
- For the overnight demo, a single host may run both Python services, but they
  remain separate processes/contracts.
- APK is published as a versioned asset with SHA-256, size, min Android version,
  and a QR code on `/download`.

No deployment provider is locked until account availability and the actual
service runtime are checked.

## Safety and failure behavior

- All simulations say `DRILL` in payload, notification, Home, Community, and
  Authority UI.
- If cloud sync fails, mesh communication and offline lessons continue.
- If mesh has no peer, outbound reports remain stored and upload later.
- If AI analysis fails, raw reports remain visible and authority broadcast is
  still available.
- If flood inference fails, the simulator can use a named manual scenario; it
  must not fabricate a model result.
