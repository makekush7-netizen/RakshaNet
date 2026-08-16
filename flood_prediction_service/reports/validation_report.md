# RakshaNet Model Validation & Threshold Selection Report

## 1. Temporal Partitioning (1901 – 2018 / 118 Samples)
- **Train Split (70%)**: 1901 – 1982 (82 years / 82 samples) | Target Rate: 57.3%
- **Validation Split (15%)**: 1983 – 1999 (17 years / 17 samples) | Target Rate: 29.4%
- **Untouched Test Split (15%)**: 2000 – 2018 (19 years / 19 samples) | Target Rate: 42.1%

## 2. Model Comparison Table (Validation & Cross-Validation)

| Model Algorithm | 5-Fold CV Accuracy | 5-Fold CV Recall | 5-Fold CV F1-Score | 5-Fold CV ROC-AUC | Val ROC-AUC | Val PR-AUC | Val Brier Score |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Dummy Baseline** | 52.5% ± 0.0% | 100.0% ± 0.0% | 68.8% ± 0.0% | 0.5000 ± 0.0000 | 0.5000 | 0.5000 | 0.2855 |
| **Logistic Regression** | 97.0% ± 4.0% | 96.4% ± 7.3% | 97.0% ± 4.0% | 0.9960 ± 0.0081 | 1.0000 | 1.0000 | 0.0480 |
| **Calibrated Random Forest** | **97.0% ± 2.4%** | **98.2% ± 3.6%** | **97.2% ± 2.3%** | **1.0000 ± 0.0000** | **1.0000** | **1.0000** | **0.0286** |
| **Gradient Boosting** | 98.0% ± 2.4% | 98.2% ± 3.6% | 98.2% ± 2.2% | 0.9798 ± 0.0249 | 0.9583 | 0.9653 | 0.0588 |

## 3. Threshold Selection Methodology (Validation Set)
- Target: Minimize False-Negative Rate ($FN\_Rate = 1 - Recall$) for flood warning life safety.
- Optimal Derived Threshold: **`0.60`**
- Validation Recall at `0.60`: **100.00%** (False Negative Rate = **0.00%**)
- Validation Precision at `0.60`: **100.00%**

## 4. Final Untouched Temporal Test Set Performance (2000 – 2018)
- **Accuracy**: **94.74%**
- **Precision**: **88.89%**
- **Recall**: **100.00%** (Zero missed flood events across 18 years)
- **F1-Score**: **94.12%**
- **ROC-AUC**: **1.0000**
- **PR-AUC**: **1.0000**
- **Brier Calibration Score**: **0.0364**
- **Confusion Matrix**: `[[10, 1], [0, 8]]`
