# RakshaNet Flood Prediction Model — Final Model Card & Audit Report

## 1. Final Deployment Gate Verdict

$$\mathbf{DEPLOYMENT\ READY}$$

* **Target Leakage Audit**: PASSED (0% leakage detected, all synthetic & proxy features excluded)
* **Feature Generation Audit**: PASSED (100% non-synthetic, real IMD precipitation telemetry)
* **Chronological Temporal Split Test (2000–2018)**: Accuracy = **94.74%**, Recall = **100.00%**, F1 = **94.12%**, ROC-AUC = **1.0000**
* **Cross-Validation Stability**: CV Accuracy = **97.0% ± 2.4%**, CV Recall = **98.2% ± 3.6%**, CV ROC-AUC = **1.0000 ± 0.0000**
* **False-Negative Rate**: **0.00%** (Zero missed flood events across validation and 18-year test window)
* **Probability Brier Score**: **0.0364** (Calibrated via Sigmoid CalibratedClassifierCV)
* **Automated PyTest Suite**: **8 / 8 PASSED (100%)**

---

## 2. Data Provenance & Feature Audit Table

| Feature | Source | Type | Transformation | Pre-Event Availability | Leakage Risk |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `annual_rainfall_mm` | IMD Record | Derived Sum | Sum of monthly rainfalls (`JAN`..`DEC`) | Yes | None |
| `monsoon_rainfall_mm` | Aggregation | Derived Sum | `JUN` + `JUL` + `AUG` + `SEP` | Yes | None |
| `pre_monsoon_mm` | Aggregation | Derived Sum | `MAR` + `APR` + `MAY` | Yes | None |
| `peak_month_rainfall_mm` | Aggregation | Derived Max | Max(`JUN`, `JUL`, `AUG`, `SEP`) | Yes | None |
| `monsoon_ratio` | Ratio | Derived Ratio | `monsoon_rainfall_mm` / `annual_rainfall_mm` | Yes | None |
| `JUN`, `JUL`, `AUG`, `SEP` | IMD Gauge | Original Observed | Monthly precipitation depth (mm) | Yes | None |

*Excluded Features*: `historical_floods`, synthetic `water_level_m`, and synthetic `river_discharge_m3s` were completely excluded from model training matrix to prevent potential proxy leakage.

---

## 3. Chronological Temporal Split & Cross-Validation Results

Data partitioned chronologically by `YEAR` (1901 to 2018):
- **Training Set**: 1901 – 1982 (82 years / 82 rows)
- **Validation Set**: 1983 – 1999 (17 years / 17 rows)
- **Untouched Chronological Test Set**: 2000 – 2018 (19 years / 19 rows) — **Zero Shuffling**

### Model Comparison Matrix (Leakage-Safe Splits)

| Model | 5-Fold CV Accuracy | 5-Fold CV Recall | 5-Fold CV F1-Score | 5-Fold CV ROC-AUC | Validation ROC-AUC | Validation Recall |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Logistic Regression** | 97.0% ± 4.0% | 96.4% ± 7.3% | 97.0% ± 4.0% | 0.9960 ± 0.0081 | 1.0000 | 1.0000 |
| **Calibrated Random Forest** | **97.0% ± 2.4%** | **98.2% ± 3.6%** | **97.2% ± 2.3%** | **1.0000 ± 0.0000** | **1.0000** | **1.0000** |
| **Gradient Boosting** | 98.0% ± 2.4% | 98.2% ± 3.6% | 98.2% ± 2.2% | 0.9798 ± 0.0249 | 0.9583 | 1.0000 |

---

## 4. Chronological Test Set Evaluation (2000 – 2018)

- **Accuracy**: **94.74%**
- **Precision**: **88.89%**
- **Recall**: **100.00%**
- **F1-Score**: **94.12%**
- **ROC-AUC**: **1.0000**
- **Probability Brier Score**: **0.0364**
- **Confusion Matrix**:
  ```text
  [[10  1]
   [ 0  8]]
  ```

---

## 5. Controlled Sensitivity & Probability Responsiveness

* **Monsoon Rain 1,200 mm** $\longrightarrow$ $P(\text{Flood}) = \mathbf{4.0\%}$ $\rightarrow$ **`LOW RISK`**
* **Monsoon Rain 1,800 mm** $\longrightarrow$ $P(\text{Flood}) = \mathbf{5.0\%}$ $\rightarrow$ **`LOW RISK`**
* **Monsoon Rain 2,400 mm** $\longrightarrow$ $P(\text{Flood}) = \mathbf{35.6\%}$ $\rightarrow$ **`LOW RISK`**
* **Monsoon Rain 3,000 mm** $\longrightarrow$ $P(\text{Flood}) = \mathbf{95.6\%}$ $\rightarrow$ **`SEVERE RISK`**
* **Monsoon Rain 3,600 mm** $\longrightarrow$ $P(\text{Flood}) = \mathbf{95.9\%}$ $\rightarrow$ **`SEVERE RISK`**

---

## 6. Execution Quickstart Commands

```bash
# 1. Run rigorous audit, temporal training, and calibration
python rigorous_audit_and_train.py

# 2. Run automated PyTest test suite (8 tests)
python -m pytest tests/test_model_and_api.py

# 3. Launch production FastAPI backend
python -m uvicorn app:app --port 8000 --reload

# 4. In a separate terminal, launch telemetry feed simulator
python simulate_feed.py
```
