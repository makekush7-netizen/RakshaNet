# RakshaNet Full App Progress

Last updated: 2026-08-17

## Current state

- V2 Android UI is implemented in the approved light direction: Home,
  Community, Learn, and Alerts, with mesh diagnostics moved out of primary
  navigation. The existing mesh coordinator/transport protocol was preserved.
- Four original project-bound images were generated and optimized: flood
  preparedness, active flood response, earthquake safety, and cyclone
  preparation. Flood imagery is used in playable screens; Earthquake and
  Cyclone are polished `Coming soon` course previews.
- Flood Readiness now contains four lessons and five scenario questions. The
  course journey, lesson cards, feedback, source notes, progress persistence,
  and Ambassador preview are visually reworked.
- Website mock is implemented and privately published at
  `https://rakshanet-demo.makewatch7.chatgpt.site`. It includes the real APK
  download, interactive simulated risk selection, an authority-room preview,
  and prominent disclosure that privileged publishing/AI/cloud bridging is not
  yet operational.
- Android version is `0.3.0-demo`. `:app:assembleDebug` passed on 2026-08-17.
  Artifact: `artifacts/RakshaNet-v0.3-demo-debug.apk`, 20,829,692 bytes,
  SHA-256 `D6305AEEDA525ADFB46181CEE11B7694DCD31692CE6439BEE67335C95B3C58F1`.

- The previous screen flow and visual direction have been rejected; no further
  UI implementation should extend them.
- V2 product, architecture, repository, cloud gateway, authority, AI-review,
  and demo flows are now specified in `PRODUCT_BLUEPRINT_V2.md`,
  `SYSTEM_ARCHITECTURE_V2.md`, and `DEMO_RUNBOOK.md`.
- V2 primary navigation is Home, Community, Learn, and Alerts. Mesh status moves
  to a global health control instead of a primary Connect tab.
- Remote `origin/main` was fetched through commit `5d531fc`. It adds a complete
  `flood_prediction_service/` with FastAPI, Docker, a model artifact, simulated
  feed, audit/model documentation, and tests. It has not been merged because
  two local connected-status fixes are intentionally preserved.
- The fetched model uses 118 annual Kerala observations. Its actual `/predict`
  contract requires `rainfall_mm` and returns probability, threshold, risk tier,
  recommendation, and optional alert ID. It will be presented as labelled
  historical-risk decision support, not real-time forecasting.

- Empty GitHub repository cloned to the requested `raksha net` folder.
- Verified mesh working tree imported with protocol v2, Room, foreground-service
  runtime, recovery, private/community UI, tests, and research.
- New PRD installed as `PRD.md`; original mesh PRD archived under `docs/`;
  flood contract installed.
- Product choices locked: topology view, immediate SOS, opt-in coordinates.
- Full-app plan and test matrix created and maintained as the delivery ledger.
- Navy/teal design system, four-tab shell, live Home dashboard, profile naming,
  and the restyled Connect experience are implemented.
- Offline Flood Readiness lessons, quiz, and Room-backed progress are implemented.
- Signed SOS/SOS-update/guidance packets, priority Alerts feed, optional offline
  speech input, and opt-in raw coordinates are implemented.
- Known-peer topology with honest TTL-derived observed-hop hints is implemented.
- Typed flood gateway, deterministic fake, reviewed templates, and risk-change
  dedup are implemented; the real service remains disabled.
- Community receipt labels now have distinct evidence: local Room persistence,
  confirmed direct-neighbor hand-off, and a signed acknowledgement emitted only
  when a receiving phone renders the message bubble.
- Locally queued packets are reloaded from Room and safely re-offered to the
  deduplicating mesh after process restart.

## Preserved baseline

- Three phones admitted; Community/private targeting and signed private ACK pass.
- Trusted reconnect, leave/rejoin, Android 15 permissions, and multi-minute
  screen-off persistence pass.
- Latest prior suite: 30 JVM tests, debug build and lint clean.

## Current phase and automated evidence

- Phase A is complete.
- Phases B-G have working implementation and automated evidence, but their
  physical-device/manual exit criteria remain open where noted in the matrix.
- On 2026-08-17, `:app:testDebugUnitTest :app:assembleDebug :app:lintDebug`
  passed with 45 JVM tests, a debug APK, and clean lint.
- Debug APK SHA-256:
  `1E22D0B29DE4173ACCEC1C9F885C7AC53E33309048348A9CD50436EA3ED758B7`
  (13,116,792 bytes). Recompute after any source change.
- Database schema is version 4 with explicit 1→2→3→4 migrations preserving
  messages/peers while adding course progress and observed-hop state.

## External blockers

- Real flood endpoint/retrain is not supplied; use a fake adapter.
- True out-of-range A→B→C range test remains user hardware work.
- No phones were visible to ADB during this implementation session, so the new
  shell, SOS, GPS, voice, course persistence, and topology UI are not yet
  physically verified. Prior three-phone mesh evidence remains preserved.

## Immediate next action

Deploy the production-built `web/` project from GitHub to Vercel with the
repository root configured as `web/`, then verify the public APK link downloads
the exact v0.3.1 artifact.

## 2026-08-17 device review and public-release pass

- User reports chat, lesson flow, and quiz work correctly on the Redmi Note 10
  Pro and Galaxy J8.
- Screenshots exposed invisible labels on dark Material buttons. Explicit white
  label colors were applied to every affected dark action button; the corrected
  Android APK assembles successfully. Device confirmation of the visual repair
  is still pending.
- Corrected artifact: `artifacts/RakshaNet-v0.3.1-ui-fix-debug.apk`, 21,027,103
  bytes, SHA-256
  `D95DCD6C6EF5994F8006C3D941EEF93EE23D50C502398207F6CD59A145446A58`.
- The website was converted from the private Sites/vinext configuration to a
  standard Next.js 16.2.6 Vercel build. `npm run build` passes.
- Website download buttons now serve `RakshaNet-v0.3.1-ui-fix.apk`.
