# Flood Prediction Service — Handoff

**For:** teammate building the ML flagging service + his coding agent
**Scope:** a deployed model that flags flood risk per region, plus a simulated
live-data feed for demo purposes, plus a small API contract so the Android
mesh app (built separately) can consume its output. This doc does not cover
the mesh app itself — see it as a black box you're producing output for.

---

## 0. Context (why this exists, what NOT to over-build)

This is one piece of a hackathon submission (RakshaNet). The Android app has
a working offline BLE/Nearby mesh already built and tested on real hardware.
This service's job is narrow: **given some regional flood-relevant data,
output a flag (region + severity) that the mesh app turns into a broadcast
guidance message.** It does not need to be state-of-the-art, does not need
live real sensor feeds, and should not become a multi-day deep learning
project. A simple, honestly-scoped, correctly-deployed classifier beats an
ambitious model that isn't finished or can't be explained clearly to judges.

**Timeline:** this needs to be usable in a demo in a few days. Bias every
decision toward "simple and working" over "sophisticated and risky."

---

## 1. Model

- **Use classical ML, not deep learning.** Logistic regression or a random
  forest (scikit-learn) on tabular flood-relevant features. Trains in
  minutes on a laptop, easy to explain to judges, easy to debug.
- **Public dataset options** (pick one, don't combine multiple hastily):
  - Kaggle has several "Flood Prediction Dataset" style datasets with
    features like rainfall, river level, humidity, soil moisture, etc. —
    search Kaggle directly, several are ready-to-use CSVs.
  - For an India-relevant angle (nice for the pitch, not required): rainfall
    and river-gauge data is available via **data.gov.in** and **India-WRIS**,
    though it takes more cleaning than a Kaggle CSV. Only go this route if
    there's clearly enough time; a clean Kaggle dataset is the safer choice
    under time pressure.
- **Target output:** a binary or 3-level (low/moderate/severe) flood-risk
  classification per input row, not a raw probability score dumped on the
  judges without interpretation.
- **Be honest about what this is in the pitch.** This is a model trained on
  historical/public data, not live sensor telemetry. Say that plainly if
  asked — an honestly-scoped claim survives a follow-up question, an
  overclaimed one doesn't.

---

## 2. Training data vs. simulated "live" inference data — direct answer

You asked whether the training data can be reused as the simulated live
input, or whether new data is needed. Direct answer: **don't feed the model
its own training rows back as "live" demo input** — that's not simulating
anything, it's just replaying memorized answers and proves nothing if
someone asks a pointed question about it. Two better options, either is
fine:

1. **Hold out a test split before training** (standard `train_test_split`,
   e.g. 80/20) and use the **held-out rows** as the "simulated live feed" for
   demo purposes. The model has never seen these rows — this is an honest,
   standard practice and also doubles as your accuracy evidence.
2. **Synthetically perturb real rows** (small random jitter on rainfall/
   river-level values within realistic ranges) to generate a stream of
   "new-looking" inputs for the live demo. Slightly more work, looks more
   dynamic in a demo (numbers visibly changing over time), but not required
   — option 1 is enough and is more defensible if questioned.

Either way, build a small script (`simulate_feed.py` or similar) that plays
these rows into the deployed endpoint on a timer (e.g. every 5-10 seconds)
during the demo, rather than requiring someone to manually trigger each
inference call live. This is what "simulated live inferencing" should
concretely mean here — a scripted, repeatable, demo-safe input stream, not
an actual live sensor integration (that's out of scope, and pretending
otherwise on stage is a risk, not a feature).

---

## 3. Deployment

Pick whichever is fastest for you, no need to over-research:

- **Hugging Face Spaces + Gradio or FastAPI** — free tier, minimal infra
  setup, gives a live public HTTPS endpoint quickly. Probably the fastest
  path if you haven't deployed something like this before.
- **Render or Railway free tier** — fine alternative if you'd rather write
  a plain FastAPI app without a Gradio UI layer.

Whichever you pick, the deployed service just needs to expose one HTTP
endpoint that accepts input features and returns a flood-risk flag (see
contract below). Keep it that simple — no auth complexity, no extra
endpoints, unless there's spare time at the very end.

---

## 4. API contract (this is what the mesh app will consume)

Keep this exact shape unless you coordinate a change — the Android side will
be built against it.

**Request** (`POST /predict`):
```json
{
  "region_id": "string",
  "rainfall_mm": 0.0,
  "river_level_m": 0.0,
  "soil_moisture_pct": 0.0,
  "timestamp": 1234567890
}
```
(Adjust the specific feature fields to match whatever dataset you actually
train on — the important part is that it's a flat JSON object of numeric/
string features, not the exact field names.)

**Response:**
```json
{
  "region_id": "string",
  "risk_level": "LOW | MODERATE | SEVERE",
  "confidence": 0.0,
  "timestamp": 1234567890
}
```

- `risk_level` is what actually matters downstream — the mesh app maps this
  directly to which pre-written guidance template gets broadcast (severity
  tiers, not raw probability). Keep it to these three values unless there's
  a strong reason to add more.
- `confidence` is nice to have for a judge-facing dashboard/graph but is not
  required for the mesh integration to work — don't block on making this
  well-calibrated.

---

## 5. What the other side does with this output (context, not your task)

So you understand where this plugs in: the mesh app will read `risk_level`
per region and, when it crosses a threshold, package a new signed packet
type (`GUIDANCE_BROADCAST`) carrying a **pre-written, human-authored**
guidance message matched to that hazard/severity tier — not a live LLM call
at broadcast time, since the mesh needs to work fully offline. Your service
is the trigger/flag, not the message author. You don't need to build any of
that broadcast logic — just make sure `risk_level` is a clean, reliably
returned field.

---

## 6. Deliverables checklist

- [ ] Dataset chosen, cleaned, train/test split done (test split kept aside
      for the live-demo simulation — see §2).
- [ ] Model trained (scikit-learn, not deep learning), reasonable accuracy
      on the test split, briefly noted somewhere (a couple of sentences is
      enough — "X% accuracy on held-out data" is a fine thing to say to
      judges).
- [ ] Model deployed with a working public HTTPS endpoint matching the
      contract in §4.
- [ ] `simulate_feed.py` (or equivalent) that plays held-out/perturbed rows
      into the endpoint on a timer, producing a visible stream of
      region/risk_level outputs for the demo.
- [ ] Endpoint tested end-to-end: can call it and reliably get back valid
      `risk_level` values, including at least one row that should trigger
      each tier (LOW/MODERATE/SEVERE), so the demo can show varied output
      rather than the same flag every time.
- [ ] Share the live endpoint URL and confirm the exact response field names
      actually match §4 (or flag the deviation) before mesh-app integration
      starts.

---

## 7. What not to spend time on

- No deep learning / neural nets — unnecessary complexity and risk for this
  timeline and use case.
- No real live sensor integration — simulated feed only, this is a demo.
- No auth/rate-limiting/production hardening on the endpoint — it just needs
  to work reliably during the demo window.
- No scope creep into multi-hazard models (earthquake, storm) unless flood
  is fully done with time to spare — flood is the one being demoed live.
