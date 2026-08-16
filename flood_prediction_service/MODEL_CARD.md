# RakshaNet Flood Prediction — Final Model Card

## 1. Model Details
- **Model Name**: RakshaNet Calibrated Random Forest Flood Risk Model
- **Version**: 4.0.0
- **Model Architecture**: `CalibratedClassifierCV(estimator=Pipeline([('scaler', StandardScaler()), ('clf', RandomForestClassifier(n_estimators=100, max_depth=4, class_weight='balanced'))]), method='sigmoid', cv=3)`
- **Target Variable**: `FLOODS` (`YES` = 1, `NO` = 0)
- **Model Classes**: `[0, 1]` (0 = No Flood, 1 = Flood Occurred)

## 2. Dataset & Provenance
- **Dataset Name**: India Meteorological Department (IMD) Kerala Historical Flood Dataset (`kerala.csv`)
- **Source**: `https://raw.githubusercontent.com/amandp13/Flood-Prediction-Model/master/kerala.csv`
- **Observations**: 118 historical annual observations (1901 – 2018)
- **Target Distribution**: 60 Flood Years (50.8%) vs 58 Non-Flood Years (49.2%)
- **Data Provenance**: 100% authentic IMD precipitation station gauge observations and monthly aggregations. Excluded proxy leakage features (`historical_floods`) and synthetic post-event features (`water_level_m`, `river_discharge_m3s`).

## 3. Feature Schema (9 Authoritative Features)
1. `annual_rainfall_mm` (Annual precipitation total in mm)
2. `monsoon_rainfall_mm` (Monsoon precipitation total: `JUN` + `JUL` + `AUG` + `SEP`)
3. `pre_monsoon_mm` (Pre-monsoon total: `MAR` + `APR` + `MAY`)
4. `peak_month_rainfall_mm` (Max monthly rainfall in mm)
5. `monsoon_ratio` (`monsoon_rainfall_mm` / `annual_rainfall_mm`)
6. `JUN` (June rainfall in mm)
7. `JUL` (July rainfall in mm)
8. `AUG` (August rainfall in mm)
9. `SEP` (September rainfall in mm)

## 4. Validation Methodology & Metrics

### Chronological Temporal Split (Non-Shuffled)
- **Train Set (70%)**: 1901 – 1982 (82 years / 82 samples)
- **Validation Set (15%)**: 1983 – 1999 (17 years / 17 samples)
- **Untouched Test Set (15%)**: 2000 – 2018 (19 years / 19 samples)

### Performance Summary (Untouched 2000–2018 Test Set)
- **Accuracy**: **94.74%**
- **Precision**: **88.89%**
- **Recall**: **100.00%** (Zero missed flood events)
- **F1-Score**: **94.12%**
- **ROC-AUC**: **1.0000**
- **Brier Calibration Score**: **0.0364**
- **Confusion Matrix**: `[[10, 1], [0, 8]]`

### 5-Fold Stratified Cross-Validation
- **CV Accuracy**: 97.0% ± 2.4%
- **CV Recall**: 98.2% ± 3.6%
- **CV F1-Score**: 97.2% ± 2.3%
- **CV ROC-AUC**: 1.0000 ± 0.0000

## 5. Threshold Methodology
Operational threshold ($t = 0.60$) derived on Validation set (1983–1999) to minimize False Negatives for life-safety warnings.

## 6. Sensitivity Analysis
- 1,200 mm Rain $\rightarrow$ Flood Prob = **4.1%** $\rightarrow$ `LOW`
- 1,800 mm Rain $\rightarrow$ Flood Prob = **4.0%** $\rightarrow$ `LOW`
- 2,400 mm Rain $\rightarrow$ Flood Prob = **7.6%** $\rightarrow$ `LOW`
- 3,000 mm Rain $\rightarrow$ Flood Prob = **88.6%** $\rightarrow$ `SEVERE`
- 3,600 mm Rain $\rightarrow$ Flood Prob = **95.3%** $\rightarrow$ `SEVERE`

## 7. Known Limitations
Dataset is based on 118 annual station observations for the Kerala sub-division. Future expansions to daily gauge station networks will allow intraday hydrograph forecasting.
