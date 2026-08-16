import pytest
import os
import json
import joblib
import pandas as pd
import numpy as np
from fastapi.testclient import TestClient

from app import app, MODEL_PATH, METADATA_PATH, DATASET_PATH

client = TestClient(app)

def test_metadata_and_artifacts_exist():
    assert os.path.exists(MODEL_PATH), f"Model artifact '{MODEL_PATH}' should exist."
    assert os.path.exists(METADATA_PATH), f"Metadata file '{METADATA_PATH}' should exist."
    assert os.path.exists(DATASET_PATH), f"Held-out test dataset '{DATASET_PATH}' should exist."

def test_metadata_contents():
    with open(METADATA_PATH, "r") as f:
        meta = json.load(f)
        
    assert "model_version" in meta
    assert "metrics" in meta
    assert "optimal_threshold" in meta
    assert meta["classes"] == [0, 1]

def test_model_artifact_load_and_classes():
    model = joblib.load(MODEL_PATH)
    assert hasattr(model, "predict_proba"), "Loaded model must support predict_proba."

def test_api_health_endpoint():
    response = client.get("/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "healthy"
    assert data["model_loaded"] is True
    assert "model_version" in data
    assert "timestamp" in data

def test_api_predict_valid_payload():
    payload = {
        "region_id": "test_region_kerala",
        "rainfall_mm": 3250.0,
        "monsoon_rainfall_mm": 2665.0,
        "pre_monsoon_mm": 260.0
    }
    response = client.post("/predict", json=payload)
    assert response.status_code == 200
    data = response.json()
    
    assert data["region_id"] == "test_region_kerala"
    assert data["risk_level"] in ["LOW", "MODERATE", "SEVERE"]
    assert 0.0 <= data["flood_probability"] <= 1.0
    assert "threshold" in data

def test_api_predict_scenarios():
    low_payload = {
        "region_id": "test_low",
        "rainfall_mm": 1800.0
    }
    resp_low = client.post("/predict", json=low_payload)
    assert resp_low.status_code == 200
    assert resp_low.json()["risk_level"] in ["LOW", "MODERATE"]
    
    severe_payload = {
        "region_id": "test_severe",
        "rainfall_mm": 3600.0
    }
    resp_sev = client.post("/predict", json=severe_payload)
    assert resp_sev.status_code == 200
    assert resp_sev.json()["risk_level"] in ["MODERATE", "SEVERE"]

def test_api_predict_missing_required_field():
    invalid_payload = {
        "region_id": "bad_req"
        # Missing required rainfall_mm
    }
    response = client.post("/predict", json=invalid_payload)
    assert response.status_code == 422  # Unprocessable Entity

def test_api_predict_invalid_negative_rainfall():
    invalid_payload = {
        "region_id": "negative_rain",
        "rainfall_mm": -500.0
    }
    response = client.post("/predict", json=invalid_payload)
    assert response.status_code == 422
