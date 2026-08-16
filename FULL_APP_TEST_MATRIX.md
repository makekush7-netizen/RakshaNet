# RakshaNet Full App Test Matrix

Last updated: 2026-08-16

| ID | Area | Test | Status | Evidence / next requirement |
|---|---|---|---|---|
| F00 | Baseline | Imported project full test/build/lint | PASS | `:app:testDebugUnitTest :app:assembleDebug :app:lintDebug`; 30 JVM tests pass, APK assembled, lint clean on 2026-08-16. |
| F01 | Shell | Four tabs navigate correctly | PARTIAL | Four-tab shell compiles; manual device navigation pending. |
| F02 | Home | Mesh status reflects live transport | PARTIAL | Home is wired to live coordinator/service state; physical state-change check pending. |
| F03 | Connect | Community/private/ACK regression | NOT RUN | Three phones. |
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

Automated full gate on 2026-08-16: 42 JVM tests passed, debug APK assembled,
and `lintDebug` passed. `PARTIAL` never substitutes for required device evidence.
