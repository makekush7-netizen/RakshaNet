# RakshaNet Full App Progress

Last updated: 2026-08-16

## Current state

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

## Preserved baseline

- Three phones admitted; Community/private targeting and signed private ACK pass.
- Trusted reconnect, leave/rejoin, Android 15 permissions, and multi-minute
  screen-off persistence pass.
- Latest prior suite: 30 JVM tests, debug build and lint clean.

## Current phase and automated evidence

- Phase A is complete.
- Phases B-G have working implementation and automated evidence, but their
  physical-device/manual exit criteria remain open where noted in the matrix.
- On 2026-08-16, `:app:testDebugUnitTest :app:assembleDebug :app:lintDebug`
  passed with 42 JVM tests, a debug APK, and clean lint.
- Debug APK SHA-256:
  `C2040E9CDA1A614BCACDD651BC21A0DB414D291D96B4A23BFCE47A4D9D486E2D`
  (13,251,665 bytes). Recompute after any source change.
- Database schema is version 4 with explicit 1→2→3→4 migrations preserving
  messages/peers while adding course progress and observed-hop state.

## External blockers

- Real flood endpoint/retrain is not supplied; use a fake adapter.
- True out-of-range A→B→C range test remains user hardware work.
- No phones were visible to ADB during this implementation session, so the new
  shell, SOS, GPS, voice, course persistence, and topology UI are not yet
  physically verified. Prior three-phone mesh evidence remains preserved.

## Immediate next action

Install the new APK on all three phones and execute F01-F11, then implement the
remaining hardening work: durable Room outbox/process-death recovery and
evidence-backed Community delivery/seen receipts.
