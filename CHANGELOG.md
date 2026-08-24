# SpeedTracer — Changelog

All notable changes to this project will be documented here.
Format loosely follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

---

## [Unreleased]

### Added

- Black game field background on the main view to match the planned v0.2 styling.
- Score-based explosion overlay on round completion for high-scoring traces, with tiered intensity for 750–1000 scores.
- Explosion tier helper and regression coverage for the score bands described in the roadmap.

### Fixed

- Confirmed the minimum circle radius is set at 15 dp in the runtime logic to keep the target physically traceable.

---

## [0.1.0] — 2026-08-23 — Initial working build

### Added

- Core game loop: randomly sized/positioned circle, 3-second countdown, touch tracing.
- `GameView.kt` — custom `View` handling circle rendering, touch events, and the frame-by-frame timer.
- `Scorer.kt` — pure Kotlin scoring engine (accuracy from mean edge deviation, 36-sector coverage, speed factor). Score range 0–1000.
- `ScoreStore.kt` — last-20-score history persisted via `SharedPreferences`.
- `MainActivity.kt` — result panel (score, accuracy %, coverage %, time), Play Again / History buttons.
- `ScorerTest.kt` — unit tests for `Scorer`.
- Beeps at 3 / 2 / 1 seconds remaining via `ToneGenerator`.
- AppCompat-only UI, `minSdk 21`.

### Fixed / Renamed

- Project originally scaffolded as **CircleTrace**; renamed to **SpeedTracer** across all files:
  - `settings.gradle.kts` `rootProject.name`
  - `app/build.gradle.kts` `namespace` + `applicationId` → `com.lpmoore.speedtracer`
  - `AndroidManifest.xml` theme reference
  - `themes.xml` style name
  - `activity_main.xml` fully-qualified `GameView` class reference
  - All Kotlin `package` declarations
  - Source directories moved from `circletrace/` → `speedtracer/`
  - `README.md` title
- Added `.gitignore` (was missing on first commit — caused HTTP 400 on push due to
  6.32 MiB of build artifacts, APK, Gradle cache, and `.DS_Store` being committed).
  Fixed by stripping cached files, amending the commit, and force-pushing a clean
  36-file / 101 KB history.
- Initialized git repo and connected remote: `https://github.com/lpmoore/SpeedTracer.git`.
