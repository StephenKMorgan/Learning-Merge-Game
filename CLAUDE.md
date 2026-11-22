# Surf & Swirl Bakery — notes for Claude

A cozy merge game for Mandy (and Surf, the golden retriever). Native Android app: Kotlin + Jetpack Compose,
fully offline. In-game "purchases" are paid with brain puzzles instead of money.

## Layout

- `android/app/src/main/java/com/surfswirl/bakery/`
  - `game/` — pure game rules, no Android: `Content.kt` (item chains, generators, characters),
    `State.kt` (the saved `GameState`), `Engine.kt` (every rule as `state -> Outcome(state, fx)`),
    `Economy.kt` (prices, shop offers, pass rewards, daily rewards), `Story.kt` (renovation chapters and
    dialogue), `Events.kt` (seasonal events and themes).
  - `puzzles/` — `Brain.kt` (adaptive difficulty), `Generators.kt` (all 6 puzzle generators), `WordBank.kt`.
  - `data/SaveStore.kt` — JSON save, atomic writes, `save.prev.json` fallback, `Migrations`.
  - `ui/` — `GameViewModel.kt` (state, dialogs, puzzle sessions), `App.kt` (root, particles, toasts),
    `screens/` (board, bakery, shop, pass, event, settings, stats, dialogs), `puzzles/PuzzleScreen.kt`,
    `components/` (candy buttons, art loader, particles, floating background), `theme/`.
  - `fx/` — sound effects (SoundPool) and haptics. `notify/` — the "energy full" notification.
- `android/app/src/main/assets/items/*.webp` — all item art (Fluent Emoji 3D, MIT, plus our cinnamon rolls).
- `android/app/src/main/res/raw/sfx_*.wav` — synthesized sound effects.
- `tools/` — `prepare_art.py` (downloads Fluent emoji + converts to WebP), `make_cinnamon_art.py`,
  `make_sfx.py`, `make_icon.py`.

## Save compatibility (most important rule)

Updates must NEVER lose Mandy's progress.
- Never change `applicationId` (`com.surfswirl.bakery`) or the signing key `android/app/bakery-debug.keystore`.
- Bump `versionCode` in `android/app/build.gradle.kts` for every release.
- `GameState` fields: every field has a default; never rename or remove one (add new ones). Unknown fields
  are ignored on load, nulls fall back to defaults.
- Item ids (`"bread:3"`, `"gen:surf"`), task ids (`c1_sweep`), event ids and puzzle type names are stored in the
  save. Never rename or reorder chain levels; only append.
- If data must be rewritten, bump `CURRENT_SAVE_VERSION` and add a step to `Migrations` in `SaveStore.kt`,
  plus a test in `SaveTest`.

## How to add things

- **Seasonal event**: add a chain in `Content.chains` with `event = "<id>"`, a `Theme` and an `EventDef` (dates,
  milestones) in `Events.kt`. Events switch automatically by date; leftover event items become coins.
- **Story chapter**: append a `Chapter` to `Story.chapters` (new task ids, scene "inside"/"patio" or add a scene
  in `Screens.kt` `Scene`). Decorations are art names from assets.
- **Item chain / generator**: append to `Content.chains` / `Content.generators` (with `unlockLevel`). Players who
  reach the level get the generator in their gift bubble automatically (`Engine.giveMissingGenerators`).
- **Puzzle type**: add to `PuzzleType` (with a `prior` difficulty), a generator in `Generators.kt`, a
  `baselineSeconds` case in `Brain`, and a composable in `PuzzleScreen.kt`.
- **Art**: add the Fluent folder name to `NAMES` in `tools/prepare_art.py` and run it from the repo root.

## Difficulty model

`effort ≈ level × factor`. Each puzzle type has a factor (how hard it is for Mandy; words start at 0.8,
math at 1.25–1.3). The shop price is an effort; the level offered for each type is `effort / factor`. After
every puzzle the factor moves toward her performance (solved, mistakes, hints, time vs. her own median).
A fast clean solve gives a hidden +25% "lucky bonus" — never show a timer or mention speed in the UI.

## Build & test

- CI: `.github/workflows/android.yml` runs tests and uploads the APK + screenshots on every push.
- Local: `cd android && ./gradlew testDebugUnitTest assembleDebug` (JDK 17+, Android SDK 34).
- UI tests (Robolectric) play the real app and save screenshots to `android/app/build/screenshots/`.
  Frame loops must use `withInfiniteAnimationFrameNanos` so tests can idle.

## Roadmap / future updates

- **Background music** (cozy café loops, CC0) with the existing `Settings.music` toggle. Sound effects already done.
- Chapter 3 "Cinnamon Famous" and more story; more renovation scenes.
- More puzzle types (logic grid riddles, truth-tellers from the Therefore app, crosswords, spelling bee).
- Generator upgrades, a collection book with rewards, achievements.
- Weekly mini-events between the big seasonal ones.
