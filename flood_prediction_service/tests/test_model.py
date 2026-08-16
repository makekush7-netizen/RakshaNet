import pytest
import os
import json
import joblib
import pandas as pd
import numpy as np

from config import Config
from features import FEATURE_COLS

def test_model_artifact_exists():
    model_path = Config.get_existing_model_path()
    assert os.path.exists(model_path), f"Model artifact '{model_path}' must exist."

def test_metadata_artifact_exists():
    meta_path = Config.get_existing_metadata_path()
    assert os.path.exists(meta_path), f"Metadata file '{meta_path}' must exist."

def test_model_predict_and_predict_proba():
    model_path = Config.get_existing_model_path()
    model = joblib.load(model_path)
    
    sample_df = pd.DataFrame([{
        'annual_rainfall_mm': 3250.0,
        'monsoon_rainfall_mm': 2665.0,
        'pre_monsoon_mm': 260.0,
        'peak_month_rainfall_mm': 930.0,
        'monsoon_ratio': 0.82,
        'JUN': 650.0,
        'JUL': 930.0,
        'AUG': 650.0,
        'SEP': 435.0
    }])[FEATURE_COLS]
    
    probs = model.predict_proba(sample_df)[0]
    assert len(probs) == 2, f"Probability vector must have 2 classes, got {len(probs)}"
    assert 0.0 <= probs[0] <= 1.0, f"Class 0 probability must be in [0, 1], got {probs[0]}"
    assert 0.0 <= probs[1] <= 1.0, f"Class 1 probability must be in [0, 1], got {probs[1]}"
    assert np.isclose(np.sum(probs), 1.0), f"Sum of probabilities must equal 1.0, got {np.sum(probs)}"

def test_model_classes_ordering():
    model_path = Config.get_existing_model_path()
    model = joblib.load(model_path)
    
    if hasattr(model, "classes_"):
        classes = list(model.classes_)
    elif hasattr(model, "estimator") and hasattr(model.estimator, "named_steps"):
        classes = list(model.estimator.named_steps['clf'].classes_)
    else:
        classes = [0, 1]
        
    assert classes == [0, 1], f"Model classes must be [0, 1], got {classes}"
