import pytest
import os
import pandas as pd
from config import Config

def test_dataset_exists():
    assert os.path.exists(Config.DATASET_PATH), f"Dataset file '{Config.DATASET_PATH}' must exist."

def test_dataset_schema_and_integrity():
    df = pd.read_csv(Config.DATASET_PATH)
    assert not df.empty, "Dataset must not be empty."
    assert "YEAR" in df.columns, "Dataset must contain 'YEAR' column."
    assert "FLOODS" in df.columns, "Dataset must contain 'FLOODS' target column."
    
    # Target values check
    unique_targets = df['FLOODS'].unique()
    assert set(unique_targets).issubset({"YES", "NO"}), f"Target values must be YES/NO, got {unique_targets}"

def test_no_duplicate_years():
    df = pd.read_csv(Config.DATASET_PATH)
    duplicates = df[df.duplicated("YEAR")]
    assert len(duplicates) == 0, f"Dataset contains duplicate years: {duplicates['YEAR'].tolist()}"

def test_monthly_rainfall_non_negative():
    df = pd.read_csv(Config.DATASET_PATH)
    monthly_cols = ['JAN', 'FEB', 'MAR', 'APR', 'MAY', 'JUN', 'JUL', 'AUG', 'SEP', 'OCT', 'NOV', 'DEC']
    for col in monthly_cols:
        if col in df.columns:
            assert (df[col] >= 0).all(), f"Column '{col}' contains negative rainfall values."
