# RakshaNet Deployment & Quickstart Guide

## 1. Environment Setup

```bash
# Clone or navigate to project directory
cd SmartAlthon

# Install reproducible dependencies
pip install -r requirements.txt
```

## 2. Train Model & Export Artifacts

```bash
python train_model.py
```

Outputs generated:
- `models/model.pkl` & `model.pkl`
- `models/metadata.json` & `metadata.json`
- `models/feature_schema.json` & `feature_schema.json`
- `models/metrics.json` & `metrics.json`
- `heldout_test_data.csv`

## 3. Run Automated PyTest Suite

```bash
python -m pytest tests/test_model_and_api.py
```

## 4. Run Production FastAPI Application

```bash
python -m uvicorn app:app --host 0.0.0.0 --port 8000 --reload
```

Interactive Web Dashboard available at: `http://localhost:8000/`

## 5. Run Live Telemetry Feed Simulator

```bash
python simulate_feed.py
```

## 6. Public HTTPS Cloud Deployment Options

### Hugging Face Spaces (Docker SDK)
1. Push repository files (`app.py`, `features.py`, `models/`, `Dockerfile`, `requirements.txt`) to Hugging Face.
2. Select **Docker** as Space SDK.
3. Hugging Face builds and deploys to public HTTPS endpoint: `https://your-space.hf.space/predict`.

### Render / Railway / AWS App Runner
- Build Command: `pip install -r requirements.txt && python train_model.py`
- Start Command: `uvicorn app:app --host 0.0.0.0 --port $PORT`
