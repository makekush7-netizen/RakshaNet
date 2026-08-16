# 🌊 RakshaNet — Flood Risk Command Center & AI Disaster Intelligence

RakshaNet is an end-to-end disaster-management command center and ML decision-support backend trained on authentic India Meteorological Department (IMD) historical precipitation observations for offline BLE mesh broadcast nodes and regional emergency management UI dashboards.

---

## 1. Command Center Architecture & System Flow

```text
               RAINFALL TELEMETRY INPUT
                          ↓
                 Feature Engineering
              (features.py / 9 Features)
                          ↓
             Calibrated ML Model Pipeline
            (StandardScaler + RandomForest)
                          ↓
                 Calibrated Classifier
            (CalibratedClassifierCV / Sigmoid)
                          ↓
               Flood Probability P(Flood)
                          ↓
           Operational Risk Threshold (0.60)
             (Derived on Validation Split)
                          ↓
        ┌─────────────────┼─────────────────┐
        ↓                 ↓                 ↓
     LOW RISK         MODERATE          SEVERE RISK
   (P < 0.60)     (0.60 <= P < 0.70)    (P >= 0.70)
        ↓                 ↓                 ↓
        └─────────────────┼─────────────────┘
                          ↓
            In-Dashboard Disaster Alert &
         Offline BLE Mesh Alert Broadcast
                          ↓
           Disaster Decision Support UI
```

---

## 2. Key Command Center Features

1. **Top Bar Header**: Real-time status badges (🟢 `SYSTEM ONLINE`, 🛡️ `Model v4.0.0`, 🎯 `Threshold: 60%`, 📍 `Region: Kerala Basin`, ⏱️ Real-time clock).
2. **Geographic Situational Awareness**: Interactive Leaflet.js map centered on Kerala River Basin (`9.9312° N, 76.2673° E`), featuring dynamic pulse markers colored by risk level, explicitly labeled **`DEMO / SIMULATED TELEMETRY`**.
3. **Real-time Charting Timeline**: Chart.js precipitation & probability timeline with a red threshold line at **0.60**.
4. **Alert Lifecycle Management**:
   - `ACTIVE` $\rightarrow$ `ACKNOWLEDGED` $\rightarrow$ `RESOLVED`
   - APIs: `GET /api/alerts`, `POST /api/alerts/{id}/acknowledge`, `POST /api/alerts/{id}/resolve`.
5. **Disaster Response Guidance Engine**: Displays Category 1/2/3 disaster preparedness protocols.
6. **Demo Mode Presets**: One-click quick presets (`Normal Monsoon`, `Heavy Monsoon`, `Extreme Rainfall`, `Random Historical Sample`, `Custom Input`).
7. **Model Transparency & Schema Breakdown**: Raw precipitation inputs (`annual_rainfall_mm`, `JUN`, `JUL`, `AUG`, `SEP`) and derived features (`monsoon_rainfall_mm`, `pre_monsoon_mm`, `peak_month_rainfall_mm`, `monsoon_ratio`).
8. **Component Health Checklist**: Live checklist verifying ML Model, API, Feature Engine, Telemetry, and Test Suite status.

---

## 3. Quickstart Commands

```bash
# 1. Install dependencies
pip install -r requirements.txt

# 2. Run reproducible ML training pipeline
python train_model.py

# 3. Run complete automated PyTest suite (34 tests)
python -m pytest tests/

# 4. Launch FastAPI production server & Command Center UI
python -m uvicorn app:app --host 0.0.0.0 --port 8000

# 5. Run Live Telemetry Simulator (Optional)
python simulate_feed.py
```

Access the Interactive Command Center Dashboard at: `http://localhost:8000/`

---

## 4. Docker Quickstart

```bash
# Build production Docker container
docker build -t rakshanet-api:latest .

# Run Docker container
docker run -p 8000:8000 --env-file .env.example rakshanet-api:latest
```

---

## 5. Authoritative Production Feature Schema

| Feature | Description | Source / Formula | Leakage Risk |
| :--- | :--- | :--- | :--- |
| `annual_rainfall_mm` | Annual total precipitation (mm) | IMD Station Record | None (Pre-event) |
| `monsoon_rainfall_mm` | Monsoon precipitation total (mm) | `JUN` + `JUL` + `AUG` + `SEP` | None (Pre-event) |
| `pre_monsoon_mm` | Pre-monsoon total (mm) | `MAR` + `APR` + `MAY` | None (Pre-event) |
| `peak_month_rainfall_mm` | Max monthly rainfall (mm) | Max(`JUN`, `JUL`, `AUG`, `SEP`) | None (Pre-event) |
| `monsoon_ratio` | Monsoon ratio | `monsoon_rainfall_mm` / `annual_rainfall_mm` | None (Pre-event) |
| `JUN`, `JUL`, `AUG`, `SEP` | Monthly precipitation totals (mm) | IMD Rain Gauge Records | None (Pre-event) |

---

## 6. Temporal Validation & Verified Model Metrics

- **Temporal Splits (1901 – 2018 / 118 Years)**:
  - Train: 1901 – 1982 (82 samples / 70%)
  - Validation: 1983 – 1999 (17 samples / 15%)
  - Untouched Test: 2000 – 2018 (19 samples / 15%) — **Zero Shuffling**

- **Untouched Temporal Test Metrics (2000 – 2018)**:
  - **Accuracy**: **94.74%**
  - **Precision**: **88.89%**
  - **Recall**: **100.00%** (Zero missed flood events across 18 years)
  - **F1-Score**: **94.12%**
  - **ROC-AUC**: **1.0000**
  - **PR-AUC**: **1.0000**
  - **Brier Score**: **0.0364**
  - **Confusion Matrix**: `[[10, 1], [0, 8]]`

---

## 7. 2–3 Minute Judge Demonstration Flow

1. **Open Command Center**: Navigate to `http://localhost:8000/`. Point out header badges showing `🟢 SYSTEM ONLINE`, `🛡️ Model v4.0.0`, `🎯 Operational Decision Threshold: 60%`, and the Leaflet.js situational map centered on Kerala Basin.
2. **Scenario 1 — Normal Monsoon**: Click `[ 🟢 Normal (1,800mm) ]` scenario preset $\rightarrow$ Click `⚡ Evaluate Flood Risk`. Point out **LOW RISK (3.9%)**, green map marker, and protocol guidance: *"Continue standard telemetry monitoring."*
3. **Scenario 2 — Heavy Monsoon**: Click `[ 🟡 Heavy (2,600mm) ]` scenario preset $\rightarrow$ Click `⚡ Evaluate Flood Risk`. Point out probability increase on Chart.js timeline and yellow warning marker.
4. **Scenario 3 — Extreme Rainfall**: Click `[ 🔴 Extreme (3,500mm) ]` scenario preset $\rightarrow$ Click `⚡ Evaluate Flood Risk`. Point out **SEVERE RISK (95.3%)**, red map marker, red probability meter crossing 60%, **EMERGENCY FLOOD RISK ALERT** banner, generated alert `ALT-1001` in the active alerts list, and Category 3 evacuation preparedness recommendations.
5. **Alert Management**: Click `[ Acknowledge ]` on alert `ALT-1001`, then click `[ Resolve ]` to demonstrate alert lifecycle management.
6. **Scientific Transparency & Limitations**: Point out the Model Input Breakdown table, Component Verification Checklist (32/32 tests passing, 0% leakage), and Scientific Scope Notice.

---

## 8. API Reference

### Core & Diagnostic Endpoints
- `GET /health`: System health status and loaded model info.
- `GET /readiness`: Readiness check for production load balancers.
- `GET /metadata`: Complete model metadata JSON.
- `GET /model-info`: Model architecture, threshold, and audit profile.

### Prediction & Alert Endpoints
- `POST /predict`: Main flood risk prediction endpoint.
- `GET /api/alerts?status=ACTIVE|ACKNOWLEDGED|RESOLVED|ALL`: Get emergency alerts.
- `POST /api/alerts/{alert_id}/acknowledge`: Acknowledge emergency alert.
- `POST /api/alerts/{alert_id}/resolve`: Resolve emergency alert.
- `GET /api/history`: Get prediction log history.
- `GET /api/telemetry/latest`: Get latest telemetry reading.
- `GET /api/stats`: Get prediction statistics summary.
- `GET /api/sample-data`: Get held-out dataset records.

---

## 9. Capability Scope & Scientific Limitation Notice

- **CURRENT CAPABILITY**:
  Annual and regional historical flood-risk assessment trained on 118 IMD rainfall observations for the Kerala subdivision.
- **FUTURE CAPABILITY**:
  Expanding to daily multi-station river gauge sensor networks and radar telemetry for intraday hydrograph forecasting.
