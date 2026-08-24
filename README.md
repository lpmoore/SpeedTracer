# SpeedTracer (Android, API 21+)

Trace the randomly sized circle as accurately and quickly as you can within 3 seconds.

## Plan → implementation map

1. Scaffold — `settings.gradle.kts`, `build.gradle.kts`, `app/build.gradle.kts` (minSdk 21, AppCompat only).
2. Round setup — `GameView.startRound()`: radius 5–100dp, random on-screen position, fading red center dot.
3. Timer + touch — `GameView.onTouchEvent` / `frame` runnable: countdown starts on first touch, beeps at 3/2/1 via `ToneGenerator`, lifting the finger ends the round as interrupted, a full loop (≥97% sector coverage) ends it early.
4. Scoring — `Scorer.kt` (pure Kotlin, `ScorerTest.kt`): accuracy from mean edge deviation vs tolerance (45% of radius, min 10dp), coverage over 36 sectors, speed factor 1.0→0.5; score 0–1000.
5. Result screen + history — `MainActivity` result panel, `ScoreStore` (SharedPreferences, last 20).
6. Explosion feedback — `ExplosionView`: runs for a fixed 1.0s, starts at score 500, and scales in 100-point tiers (500/600/700/800/900/1000).

## Build

Open in Android Studio (Hedgehog or newer) and let it generate the Gradle wrapper, or run
`gradle wrapper && ./gradlew assembleDebug`. Unit tests: `./gradlew test`.

## Design notes / deliberate choices

- Countdown starts on first touch rather than when the circle appears, so reaction time isn't penalised and "time taken" is clean.
- Tolerance scales with radius but has a 10dp floor so 5dp circles are still traceable with a finger.
- No assets: sounds come from `ToneGenerator`, graphics are Canvas draws.
