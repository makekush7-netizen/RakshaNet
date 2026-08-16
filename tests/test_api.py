import pytest
import os
import json
from fastapi.testclient import TestClient
from app import app

client = TestClient(app)

def test_api_health_endpoint():
    response = client.get("/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "healthy"
    assert data["model_loaded"] is True
    assert "model_version" in data
    assert "timestamp" in data

def test_api_readiness_endpoint():
    response = client.get("/readiness")
    assert response.status_code == 200
    assert response.json()["status"] == "ready"

def test_api_metadata_endpoint():
    response = client.get("/metadata")
    assert response.status_code == 200
    data = response.json()
    assert "model_version" in data
    assert "optimal_threshold" in data

def test_api_model_info_endpoint():
    response = client.get("/model-info")
    assert response.status_code == 200
    data = response.json()
    assert data["model_name"] == "Calibrated Random Forest Classifier"
    assert data["optimal_threshold"] == 0.60
    assert data["temporal_test_recall"] == "100.0%"

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
    assert "recommended_action" in data

def test_api_predict_scenarios():
    low_payload = {"region_id": "test_low", "rainfall_mm": 1800.0}
    resp_low = client.post("/predict", json=low_payload)
    assert resp_low.status_code == 200
    assert resp_low.json()["risk_level"] in ["LOW", "MODERATE"]
    
    severe_payload = {"region_id": "test_severe", "rainfall_mm": 3600.0}
    resp_sev = client.post("/predict", json=severe_payload)
    assert resp_sev.status_code == 200
    assert resp_sev.json()["risk_level"] in ["MODERATE", "SEVERE"]

def test_api_predict_missing_required_field():
    invalid_payload = {"region_id": "bad_req"}
    response = client.post("/predict", json=invalid_payload)
    assert response.status_code == 422  # Unprocessable Entity

def test_api_predict_invalid_negative_rainfall():
    invalid_payload = {"region_id": "negative_rain", "rainfall_mm": -500.0}
    response = client.post("/predict", json=invalid_payload)
    assert response.status_code == 422

def test_api_predict_malformed_string_rainfall():
    response = client.post("/predict", content='{"rainfall_mm": "invalid_string"}', headers={"Content-Type": "application/json"})
    assert response.status_code == 422

def test_api_predict_raw_nan_text():
    response = client.post("/predict", content='{"rainfall_mm": NaN}', headers={"Content-Type": "application/json"})
    assert response.status_code == 422

def test_api_stats_endpoint():
    response = client.get("/api/stats")
    assert response.status_code == 200
    data = response.json()
    assert "total_predictions" in data

def test_api_sample_data_endpoint():
    response = client.get("/api/sample-data?count=2")
    assert response.status_code == 200
    assert "samples" in response.json()

def test_api_alert_lifecycle():
    # Trigger severe alert
    severe_payload = {"region_id": "test_alert_region", "rainfall_mm": 3600.0}
    resp = client.post("/predict", json=severe_payload)
    assert resp.status_code == 200
    alert_id = resp.json().get("alert_id")
    assert alert_id is not None
    
    # Get alerts
    alerts_resp = client.get("/api/alerts?status=ACTIVE")
    assert alerts_resp.status_code == 200
    active_alerts = alerts_resp.json()["alerts"]
    assert any(a["alert_id"] == alert_id for a in active_alerts)
    
    # Acknowledge
    ack_resp = client.post(f"/api/alerts/{alert_id}/acknowledge")
    assert ack_resp.status_code == 200
    assert ack_resp.json()["alert_status"] == "ACKNOWLEDGED"
    
    # Resolve
    res_resp = client.post(f"/api/alerts/{alert_id}/resolve")
    assert res_resp.status_code == 200
    assert res_resp.json()["alert_status"] == "RESOLVED"

def test_api_history_and_telemetry():
    hist_resp = client.get("/api/history?limit=5")
    assert hist_resp.status_code == 200
    assert "history" in hist_resp.json()
    
    tel_resp = client.get("/api/telemetry/latest")
    assert tel_resp.status_code == 200
    assert "telemetry" in tel_resp.json()
    assert tel_resp.json()["system_status"] == "ONLINE"
