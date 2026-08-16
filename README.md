# RakshaNet

RakshaNet is an offline-first Android disaster-readiness app combining verified
nearby mesh messaging, local flood-readiness courses, signed SOS alerts,
guidance broadcasts, and an offline mesh-topology view.

The Android project is Kotlin + Jetpack Compose + Room, with Google Nearby
Connections behind a transport-neutral `PacketRouter` boundary.

Start with [PRD.md](PRD.md), [FULL_APP_EXECUTION_PLAN.md](FULL_APP_EXECUTION_PLAN.md),
and [FULL_APP_PROGRESS.md](FULL_APP_PROGRESS.md).

## Build

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

Real Nearby/background/SOS device tests require physical Android phones; see
[DEVICE_TEST_GUIDE.md](DEVICE_TEST_GUIDE.md).

## Flood Prediction Service

The ML Flood Prediction Service is located in [`flood_prediction_service/`](./flood_prediction_service).
It provides a FastAPI service and simulation script (`simulate_feed.py`) to stream flood risk predictions (`LOW`, `MODERATE`, `SEVERE`) to the mesh network.
For service setup, training scripts, API specifications, and deployment guides, see [`flood_prediction_service/README.md`](./flood_prediction_service/README.md).

