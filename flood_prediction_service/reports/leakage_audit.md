# RakshaNet Target Leakage & Data Provenance Audit Report

## 1. Feature Provenance & Leakage Matrix

| Feature | Source | Raw/Derived | Required | Pre-Event Available | Used by Model | Leakage Risk | Transformation |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `annual_rainfall_mm` | IMD Station Record | Raw | Yes | Yes | Yes | None | Sum of monthly gauge measurements |
| `monsoon_rainfall_mm` | Aggregation | Derived | Auto | Yes | Yes | None | `JUN` + `JUL` + `AUG` + `SEP` |
| `pre_monsoon_mm` | Aggregation | Derived | Auto | Yes | Yes | None | `MAR` + `APR` + `MAY` |
| `peak_month_rainfall_mm` | Aggregation | Derived | Auto | Yes | Yes | None | Max(`JUN`, `JUL`, `AUG`, `SEP`) |
| `monsoon_ratio` | Ratio | Derived | Auto | Yes | Yes | None | `monsoon_rainfall_mm` / `annual_rainfall_mm` |
| `JUN`, `JUL`, `AUG`, `SEP` | IMD Rain Gauge | Raw | Optional | Yes | Yes | None | Monthly gauge reading (mm) |

## 2. Excluded Features Audit & Rationale

- **`historical_floods`**: EXCLUDED. In small historical sub-division datasets, past flood counts can encode label target proxies or historical prior biases. Excluded from training matrix.
- **`water_level_m`**: EXCLUDED. Synthetic rule-derived variable (`monsoon / 350`) previously created for UI simulation. Removed from ML training.
- **`river_discharge_m3s`**: EXCLUDED. Synthetic rule-derived variable (`monsoon * 1.85`). Removed from ML training.
- **`population_density`, `elevation_m`, `latitude`, `longitude`, `land_cover`, `soil_type`, `infrastructure`**: EXCLUDED. Zero signal / constant in single sub-division dataset.

## 3. Preprocessing Split Safeguards
All transformations (`StandardScaler`, `prepare_features`) are fitted exclusively on training splits (`X_train`) to prevent data leakage into validation or temporal test sets.
