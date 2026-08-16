# RakshaNet V2 Execution Plan

Last updated: 2026-08-17

The earlier B–G implementation remains a useful functional baseline, but its
screen flow and styling were rejected. V2 redesigns the product around
`PRODUCT_BLUEPRINT_V2.md` while preserving the verified mesh core.

## Workstream 0 — Protect and integrate the repository

1. Commit the two current connected-status mesh fixes and decision record.
2. Integrate fetched remote commit `5d531fc` without overwriting local work.
3. Run the narrow merge/build checks once; no physical UI loop.
4. Mechanically organize the model under `services/flood-model/` in a separate
   commit after the integration is stable.

Exit: one clean branch contains Android plus the teammate's exact ML work, with
history preserved and no unresolved changes.

## Workstream 1 — V2 design contract before app code

1. Produce four high-fidelity screen specifications: Prepared Home, Active
   Incident Home, Community incident room, and Learn journey.
2. Define component inventory, spacing/type scale, icon family, image aspect
   ratios, empty/loading/error states, and exact copy.
3. Generate only two or three restrained flood content assets after the native
   composition is approved.
4. User approves the direction from static previews before Compose changes.

Exit: approved light visual direction with no ambiguous screen flow.

## Workstream 2 — Demo infrastructure

1. Add versioned JSON contracts.
2. Build control-plane incident/event/report endpoints with idempotent storage.
3. Deploy the existing flood service and verify its real `/predict` contract.
4. Build the website landing/download page and labelled simulator/authority UI.
5. Add deterministic analysis fallback, then optional structured LLM adapter.

Exit: browser-only drill can be started, reports added, analysis drafted, and an
authority message approved without Android.

## Workstream 3 — Android gateway vertical slice

1. Add verified cloud envelopes and a durable sequence cursor.
2. Poll while online; persist before injecting an event into `MeshCoordinator`.
3. Upload structured field reports idempotently when a gateway is available.
4. Continue offline on cloud failure and sync later.

Exit: local automated contract tests pass, followed by one user-run two-phone
test proving website → online Redmi → offline Samsung → report return.

## Workstream 4 — V2 Android experience

Implement in demo-value order:

1. Theme tokens and shared components.
2. Prepared/active Home modes and network-health sheet.
3. Community incident room and structured update composer.
4. Authority bulletin and Alerts/SOS refinement.
5. Learn journey with richer flood chapters and sourced content.
6. First-run identity/community/readiness flow.

Exit: user performs visual and interaction review on Redmi and Galaxy J8. The
agent only runs narrow compile/unit checks needed to hand over an installable
APK; it does not repeatedly operate the phones for cosmetic verification.

## Workstream 5 — Demo rehearsal and release

1. Build a versioned APK and publish its hash/QR on `/download`.
2. Execute `DEMO_RUNBOOK.md` once live, fix only observed blockers, and record a
   backup video.
3. Run the narrow safety regression: Community, private, SOS, screen-off relay,
   cloud-to-mesh, mesh-to-cloud.
4. Clearly mark all physical and untested claims in the matrices.

Exit: reproducible three-minute demonstration and a downloadable APK.

## Deferred production work

- Authority credential issuance/key rotation.
- Global public membership and abuse controls.
- Best-path routing and dense-network measurements.
- Production database/observability/SLA work.
- Real-time multi-station hydrological forecasting.
- End-to-end private-message confidentiality.
