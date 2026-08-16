import pytest
import pandas as pd
import numpy as np
from features import FEATURE_COLS, prepare_features, FeatureEngineer

def test_feature_cols_list():
    assert len(FEATURE_COLS) == 9
    assert 'annual_rainfall_mm' in FEATURE_COLS
    assert 'monsoon_rainfall_mm' in FEATURE_COLS

def test_prepare_features_from_annual_only():
    raw_df = pd.DataFrame([{"annual_rainfall_mm": 3200.0}])
    df_feat = prepare_features(raw_df)
    
    assert list(df_feat.columns) == FEATURE_COLS
    assert df_feat.iloc[0]['annual_rainfall_mm'] == 3200.0
    assert df_feat.iloc[0]['monsoon_rainfall_mm'] == 3200.0 * 0.82
    assert df_feat.iloc[0]['monsoon_ratio'] > 0.0

def test_prepare_features_from_monthly():
    raw_df = pd.DataFrame([{
        "JAN": 10.0, "FEB": 10.0, "MAR": 50.0, "APR": 50.0, "MAY": 100.0,
        "JUN": 600.0, "JUL": 900.0, "AUG": 600.0, "SEP": 400.0,
        "OCT": 200.0, "NOV": 100.0, "DEC": 20.0
    }])
    df_feat = prepare_features(raw_df)
    
    assert df_feat.iloc[0]['annual_rainfall_mm'] == 3040.0
    assert df_feat.iloc[0]['monsoon_rainfall_mm'] == 2500.0
    assert df_feat.iloc[0]['pre_monsoon_mm'] == 200.0
    assert df_feat.iloc[0]['peak_month_rainfall_mm'] == 900.0

def test_feature_engineer_transformer():
    fe = FeatureEngineer()
    transformer = fe.fit(None)
    res = transformer.transform([{"rainfall_mm": 2800.0}])
    assert isinstance(res, pd.DataFrame)
    assert list(res.columns) == FEATURE_COLS
