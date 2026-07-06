# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

FitBase is a personal fitness/nutrition Android app for a single user. The repo has two layers:

1. **Knowledge base** (`/knowledge_base/`) — Markdown files containing user data, scientific evidence (papers), and business rules. This is the **single source of truth** for all training and nutrition logic.
2. **Generated app code** (`/output/ENTREGABLE_2_CODIGO/`) — Android app (Java) + Google Apps Script backend.

## Critical Rule

**NEVER** invent, infer, or use pre-trained knowledge for business logic, exercises, or nutritional guidelines. All domain logic must come from `/knowledge_base/` files. Always read `manifest.md` first.

## Architecture

```
Android App (Java + Views/XML)
  └─ Retrofit/REST ──► Google Apps Script (Codigo.gs)
                           └─► Google Sheets (database)
```

- **Frontend**: Java 17, Android Views/XML. **NO Kotlin, NO Jetpack Compose** (except `HealthConnectBridge.kt` which is a Kotlin coroutine bridge required by Health Connect SDK).
- **Backend**: Single `Codigo.gs` file deployed as Apps Script Web App. All endpoints go through `doGet`/`doPost` with an `accion` query parameter.
- **Database**: Google Sheets (9 sheets: `metricas_zepp`, `peso_log`, `plan_anual`, `plan_semanal`, `sesiones_plan`, `ejercicios_plan`, `ejercicios_log`, `ejercicios_catalogo`, `metricas_subjetivas`).
- **Health data**: Amazfit GTS 4 via Zepp → Health Connect → app. HRV and stress require manual entry.

## Build & Run

The Android project is at `output/ENTREGABLE_2_CODIGO/android/`.

```bash
# Build (from the android directory)
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug
```

- AGP 8.4.0, compileSdk 34, minSdk 26, targetSdk 34
- Java 17 source/target compatibility
- ViewBinding enabled
- Key dependencies: Retrofit 2.9, Room 2.6.1, Health Connect 1.1.0-alpha07, Material 1.11.0

The backend is a single file (`output/ENTREGABLE_2_CODIGO/backend/Codigo.gs`) pasted into Google Apps Script and deployed as a Web App. The endpoint URL is in `Constants.java:API_BASE_URL`.

## Code Conventions

- **Language**: Code and comments in Spanish. Variable names for business concepts in Spanish (`serieActual`, `pesoSugerido`).
- **Naming**: Classes PascalCase, variables camelCase, constants UPPER_SNAKE_CASE.
- **Architecture**: MVVM — Activity observes ViewModel via LiveData, ViewModel calls Repository, Repository decides API vs Room cache.
- **API pattern**: All Retrofit calls go to the same `exec` endpoint, differentiated by `accion` parameter.
- **Offline**: Room DB caches data; `SyncManager` queues failed POSTs as `OperacionPendiente` and retries on connectivity restore.
- **Theme**: Dark mode forced via `AppCompatDelegate.MODE_NIGHT_YES`.

## Key Files

| File | Role |
|------|------|
| `knowledge_base/manifest.md` | Master map — read before any action |
| `knowledge_base/usuario/prioridades.md` | Priority ranking that drives all decisions |
| `knowledge_base/reglas/desarrollo/compilador.md` | Code structure, conventions, data models |
| `knowledge_base/reglas/desarrollo/ui.md` | Screen specs and flows |
| `knowledge_base/reglas/logica/base_datos.md` | Google Sheets schema |
| `knowledge_base/reglas/logica/motor_pesos.md` | Load autoregulation algorithm |
| `knowledge_base/reglas/nutricion/motor_dieta.md` | Macro calculation logic |
| `output/ENTREGABLE_2_CODIGO/backend/Codigo.gs` | Entire backend in one file |
| `output/ENTREGABLE_2_CODIGO/android/.../Constants.java` | API URL, user biometrics, fallback macros |
| `output/ENTREGABLE_2_CODIGO/android/.../FitBaseApi.java` | Retrofit interface matching Codigo.gs endpoints |

## Android Project Structure

```
com.fitbase/
├── FitBaseApp.java          # Application: dark theme, notification channels, sync
├── data/
│   ├── api/                 # Retrofit (ApiClient, FitBaseApi)
│   ├── health/              # Health Connect (HealthConnectReader.java, HealthConnectBridge.kt)
│   ├── local/               # Room DB + SyncManager (offline queue)
│   └── model/               # POJOs/DTOs (Sesion, Ejercicio, EjercicioLog, Fase, etc.)
├── service/TimerService.java # Foreground service for rest timer
├── ui/
│   ├── home/                # Morning dashboard (macros, sleep, today's session)
│   ├── workout/             # Gym screen (swipe between exercises, log sets)
│   ├── summary/             # Post-workout summary
│   ├── plan/                # Annual and weekly plan views
│   └── progression/         # Progress tracking with metrics
└── util/                    # Constants, FeedbackHelper
```

## Apps Script Backend Sections

`Codigo.gs` is organized into numbered sections:
- §1 Config (sheet names, cache TTL)
- §2 Endpoints (doGet/doPost switch on `accion`)
- §3 Main logic (data retrieval functions)
- §4 Load engine (autoregulation based on evidence)
- §5 Helpers
- §6 Initialize (create sheets + headers — run once)
- §7 Populate (pre-generate annual/weekly/session plans)
- §8 Cleanup (clear test logs, keep structure)

## User Context

Single user: 24-year-old male, 188cm, 78.2kg, 18.9% body fat, intermediate-advanced. Priorities: P1 Aesthetics (V-taper), P2 Posture, P3 Hypertrophy (Shoulders > Biceps > Back). Elbow injury (avoid loaded extension). Halal diet, Ramadan-aware. Swims 2x/week. 11-month training plan.
