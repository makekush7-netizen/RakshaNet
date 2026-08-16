# RakshaNet Full App Execution Plan

Last updated: 2026-08-16

## Locked product choices

- Four bottom tabs: Home, Courses, Connect, Alerts.
- “Map” is an offline mesh-topology view; no online map tiles or geocoding.
- SOS broadcasts a generic signed alert immediately. Category/note/location are
  optional refinements referencing the original alert.
- Raw GPS coordinates are explicit opt-in and never reverse-geocoded.
- Flood integration uses a fake/disabled adapter until the retrained endpoint is
  supplied and verified. No accuracy claim is embedded in the app.

## Phase A — Repository and regression baseline

Import the verified mesh source and preserve all PRDs/contracts/history. Exit:
the existing 30 JVM tests, debug APK, and lint pass in this repository.

## Phase B — Design system, shell, Home

Build reusable navy/teal/emergency tokens, typography/shapes, four-tab shell,
Home dashboard cards, live mesh status, and profile/settings entry. Exit:
build/lint plus navigation/state verification.

## Phase C — Connect restyle and receipts

Restyle chat without routing changes. Add Community Sent/Delivered/Seen only
when the corresponding evidence exists. Exit: automated mesh suite and physical
Community/private regression remain green.

## Phase D — Courses

Build the course tree, bundled Flood Readiness lessons and quiz, Room-backed
progress, and locked Earthquake/Storm/Ambassador nodes. Exit: progress survives
restart and quiz completion behaves correctly.

## Phase E — Alerts/SOS

Add signed SOS alert/update packets, immediate generic broadcast, optional
category/note/voice/raw-coordinate refinement, and incoming/outgoing alert feed.
Exit: protocol tests and three-phone physical relay/display.

## Phase F — Topology

Show known/connected peers, observed hop hints, and health. Do not claim best
routing without measurements. Exit: controlled three-phone topology evidence.

## Phase G — Flood gateway

Add typed `/predict` contract, fake implementation, risk-transition dedup,
reviewed templates, and signed guidance packets. Real HTTP stays disabled until
endpoint confirmation. Exit: fake LOW→SEVERE creates exactly one broadcast.

## Phase H — Hardening/release

Add durable outbox/process-death recovery, onboarding, OEM battery guidance,
accessibility, release configuration, and privacy copy. Exit: full suite,
three-device regression, true bridge test, 30-minute background test, signed APK.

No phase passes from file presence. Record automated and physical evidence in
the full-app progress and test matrix.
