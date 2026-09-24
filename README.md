# RFID Warehouse Platform

A warehouse RFID tracking system: a Next.js/Postgres web app (dashboard + API) and a Kotlin Android app for the Chainway C5 E710 handheld reader.

## Repository layout

```
web/                          Next.js 16 (TypeScript), Prisma, PostgreSQL, React dashboard
android/warehouse-app/        Kotlin Android app for the Chainway C5 E710
```

## What's implemented

- **Database**: PostgreSQL `rfid_warehouse`, schema in `web/prisma/schema.prisma` — `products`, `activities` (per your original SQL), plus `users` (web login) and `scan_batches` (idempotency for Android uploads).
- **Web app** (`web/`): login-protected dashboard with stock/activity charts, searchable Products table, filterable Activities log, product detail pages, and a device-facing API (`/api/device/*`) authenticated by API key for the Android app.
- **Android app** (`android/warehouse-app/`): colorway home screen (gradient header, accent-colored action buttons) with pull-to-refresh KPI cards and MPAndroidChart graphs (stock by status, 7-day activity), Settings screen (Server URL / API Key / Power only), and two entry points into one shared Scan screen (Start/Pause/Send): **Tag Registration** (registers a new EPC to a product — SKU/Name entered per tag) and **Record Activity** (a single button that opens a picker for Stock Opname / Transfer / Outbound). Location is a dropdown of known warehouse locations with a manual "+ Add new location" fallback. During Record Activity, each newly scanned tag is looked up against the database live and the list shows Product Name / Qty / Location directly (or "Not registered" for unknown tags) — not raw RSSI. Reader ID / Operator Name fields were removed per your request.
- **Business rules** (see `C:\Users\Admin\.claude\plans\enumerated-scribbling-hinton.md` for the full table): inbound creates a product; stock_opname corrects location; transfer updates location; outbound marks `sold`. Unknown/duplicate/already-sold EPCs are rejected with a reason, verified end-to-end against the live database.

## Local environment

- `web/.env` holds local secrets (DB connection string, session secret, device API key, admin login) and is gitignored — never commit it. Copy `web/.env.example` to `web/.env` and fill in your own values, then run `npm run db:seed` to seed the admin user.
- The Android app (`android/warehouse-app/`) talks directly to Supabase using the client credentials in `SupabaseModule.kt` rather than through `web/`'s API — see that file for the current connection setup.

## Running the web app

```powershell
cd web
npm install
npm run dev
```

Open http://localhost:3000 and sign in with the credentials above. Production build: `npm run build && npm run start`.

## Running the Android app

1. Open `android/warehouse-app/` in Android Studio (or build from CLI — see below).
2. On first launch, open **Settings** and set:
   - **Server URL**: `http://<this-PC's-LAN-IP>:3000` (find it with `ipconfig`; the Android device and PC must be on the same network). `10.0.2.2:3000` works from the Android emulator only.
   - **API Key**: the device API key above.
   - **Read Range / Power**: Near / Medium / Far.
3. Tap **Test Connection** to confirm it reaches `/api/device/ping`.
4. From Home, tap **TAG REGISTRATION** to register new EPCs to products, or **RECORD ACTIVITY** to pick Stock Opname / Transfer / Outbound.

CLI build (already verified working on this machine):

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
cd android\warehouse-app
.\gradlew.bat assembleDebug
```

APK output: `android\warehouse-app\app\build\outputs\apk\debug\app-debug.apk`

## Known risks / follow-ups

- `middleware.ts` uses Next.js's deprecated-but-functional "middleware" convention; Next 16 recommends renaming to `proxy.ts`. Left as-is since the rename needs a git-committed working tree for the official codemod, and it's a warning, not an error.
- Not yet verified: DEVICE VERIFIED / PHYSICALLY VERIFIED — the Android app has only been build-verified (`gradlew assembleDebug` succeeds cleanly) and UI-reviewed as code; it has not been installed on a physical Chainway C5 or tested with real RFID tags.
- Prisma stayed on v5 (audit-clean) rather than jumping to the newly available v7 major, to avoid unverified breaking changes this session.
