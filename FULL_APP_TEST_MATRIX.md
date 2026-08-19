# RakshaNet Full App Test Matrix

Last updated: 2026-08-17

The existing rows preserve evidence for the functional baseline. The V2 visual
flow has not been implemented or tested; the user will perform device/UI checks.

| ID | Area | Test | Status | Evidence / next requirement |
|---|---|---|---|---|
| V00 | Planning | V2 product and architecture are internally specified | PASS | `PRODUCT_BLUEPRINT_V2.md`, `SYSTEM_ARCHITECTURE_V2.md`, and `DEMO_RUNBOOK.md` created 2026-08-17. |
| V01 | Repository | Local mesh fix and fetched ML history integrated cleanly | NOT RUN | Fetch only; merge intentionally deferred until local fix is committed. |
| V02 | Design | Four native-style screen previews approved | PASS | User approved the generated mobile and website direction on 2026-08-17. |
| V03 | Control plane | Browser drill/report/analysis/approve flow | PARTIAL | Static interactive prototype builds and is privately published; real incident/AI/authority operations are intentionally disabled and disclosed. |
| V04 | Gateway | Website event reaches offline phone through online phone | NOT RUN | User physical two-phone test. |
| V05 | Gateway | Offline field report returns to authority website | NOT RUN | User physical two-phone test. |
| V06 | Safety | Every simulated incident surface says DRILL | NOT RUN | Website, payload, notification, Home, Community, Alerts. |
| V07 | Release | Versioned APK downloads from website | PASS | Public Vercel URL returned HTTP 200, `application/vnd.android.package-archive`, and exact local size 21,027,103 bytes for v0.3.1 on 2026-08-17. Local SHA-256 is `D95DCD6C6EF5994F8006C3D941EEF93EE23D50C502398207F6CD59A145446A58`. |
| V08 | Android V2 | Approved navigation/theme/screens compile | PASS | `:app:assembleDebug` passed on 2026-08-17; no device/UI claim. |
| V09 | Learning | Flood journey plus Earthquake/Cyclone previews compile | PARTIAL | Four lessons, five questions, generated imagery and Coming soon cards compile; user device review pending. |
| V10 | Android V2 | Chat, lesson flow and quiz manual review | PASS | User reports all three work correctly on Redmi Note 10 Pro and Galaxy J8 and supplied screenshots on 2026-08-17. |
| V11 | Android V2 | Dark action-button labels remain readable | PARTIAL | Screenshots reproduced invisible labels; explicit white labels applied and APK assembles. Corrected APK still needs user visual confirmation. |
| V12 | Web release | Git-connected Vercel production deployment | PASS | GitHub `main` deploys from root `web/`; deployment `dpl_7EBBVtv5HH18vkNxuL8GF8RTgHKQ` is READY at `https://rakshanet-gamma.vercel.app`. Homepage and APK route verified; first-hour runtime error scan clean. |

| ID | Area | Test | Status | Evidence / next requirement |
|---|---|---|---|---|
| F00 | Baseline | Imported project full test/build/lint | PASS | `:app:testDebugUnitTest :app:assembleDebug :app:lintDebug`; 30 JVM tests pass, APK assembled, lint clean on 2026-08-16. |
| F01 | Shell | Four tabs navigate correctly | PARTIAL | Four-tab shell compiles; manual device navigation pending. |
| F02 | Home | Mesh status reflects live transport | PARTIAL | Home is wired to live coordinator/service state; physical state-change check pending. |
| F03 | Connect | Community/private/ACK regression | PARTIAL | Coordinator tests cover transport-confirmed Delivered and display-triggered signed Seen; three-phone regression pending. |
| F04 | Courses | Flood lessons render offline | PARTIAL | Bundled content compiles and needs no network; device/manual rendering pending. |
| F05 | Courses | Quiz/progress survives restart | PARTIAL | Room v4 repository and progress-calculator tests pass; process/device restart pending. |
| F06 | SOS | Generic alert is signed/stored/relayed | PARTIAL | Codec/rules/coordinator storage, relay and dedup JVM tests pass; three phones pending. |
| F07 | SOS | Refinement references original | PASS | Protocol/coordinator JVM tests verify the signed reference to the original SOS. |
| F08 | SOS | Voice handles unavailable recognizer | NOT RUN | Physical device. |
| F09 | SOS | Location is opt-in/raw/offline | PARTIAL | Default-off permission flow and raw fused coordinates compile; physical device pending. |
| F10 | Alerts | SOS/guidance appears once in feed | PARTIAL | Alert query/dedup/coordinator tests pass; device rendering pending. |
| F11 | Topology | Peer/hop view is truthful | PARTIAL | TTL-derived observed-hop JVM test passes and UI labels it as observed, not optimal; three phones pending. |
| F12 | Gateway | Fake risk change emits one guidance | PASS | Deterministic gateway/transition JVM tests emit once per actual level change. |
| F13 | Gateway | Real LOW/MODERATE/SEVERE contract | BLOCKED | Await endpoint. |
| F14 | Hardening | True out-of-range A→B→C | NOT RUN | User range test. |
| F15 | Hardening | 30-minute screen-off/background | NOT RUN | Three phones. |
| F16 | Hardening | Queued packet survives process death | PARTIAL | Pending-packet reload/resend unit test passes and Room query compiles; force-stop/device test pending. |

Automated full gate on 2026-08-17: 45 JVM tests passed, debug APK assembled,
and `lintDebug` passed. `PARTIAL` never substitutes for required device evidence.

| V13 | Documentation | Unlisted technical field notes route | PASS | `/field-notes-2026` was added with `noindex` metadata. `web/npm run build` passed on 2026-08-20; Git-connected production publication pending. |
