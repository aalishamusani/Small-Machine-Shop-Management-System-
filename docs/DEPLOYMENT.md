# Deploying SMSMS to the cloud (free) — step by step

**What you end up with:** the app on your phone works from anywhere, with your
laptop switched off.

```
 Phone (APK)  ──https──▶  Render (free web server: Node + Python ML)  ──▶  Neon (free Postgres database)
```

| Part | Service | Cost |
|---|---|---|
| Database | [Neon](https://neon.com) | free plan |
| Backend + ML scripts | [Render](https://render.com) free web service, built from this GitHub repo using the `Dockerfile` | free plan |
| The app | an `.apk` file you install on the phone | free |

> **Never paste passwords or database addresses into chat.** Type or paste them
> only into the Neon/Render websites and your own PowerShell window.

---

## Part 1 — Put the deployment files on GitHub (Claude does this)

Render builds from the GitHub repo, so the `Dockerfile`, `render.yaml` and the
setup script must be pushed first. *Status: ask Claude to "commit and push the
deployment files".*

## Part 2 — Create the database on Neon (about 5 minutes)

1. Go to **neon.com** and sign up (easiest: **Continue with GitHub**).
2. Create a project: name it `smsms`, pick the Postgres version offered, and choose
   the region **closest to you** (e.g. Singapore).
3. Open the project's **Connect** / **Connection details** panel.
4. **Turn OFF "Connection pooling"** (so the address has no `-pooler` in it). You need
   the *direct* connection.
5. Note these four things (keep the page open, don't share them):
   - **Host** (looks like `ep-something.region.aws.neon.tech`)
   - **Database** name (often `neondb`)
   - **User** (often `neondb_owner`)
   - **Password** (use the "show" / "copy" button)

## Part 3 — Load your tables, users and demo data into Neon

Open **PowerShell** and run these lines **one at a time**, replacing the
`PASTE-...` parts with the Neon values from Part 2:

```powershell
cd "C:\Users\HP\OneDrive\Documents\Mini Project Sales Forecasting\backend"
$env:PGHOST     = "PASTE-HOST"
$env:PGPORT     = "5432"
$env:PGDATABASE = "PASTE-DATABASE"
$env:PGUSER     = "PASTE-USER"
$env:PGPASSWORD = "PASTE-PASSWORD"
$env:PGSSLMODE  = "require"
node scripts/setupDeployedDatabase.js --yes
```

What to check:
- The second line of output says **`Target database: ... on ep-...neon.tech`**.
  If it says `127.0.0.1`, it is pointing at your laptop — stop and tell Claude.
  (The script refuses to touch a database that already has orders, so your local
  data is safe.)
- At the end it prints `Done.`, "9 demo orders loaded", the forecast, and
  "Flagged 4 as outliers".

The script creates 3 logins (`plant_head`, `production_head`, `quality_head`) with
**new random passwords**. They are saved in
`C:\Users\HP\SMSMS_private_data\deployed-credentials.txt` — outside OneDrive and
outside git. Open that file to see them. **Then close the PowerShell window**
(this also forgets the database password you typed).

## Part 4 — Start the backend on Render (about 10 minutes, mostly waiting)

1. Go to **render.com** and sign up with GitHub. When asked, allow Render to see
   the `Mini_Project_Sales_Forecasting` repo (it's private; you can limit access
   to just that repo).
2. Click **New +** → **Blueprint**, choose this repo. Render reads `render.yaml`.
3. It will ask for the four values marked secret: **PGHOST**, **PGDATABASE**,
   **PGUSER**, **PGPASSWORD**. Enter the same Neon values as in Part 3.
   (`JWT_SECRET` is generated for you; the other settings are filled in.)
4. Click **Apply / Deploy**. The first build takes a few minutes (it installs the
   Python libraries). Watch the **Logs** tab; when it says the service is live,
   copy its address, which looks like `https://smsms-backend-xxxx.onrender.com`.
5. Test it: open `https://smsms-backend-xxxx.onrender.com/health` in a browser. You
   should see `{"status":"ok"}`.

> If Render asks for a payment card before letting you use the free plan, stop and
> tell Claude. Free instances should not charge you, but account rules can change.

## Part 5 — Build the phone app (APK) for your live address

Ask Claude to do this, or run it yourself (replace the address, keep the trailing `/`):

```bash
cd android
./gradlew.bat :app:assembleRelease -PsmsmsBaseUrl=https://smsms-backend-xxxx.onrender.com/
```

The finished file is at
`C:\Users\HP\.gradle-builds\smsms-android\app\outputs\apk\release\app-release.apk`.
(This "release" build only talks over secure `https`, so it won't work with your
laptop — that's intentional.)

## Part 6 — Install on the phone

1. Copy the APK to the phone (USB cable, Google Drive, Telegram or email).
2. Tap it and allow "install from this source" once. If Play Protect warns that the
   app is unknown, choose **Install anyway** (it's your own app, not from the Play Store).
3. Log in with the usernames and passwords from `deployed-credentials.txt`.

## Part 7 — Before you demo (important!)

- **Free servers go to sleep** after about 15 minutes without use, and wake up in
  30–60 seconds on the next request. **Open the `/health` address in a browser 2 minutes
  before the demo**, and again if it has been idle for a while.
- Tap **Forecast** and **Anomalies** once beforehand so the slow first run is done.
- On the Forecast screen you can show **"Add Aug 2026 sales"** live (needs the real
  total for August in rupees, for example `3200000`). Use **Undo last entry** to
  remove it afterwards if you want a clean state.
- The demo data (9 orders, 30 quality checks, 4 flagged anomalies) is **made-up test
  data**, not the shop's real data.

## If something goes wrong

| What you see | Likely cause | What to do |
|---|---|---|
| App says "could not reach the server" | The free server is asleep | Open the `/health` address in the phone browser, wait up to a minute, retry |
| `/health` never loads | Deploy failed or still building | Render → your service → **Logs** |
| Login says invalid username or password | Wrong password | Re-read `deployed-credentials.txt`; passwords are case-sensitive |
| Forecast screen shows an error | Database or Python problem | Render → **Logs**, copy the error to Claude (not passwords) |
| App works on Wi-Fi but not mobile data | It shouldn't differ | Check the phone's internet; try the `/health` address in Chrome |

## Security notes (mention these if asked)

- Passwords are stored as bcrypt hashes, logins use signed JWT tokens, and role
  checks are enforced on the server.
- The database password and JWT secret live only in Render's settings, never in GitHub.
- Known limits: there is no login rate limiting or token revocation yet, so don't
  leave it running publicly long-term without those.

## After the demo — shutting it down

- **Render:** open the service → **Settings** → **Suspend** (or delete) it.
- **Neon:** delete the project (or just leave it; free plans cost nothing).
- Delete `deployed-credentials.txt` when you no longer need it.
