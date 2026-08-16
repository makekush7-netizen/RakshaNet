# RakshaNet Full App Agent Guide

This repository is the source of truth for the complete RakshaNet Android app.
The verified offline mesh is a protected foundation, not a prototype to rewrite.

## Read order at the start of every chat

1. `PRD.md` — full product scope and design requirements.
2. `HANDOFF_TO_CLAUDE.md` — verified mesh architecture and evidence.
3. `HANDOFF_FLOOD_MODEL.md` — external prediction-service contract.
4. `DECISIONS.md` — locked technical/product choices.
5. `FULL_APP_EXECUTION_PLAN.md` — implementation phases and gates.
6. `FULL_APP_PROGRESS.md` — current implementation state and next action.
7. `FULL_APP_TEST_MATRIX.md` — full-app verification record.
8. `TEST_MATRIX.md` — preserved detailed mesh evidence.

## Working rules

- Preserve `PacketRouter`, `MeshCoordinator`, signing, TTL/dedup, foreground
  service ownership, trusted reconnect, and Android 15 permission repairs.
- Red is reserved for emergency/SOS UI only.
- No account or mandatory network dependency. Courses and emergency flows remain
  useful offline.
- Never claim physical mesh, GPS, speech, background, or range behavior without
  physical-device evidence.
- Implement one testable vertical slice at a time and record exact evidence.
- Build flood integration against its contract and a fake adapter until the
  retrained endpoint is explicitly supplied and verified.
- Use Room migrations; never destroy existing mesh/chat data to add a feature.
- Update decisions, progress, and tests after every material session.
- Preserve user changes and avoid destructive Git operations.

## Status definitions

- `PASS`: executed and observed.
- `FAIL`: executed and did not meet expectation.
- `PARTIAL`: useful evidence exists but the full gate was not exercised.
- `BLOCKED`: a concrete external dependency prevents execution.
- `NOT RUN`: no evidence yet.
