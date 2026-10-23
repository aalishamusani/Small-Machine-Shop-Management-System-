# Small Machine Shop Management System (SMSMS)

An Android app and REST backend that digitise the day-to-day work of a small
machine shop: customer orders split into production batches, quality checks,
payments, and a **sales forecast** and **defect anomaly detection** built on
the shop's own history.

Built as a college mini-project (B.Tech CSE – AI & ML, semester 5) by a team of
three students.

## What it does

| Module | What you can do |
|---|---|
| **Orders & batches** | Create an order (total amount is calculated on the server from rate × quantity), split it into batches, move batches through *pending → in progress → completed*, close the order when everything is done. |
| **Quality control** | Log inspections per batch (quantity checked, defects). Pass/fail is derived from the defect count, and total quantity checked can never exceed the batch size. |
| **Anomaly detection** | Flags unusually high defect rates with a z-score rule (mean + 2 standard deviations) so the Plant Head can see which batches need attention. |
| **Sales forecasting** | Compares three time-series models on the shop's monthly sales, picks the best by RMSE, and forecasts the next three months. The Plant Head can add each finished month's real sales in the app and re-run the forecast. |
| **Accounts** | Record payments per order; amount due, paid and outstanding are calculated automatically. |
| **Roles & security** | Three roles (Plant Head, Production Head, Quality Head), bcrypt password hashing, JWT login, and role checks enforced on the server — not just hidden buttons in the app. |

## Architecture

```
Android app (Kotlin, Jetpack Compose)
        │  HTTPS + JWT
        ▼
Node.js / Express REST API ──────────────▶ PostgreSQL
   (role-checked on the server)                ▲
        │ starts on demand                     │ reads / writes
        ▼                                      │
Python scripts in ml/  (forecasting, anomaly detection)
```

- **Android:** Kotlin, Jetpack Compose, Retrofit/OkHttp, a hand-drawn forecast chart.
- **Backend:** Node.js, Express 5, `pg`, `bcryptjs`, `jsonwebtoken`.
- **Database:** PostgreSQL (schema in `database/`).
- **ML:** Python, pandas, NumPy, scikit-learn, statsmodels.
- **Deployment:** Docker image on Render (free tier) with a Neon Postgres database — see [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md).

## The forecasting approach

The data is 13 months of the shop's real monthly dispatch value, consolidated
from raw Excel exports that came in three different layouts (see
`data/SMSMS_EDA_SalesHistory.ipynb`; the values in this public repository are
scaled so the real figures are not exposed). One year of history is enough for
a trend but not for proving seasonality, so none of the models assume it.

1. **Moving average** (3 months) — the baseline every other model must beat.
2. **Linear regression** on time.
3. **Holt's linear trend** (exponential smoothing).

Models are evaluated with **walk-forward validation** (train on the past,
predict the next month, repeat) rather than a random split, which would let a
model see the future. The winner is chosen by RMSE; on this data the simple
moving average was best. With only a few evaluation folds the error estimates
are rough, and the app shows MAE/RMSE next to the forecast so users know how
much to trust it.

## Repository layout

```
android/        Android app (Kotlin + Jetpack Compose)
backend/        Express API, controllers, role middleware, DB loader scripts
ml/             train_forecast.py, anomaly_detection.py
database/       PostgreSQL schema and migrations
data/           Monthly sales history (scaled) and the EDA notebook
sample_data/    Anonymised stand-ins for the shop's raw Excel exports
docs/           Brief, architecture and ER diagrams, deployment guide
Dockerfile, render.yaml   Cloud deployment
```

## Running it locally

**Prerequisites:** Node.js 20+, Python 3.11+, PostgreSQL 15+, Android Studio.

```bash
# 1. Database: create an empty Postgres database and a user for it.

# 2. Backend settings: copy the example and fill in your own values
cp backend/.env.example backend/.env        # also: cp ml/.env.example ml/.env
npm --prefix backend install

# 3. Python environment for the ML scripts
cd ml && python -m venv .venv && .venv/Scripts/pip install -r requirements.txt && cd ..

# 4. Create tables, 3 users and demo data in the EMPTY database.
#    Passwords are generated and saved to a file in your home folder.
cd backend && node scripts/setupDeployedDatabase.js --yes

# 5. Start the API
npm start        # http://localhost:4000/health
```

Open `android/` in Android Studio and run it on the emulator (it talks to
`10.0.2.2:4000`, the emulator's alias for your PC). For a real phone, build with
`./gradlew :app:assembleDebug -PsmsmsDebugBaseUrl=http://<your-PC-address>:4000/`.

## Security notes

- Passwords are bcrypt-hashed; logins use signed JWTs; every route except login requires a token.
- Secrets live in `.env` files (git-ignored) or the hosting dashboard, never in the repository.
- Known limitations: no login rate limiting or token revocation yet.

## Data note

The demo orders and quality checks are made up. The raw Excel exports of the
real shop are **not** included; `sample_data/` holds anonymised look-alikes, and
the monthly sales series is scaled by a hidden constant, so absolute values here
are not the shop's real figures.

## Status

A working prototype. Still to do: automated tests (unit and role-based access
tests), formal validation of anomaly detection on real defect data, role
dashboards and owner reports, and machine scheduling (deliberately out of scope
for this version).
