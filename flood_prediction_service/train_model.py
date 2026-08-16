import numpy as np
import pandas as pd
import requests
import json
import joblib
import os
import time

from sklearn.dummy import DummyClassifier
from sklearn.model_selection import StratifiedKFold, TimeSeriesSplit, cross_val_score
from sklearn.preprocessing import StandardScaler
from sklearn.pipeline import Pipeline
from sklearn.linear_model import LogisticRegression
from sklearn.ensemble import RandomForestClassifier, GradientBoostingClassifier
from sklearn.calibration import CalibratedClassifierCV
from sklearn.metrics import (
    accuracy_score, precision_score, recall_score, f1_score, roc_auc_score,
    average_precision_score, confusion_matrix, classification_report, brier_score_loss
)

from features import FEATURE_COLS, prepare_features, FeatureEngineer
from config import Config

SEED = 42
np.random.seed(SEED)

KERALA_DATASET_URL = "https://raw.githubusercontent.com/amandp13/Flood-Prediction-Model/master/kerala.csv"

def train_and_evaluate():
    print("=" * 80)
    print(" RAKSHANET PRODUCTION ML TRAINING & EVALUATION PIPELINE ")
    print("=" * 80)
    
    os.makedirs("data", exist_ok=True)
    os.makedirs("models", exist_ok=True)
    os.makedirs("reports", exist_ok=True)
    
    local_csv = Config.DATASET_PATH
    print(f"1. Loading dataset from: {local_csv}")
    if not os.path.exists(local_csv):
        df_raw = pd.read_csv(KERALA_DATASET_URL)
        df_raw.to_csv(local_csv, index=False)
    else:
        df_raw = pd.read_csv(local_csv)
        
    print(f"   - Total Historical Observations: {len(df_raw)} (1901 - 2018)")
    
    # Sort chronologically by YEAR
    df_raw = df_raw.sort_values("YEAR").reset_index(drop=True)
    df_raw['target'] = (df_raw['FLOODS'] == 'YES').astype(int)
    
    print("\n2. Engineering Features using features.py...")
    df_features = prepare_features(df_raw)
    X = df_features[FEATURE_COLS]
    y = df_raw['target']
    
    print(f"   - Authoritative Features ({len(FEATURE_COLS)}): {FEATURE_COLS}")
    print(f"   - Target Distribution: 1={y.sum()} ({y.mean()*100:.1f}%), 0={(1-y.mean())*len(y):.0f} ({(1-y.mean())*100:.1f}%)")
    
    # Chronological Split
    n_total = len(df_raw)
    n_train = int(n_total * 0.70)  # 82 samples (1901 - 1982)
    n_val = int(n_total * 0.15)    # 17 samples (1983 - 1999)
    # n_test = 19 samples (2000 - 2018)
    
    X_train = X.iloc[:n_train]
    y_train = y.iloc[:n_train]
    df_train = df_raw.iloc[:n_train]
    
    X_val = X.iloc[n_train:n_train+n_val]
    y_val = y.iloc[n_train:n_train+n_val]
    df_val = df_raw.iloc[n_train:n_train+n_val]
    
    X_test = X.iloc[n_train+n_val:]
    y_test = y.iloc[n_train+n_val:]
    df_test = df_raw.iloc[n_train+n_val:]
    
    print("\n3. Temporal Non-Shuffled Partitioning:")
    print(f"   - Train Split:      {len(X_train)} samples ({df_train['YEAR'].min()} - {df_train['YEAR'].max()})")
    print(f"   - Validation Split: {len(X_val)} samples ({df_val['YEAR'].min()} - {df_val['YEAR'].max()})")
    print(f"   - Untouched Test:   {len(X_test)} samples ({df_test['YEAR'].min()} - {df_test['YEAR'].max()}) — UNTOUCHED")
    
    # Model Comparison
    print("\n4. Model Comparison on Leakage-Safe Splits:")
    X_train_val = X.iloc[:n_train+n_val]
    y_train_val = y.iloc[:n_train+n_val]
    
    cv_strat = StratifiedKFold(n_splits=5, shuffle=True, random_state=SEED)
    
    candidate_models = {
        "Dummy Baseline": DummyClassifier(strategy='prior'),
        "Logistic Regression": LogisticRegression(max_iter=1000, random_state=SEED),
        "Random Forest": RandomForestClassifier(n_estimators=100, max_depth=4, random_state=SEED, class_weight='balanced'),
        "Gradient Boosting": GradientBoostingClassifier(n_estimators=100, max_depth=3, random_state=SEED)
    }
    
    comp_results = {}
    for name, clf in candidate_models.items():
        pipe = Pipeline([('scaler', StandardScaler()), ('clf', clf)])
        
        acc_scores = cross_val_score(pipe, X_train_val, y_train_val, cv=cv_strat, scoring='accuracy')
        rec_scores = cross_val_score(pipe, X_train_val, y_train_val, cv=cv_strat, scoring='recall')
        f1_scores = cross_val_score(pipe, X_train_val, y_train_val, cv=cv_strat, scoring='f1')
        auc_scores = cross_val_score(pipe, X_train_val, y_train_val, cv=cv_strat, scoring='roc_auc')
        
        pipe.fit(X_train, y_train)
        val_probs = pipe.predict_proba(X_val)[:, 1] if hasattr(pipe, "predict_proba") else pipe.predict(X_val)
        val_auc = roc_auc_score(y_val, val_probs) if len(np.unique(y_val)) > 1 and len(np.unique(val_probs)) > 1 else 0.5
        val_pr_auc = average_precision_score(y_val, val_probs) if len(np.unique(y_val)) > 1 and len(np.unique(val_probs)) > 1 else 0.5
        val_brier = brier_score_loss(y_val, val_probs)
        
        comp_results[name] = {
            "cv_accuracy_mean": round(float(np.mean(acc_scores)), 4),
            "cv_accuracy_std": round(float(np.std(acc_scores)), 4),
            "cv_recall_mean": round(float(np.mean(rec_scores)), 4),
            "cv_recall_std": round(float(np.std(rec_scores)), 4),
            "cv_f1_mean": round(float(np.mean(f1_scores)), 4),
            "cv_f1_std": round(float(np.std(f1_scores)), 4),
            "cv_roc_auc_mean": round(float(np.mean(auc_scores)), 4),
            "cv_roc_auc_std": round(float(np.std(auc_scores)), 4),
            "val_roc_auc": round(float(val_auc), 4),
            "val_pr_auc": round(float(val_pr_auc), 4),
            "val_brier_score": round(float(val_brier), 4)
        }
        print(f"   - {name:<20}: CV Acc={np.mean(acc_scores)*100:.1f}% | CV Recall={np.mean(rec_scores)*100:.1f}% | CV AUC={np.mean(auc_scores):.4f} | Val AUC={val_auc:.4f} | Brier={val_brier:.4f}")
        
    # Select Final Model: Calibrated Random Forest Classifier
    print("\n5. Fitting & Calibrating Final Random Forest Classifier...")
    base_rf = Pipeline([
        ('scaler', StandardScaler()),
        ('clf', RandomForestClassifier(n_estimators=100, max_depth=4, random_state=SEED, class_weight='balanced'))
    ])
    
    calibrated_clf = CalibratedClassifierCV(base_rf, cv=3, method='sigmoid')
    calibrated_clf.fit(X_train, y_train)
    
    # Deriving Operational Threshold on Validation Set
    print("\n6. Deriving Operational Threshold on Validation Set (Minimizing False Negatives)...")
    val_probs_cal = calibrated_clf.predict_proba(X_val)[:, 1]
    
    thresholds_grid = np.linspace(0.10, 0.90, 81)
    best_t = 0.60
    best_fn_rate = 1.0
    best_f1 = 0.0
    
    for t in thresholds_grid:
        preds = (val_probs_cal >= t).astype(int)
        rec = recall_score(y_val, preds, zero_division=0)
        f1 = f1_score(y_val, preds, zero_division=0)
        fn_rate = 1.0 - rec
        
        if rec >= 0.90 and (fn_rate < best_fn_rate or (fn_rate == best_fn_rate and f1 > best_f1)):
            best_fn_rate = fn_rate
            best_f1 = f1
            best_t = round(float(t), 2)
            
    opt_threshold = best_t if best_t != 0.50 else 0.60
    print(f"   - Derived Operational Threshold: {opt_threshold}")
    print(f"   - Validation Recall: {1.0 - best_fn_rate:.4f} (False Negative Rate: {best_fn_rate:.4f})")
    
    # Chronological Temporal Test Evaluation (2000 - 2018 / 19 Samples)
    print("\n7. Final Evaluation on Untouched Chronological Test Set (2000 - 2018)...")
    test_probs = calibrated_clf.predict_proba(X_test)[:, 1]
    test_preds = (test_probs >= opt_threshold).astype(int)
    
    acc_test = float(accuracy_score(y_test, test_preds))
    prec_test = float(precision_score(y_test, test_preds, zero_division=0))
    rec_test = float(recall_score(y_test, test_preds, zero_division=0))
    f1_test = float(f1_score(y_test, test_preds, zero_division=0))
    auc_test = float(roc_auc_score(y_test, test_probs))
    pr_auc_test = float(average_precision_score(y_test, test_probs))
    brier_test = float(brier_score_loss(y_test, test_probs))
    cm_test = confusion_matrix(y_test, test_preds).tolist()
    
    print(f"   - Temporal Test Accuracy:    {acc_test*100:.2f}%")
    print(f"   - Temporal Test Precision:   {prec_test*100:.2f}%")
    print(f"   - Temporal Test Recall:      {rec_test*100:.2f}%")
    print(f"   - Temporal Test F1-Score:    {f1_test*100:.2f}%")
    print(f"   - Temporal Test ROC-AUC:     {auc_test:.4f}")
    print(f"   - Temporal Test PR-AUC:      {pr_auc_test:.4f}")
    print(f"   - Probability Brier Score:  {brier_test:.4f}")
    print(f"   - Confusion Matrix:          {cm_test}")
    
    # Controlled Physical Sanity Experiment
    print("\n8. Controlled Physical Sanity Experiments...")
    median_row = X.median().to_dict()
    sanity_results = []
    for r_val in [1200, 1800, 2400, 3000, 3600]:
        sample = median_row.copy()
        sample['annual_rainfall_mm'] = float(r_val)
        sample['monsoon_rainfall_mm'] = float(r_val * 0.82)
        sample['pre_monsoon_mm'] = float(r_val * 0.08)
        sample['JUN'] = float(r_val * 0.82 * 0.25)
        sample['JUL'] = float(r_val * 0.82 * 0.35)
        sample['AUG'] = float(r_val * 0.82 * 0.25)
        sample['SEP'] = float(r_val * 0.82 * 0.15)
        sample['peak_month_rainfall_mm'] = float(r_val * 0.82 * 0.35)
        sample['monsoon_ratio'] = round(r_val * 0.82 / r_val, 4)
        
        sample_df = pd.DataFrame([sample])[FEATURE_COLS]
        prob = float(calibrated_clf.predict_proba(sample_df)[0][1])
        tier = "SEVERE" if prob >= 0.70 else ("MODERATE" if prob >= 0.40 else "LOW")
        print(f"   - Rainfall {r_val:>4d}mm -> Flood Probability: {prob*100:>5.1f}% | Risk Tier: {tier}")
        sanity_results.append({"rainfall_mm": r_val, "flood_probability": round(prob, 4), "risk_tier": tier})
        
    # Feature Schema JSON
    feature_schema = {
        "expected_features": FEATURE_COLS,
        "feature_count": len(FEATURE_COLS),
        "target": "FLOODS",
        "classes": [0, 1],
        "primary_input": "annual_rainfall_mm",
        "optional_monthly_inputs": ["JUN", "JUL", "AUG", "SEP"]
    }
    
    # Training Metrics JSON
    metrics_json = {
        "model_selected": "Calibrated Random Forest Classifier",
        "random_seed": SEED,
        "chronological_test_metrics": {
            "accuracy": round(acc_test, 4),
            "precision": round(prec_test, 4),
            "recall": round(rec_test, 4),
            "f1_score": round(f1_test, 4),
            "roc_auc": round(auc_test, 4),
            "pr_auc": round(pr_auc_test, 4),
            "brier_score": round(brier_test, 4),
            "confusion_matrix": cm_test
        },
        "model_comparison": comp_results,
        "optimal_threshold": opt_threshold,
        "sanity_tests": sanity_results
    }
    
    # Metadata JSON
    metadata = {
        "model_version": "4.0.0",
        "training_date": time.strftime("%Y-%m-%d %H:%M:%S"),
        "dataset_name": "IMD Kerala Historical Flood Dataset (kerala.csv)",
        "dataset_source": KERALA_DATASET_URL,
        "total_observations": n_total,
        "temporal_splits": {
            "train": f"1901 - 1982 ({len(X_train)} samples)",
            "validation": f"1983 - 1999 ({len(X_val)} samples)",
            "test": f"2000 - 2018 ({len(X_test)} samples)"
        },
        "feature_cols": FEATURE_COLS,
        "target_name": "FLOODS (YES=1, NO=0)",
        "classes": [0, 1],
        "optimal_threshold": opt_threshold,
        "brier_score": round(brier_test, 4),
        "metrics": metrics_json["chronological_test_metrics"],
        "sanity_results": sanity_results
    }
    
    # Export to both root and models/ directory
    print("\n9. Exporting Versioned Artifacts to 'models/' and root directories...")
    for target_dir in [".", "models"]:
        joblib.dump(calibrated_clf, os.path.join(target_dir, "model.pkl"))
        with open(os.path.join(target_dir, "metadata.json"), "w") as f:
            json.dump(metadata, f, indent=2)
        with open(os.path.join(target_dir, "feature_schema.json"), "w") as f:
            json.dump(feature_schema, f, indent=2)
        with open(os.path.join(target_dir, "metrics.json"), "w") as f:
            json.dump(metrics_json, f, indent=2)
            
    df_test.to_csv("heldout_test_data.csv", index=False)
    print("   - Successfully exported model.pkl, metadata.json, feature_schema.json, metrics.json, heldout_test_data.csv")
    print("\nTraining and evaluation pipeline executed successfully!")
    print("=" * 80)

if __name__ == "__main__":
    train_and_evaluate()
