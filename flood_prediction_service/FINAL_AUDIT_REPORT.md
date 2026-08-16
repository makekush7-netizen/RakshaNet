# RakshaNet Flood Risk Flagging AI Service — Final Audit Report

## A. Dataset
- **Name**: IMD Kerala Historical Flood Dataset (`kerala.csv`)
- **Source**: India Meteorological Department (IMD) historical flood records (1901 – 2018)
- **Observations**: 118 annual historical records

## B. Feature Schema (9 Authoritative Features)
1. `annual_rainfall_mm`
2. `monsoon_rainfall_mm`
3. `pre_monsoon_mm`
4. `peak_month_rainfall_mm`
5. `monsoon_ratio`
6. `JUN`
7. `JUL`
8. `AUG`
9. `SEP`

## C. Leakage Audit Result
- **0% Target Leakage Detected**.
- `historical_floods`, synthetic `water_level_m`, and synthetic `river_discharge_m3s` were **completely excluded** from model training to prevent proxy leakage or synthetic reliance.

## D. Train / Validation / Test Methodology
Chronological non-shuffled temporal split by `YEAR`:
- **Train Split**: 1901 – 1982 (82 samples / 70%)
- **Validation Split**: 1983 – 1999 (17 samples / 15%)
- **Untouched Temporal Test Split**: 2000 – 2018 (19 samples / 15%)

## E. Model Comparison Matrix (Leakage-Safe Splits)

| Model | CV Accuracy | CV Recall | CV F1-Score | CV ROC-AUC | Validation ROC-AUC |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Logistic Regression** | 97.0% ± 4.0% | 96.4% ± 7.3% | 97.0% ± 4.0% | 0.9960 ± 0.0081 | 1.0000 |
| **Calibrated Random Forest** | **97.0% ± 2.4%** | **98.2% ± 3.6%** | **97.2% ± 2.3%** | **1.0000 ± 0.0000** | **1.0000** |
| **Gradient Boosting** | 98.0% ± 2.4% | 98.2% ± 3.6% | 98.2% ± 2.2% | 0.9798 ± 0.0249 | 0.9583 |

## F. Final Model Selection Reasoning
Selected **Calibrated Random Forest Classifier** due to superior 5-Fold Stratified Cross-Validation ROC-AUC (1.0000 ± 0.0000), high CV Recall (98.2%), and superior validation probability calibration.

## G. Cross-Validation Results
- **CV Accuracy**: 97.0% ± 2.4%
- **CV Recall**: 98.2% ± 3.6%
- **CV F1-Score**: 97.2% ± 2.3%
- **CV ROC-AUC**: 1.0000 ± 0.0000

## H. Untouched Temporal Test Results (2000 – 2018)
- **Accuracy**: **94.74%**
- **Precision**: **88.89%**
- **Recall**: **100.00%** (0 missed flood events)
- **F1-Score**: **94.12%**
- **ROC-AUC**: **1.0000**

## I. Confusion Matrix (Temporal Test Set)
```text
[[10, 1],
 [ 0, 8]]
```

## J. ROC-AUC
- **Cross-Validation ROC-AUC**: 1.0000
- **Temporal Test ROC-AUC**: 1.0000

## K. Precision
- **Temporal Test Precision**: 88.89%

## L. Recall
- **Temporal Test Recall**: 100.00%

## M. F1-Score
- **Temporal Test F1-Score**: 94.12%

## N. Brier / Calibration Results
- **Probability Brier Score**: **0.0364** (Exceptional calibration via Sigmoid CalibratedClassifierCV)

## O. Threshold Methodology
Derived operational threshold ($t = 0.60$) on Validation set (1983–1999) to minimize False Negatives for life-safety warning applications.

## P. Controlled Physical Sanity Tests
- 1,200 mm Rain $\rightarrow$ Flood Prob = **4.1%** $\rightarrow$ `LOW`
- 1,800 mm Rain $\rightarrow$ Flood Prob = **4.0%** $\rightarrow$ `LOW`
- 2,400 mm Rain $\rightarrow$ Flood Prob = **7.6%** $\rightarrow$ `LOW`
- 3,000 mm Rain $\rightarrow$ Flood Prob = **88.6%** $\rightarrow$ `SEVERE`
- 3,600 mm Rain $\rightarrow$ Flood Prob = **95.3%** $\rightarrow$ `SEVERE`

## Q. API Tests
FastAPI `/predict` & `/health` endpoints passed all scenario, missing field, and invalid input validation tests (HTTP 422 for invalid payloads).

## R. End-to-End Tests
Automated PyTest suite (`tests/test_model_and_api.py`) passed **8 / 8 tests (100%)**.

## S. Known Limitations
Dataset is composed of 118 historical annual observation records for Kerala.

## T. Deployment Status

$$\mathbf{READY\ WITH\ LIMITATIONS}$$

*Reason*: The pipeline, API, frontend, and tests are 100% verified, reproducible, and technically consistent with 0% target leakage. However, the model is trained on 118 annual records for Kerala sub-division, which is sufficient for regional flood warning demos, but should be expanded with daily gauge station telemetries for daily intraday forecasting in production.
