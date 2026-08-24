# SpeedTracer — Feature Plan

> **Note:** This file (and CHANGELOG.md) will eventually be moved to `.gitignore`
> once a more formal project-management tool is adopted.

---

## Conversion reference

| Real-world                      | dp     | Notes                                |
| ------------------------------- | ------ | ------------------------------------ |
| 3/16 inch (min circle diameter) | 30 dp  | = 15 dp radius; baseline 160 dp/inch |
| Current min radius              | 5 dp   | → change to **15 dp**                |
| Current max radius              | 100 dp | keep for now                         |

---

## Near-term (v0.2)

### 1. Minimum circle size fix

- Change minimum radius in `GameView.startRound()` from `5dp` to `15dp` (≈ 3/16" diameter).
- Ensures the circle is always physically traceable with a finger.

### 2. Black playing field

- Set `GameView` background to solid black (`#000000`) for now.
- **Future:** swap the background for a `Drawable` / `Bitmap` so graphics can be
  layered behind the circle without changing gameplay logic.
- **Later stages:** "trick" backgrounds — photographs or illustrations that contain
  circle-like shapes or arcs to confuse the player's eye. The real circle will still
  be drawn on top; visual noise is the challenge.

### 3. Score ≥ 500 — Animated explosion

- Draw a particle explosion centered on the circle's `(cx, cy)` after the round ends.
- Explosion size / intensity scales with score:
  - 500–599 → small burst
  - 600–699 → medium
  - 700–799 → large
  - 800–899 → extra large
  - 900–999 → massive
  - 1000 → see "Perfect Score" below
- Implementation idea: a `ValueAnimator` driving an `ExplosionView` overlay (list of
  `Particle` data classes with position, velocity, alpha, radius). Canvas-drawn on
  each frame. Keep it self-contained so it can be triggered from `MainActivity`.
- Animation timing: fixed 1000 ms for all explosion sizes.

### 4. Perfect score (1000) — Tesseract boss

- On a score of exactly 1000:
  1. Screen fades to white.
  2. A wireframe **tesseract** (4-D hypercube projection) appears, slowly rotating.
  3. Player must trace its outer silhouette (or a designated path — TBD).
  4. Treat as a joke "final boss" easter egg for now; no scoring impact.
- Implementation: project a rotating tesseract using its 16-vertex / 32-edge
  definition onto 2-D canvas via a simple orthographic + rotation matrix.
  Animate with a `ValueAnimator` on rotation angle.

---

## Sound (v0.3)

All sounds via `SoundPool` (short clips) or `ToneGenerator` (already in use).

| Trigger       | Sound idea                               |
| ------------- | ---------------------------------------- |
| Score 500–599 | Small explosion crack                    |
| Score 600–699 | Bigger blast                             |
| Score 700–799 | Deep boom                                |
| Score 800–899 | Rumbling explosion                       |
| Score 900–999 | Ground-shaking detonation                |
| Score 1000    | Ethereal "portal" sound before tesseract |
| Level up      | Ascending chime / fanfare                |
| Round start   | Soft countdown tick (already have beeps) |

- Sound assets: source royalty-free `.ogg` files or generate programmatically.
- **Future:** background music tracks per level, crossfaded on level change.
  Consider `ExoPlayer` or `MediaPlayer` for longer audio.

---

## Levels (v0.4)

### Promotion criteria (rolling window of last 10 rounds per level)

| Level | Circles | Time limit | Promotion threshold                     |
| ----- | ------- | ---------- | --------------------------------------- |
| 1     | 1       | 3 s        | ≥ 5 of last 10 rounds score ≥ 750       |
| 2     | 2       | 6 s        | ≥ 6 of last 10 rounds score ≥ 750       |
| 3     | 3       | 9 s        | ≥ 7 of last 10 rounds score ≥ 750 — TBD |
| …     | …       | +3 s       | +1 threshold per level — TBD            |

- Each level's rolling history stored separately in `ScoreStore`.
- **Cut scene / transition screen:** brief full-screen animation + level title card
  before gameplay resumes. Could be as simple as a fade-in text overlay for v0.4;
  proper animation later.
- Multi-circle scoring: score is the _average_ of all circle scores in the round
  (keeps 0–1000 range meaningful). TBD whether circles are simultaneous or sequential.

---

## Badges & Powerups (v0.5)

### Badges

- One unique badge design per level (primary-color themed: red L1, blue L2, yellow L3, …).
- Badge awarded each time the player _passes_ a level (promotion threshold hit).
- Badge count stored persistently per level in `ScoreStore` (or a new `BadgeStore`).
- Displayed in a "collection" screen accessible from the main menu.

### Powerup redemption

- Trade-in happens in the main menu before a round.
- Active powerups shown as icons in a horizontal strip across the top of the screen.
- Powerup is consumed at the start of the round it was activated for.

### Powerup ideas

| Powerup         | Cost           | Effect                                                        | Notes                                    |
| --------------- | -------------- | ------------------------------------------------------------- | ---------------------------------------- |
| **Wide Brush**  | 10 × L1 badges | Trace line width +10% for one round                           | Makes accuracy tolerance slightly easier |
| **Time Warp**   | 10 × L2 badges | +0.75 s added to the round timer                              | Straightforward extra time               |
| **Ghost Path**  | 10 × L2 badges | Faint ghost of the ideal circle edge shown for first 1 s      | Gives player a reference arc             |
| **Magnet**      | 15 × L2 badges | Trace points are softly snapped ≤ 5 dp toward the circle edge | Subtle aim-assist                        |
| **Mulligan**    | 20 × L3 badges | If round score < 400, replay the same circle once (no cost)   | Safety net                               |
| **Double Down** | 20 × L3 badges | Score for this round × 1.5 (capped at 1000)                   | High risk / reward                       |
| **Zoom**        | 10 × L1 badges | Circle radius +15% (easier to trace physically)               | May feel like cheating — balance TBD     |

> Open questions:
>
> - Should powerups stack within a round?
> - Should there be a cooldown between uses of the same powerup?
> - L3+ badge powerups TBD once level 3 gameplay is defined.

---

## Longer-term / Backlog

- **Leaderboard** — local high score table; eventually cloud-synced (Firebase or Play Games).
- **Haptic feedback** — vibration pulses on countdown ticks and explosions.
- **Accessibility** — color-blind palette option for badges and circle colors.
- **Tutorial / onboarding** — first-launch walkthrough.
- **Settings screen** — sound toggle, haptics toggle, reset progress.
- **Animated backgrounds** — per-level themes (starfield, lava, glitch grid, …).
- **Trick backgrounds** (late-stage) — images with embedded circle patterns to mislead
  the player; real game circle rendered on top with subtle highlight.
- **Share score** — screenshot + score card shareable to social media.
