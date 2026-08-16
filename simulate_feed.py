import requests
import time
import random
import json

API_URL = "http://127.0.0.1:8000/predict"

SCENARIOS = {
    "NORMAL": [1400.0, 1600.0, 1850.0, 1900.0, 2100.0],
    "HEAVY": [2500.0, 2650.0, 2750.0, 2850.0, 2950.0],
    "EXTREME": [3200.0, 3450.0, 3600.0, 3800.0, 4100.0]
}

def run_simulation(scenario: str = "ALL", iterations: int = 15, delay_sec: float = 1.0):
    print("=" * 80)
    print(" RAKSHANET SIMULATED LIVE TELEMETRY FEED GENERATOR ")
    print("=" * 80)
    print(f"Target Endpoint: {API_URL}")
    print(f"Scenario Mode:   {scenario}")
    print(f"Iterations:      {iterations}\n")

    for i in range(iterations):
        if scenario == "ALL":
            scen_type = random.choice(["NORMAL", "HEAVY", "EXTREME"])
        else:
            scen_type = scenario if scenario in SCENARIOS else "NORMAL"
            
        rain_val = random.choice(SCENARIOS[scen_type])
        
        payload = {
            "region_id": f"region_kerala_station_{random.randint(101, 108)}",
            "rainfall_mm": float(rain_val),
            "timestamp": int(time.time())
        }
        
        try:
            resp = requests.post(API_URL, json=payload, timeout=5.0)
            if resp.status_code == 200:
                data = resp.json()
                print(f"[{i+1:02d}/{iterations:02d}] 📡 Stream Sent -> Region: {data['region_id']:<24} | Rain: {rain_val:>6.1f}mm | Prob: {data['flood_probability']*100:>5.1f}% | Risk: {data['risk_level']}")
            else:
                print(f"[{i+1:02d}/{iterations:02d}] ⚠️ HTTP Error {resp.status_code}: {resp.text}")
        except Exception as e:
            print(f"[{i+1:02d}/{iterations:02d}] ❌ Connection Failed: {e}")
            
        time.sleep(delay_sec)
        
    print("\n[OK] Simulation batch completed successfully.")

if __name__ == "__main__":
    run_simulation(scenario="ALL", iterations=10, delay_sec=0.5)
