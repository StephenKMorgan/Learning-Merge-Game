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
    `screens/` (board, places/story, shop, pass, event, settings, stats, dialogs), `puzzles/PuzzleScreen.kt`,
    `components/` (candy buttons, art loader, particles, floating background), `theme/`.
  - `fx/` — sound effects (SoundPool) and haptics. `notify/` — the "energy full" notification.
- `android/app/src/main/assets/items/*.webp` — all item art (Fluent Emoji 3D, MIT, plus our cinnamon rolls).
- `android/app/src/main/res/raw/sfx_*.wav` — synthesized sound effects.
- `music/` — the live music engine (pure Kotlin, no Android): `Instruments.kt` (synth voices), `Styles.kt` (one style
  per mood/event: chords, instruments, when each part joins), `Engine.kt` (composer + mixer), played by `fx/Music.kt`
  on an AudioTrack thread. `GameViewModel.updateMusic()` picks styles (puzzle / night after 8pm / event / day) and
  intensity (orders ready + board fullness).
- `android/app/src/main/assets/scenes/` — the hand-drawn renovation scenes: `<scene>.json` lists each layer
  (position, z order, and when it shows: `always`, `done:<task>`, `undone:<task>`) plus the layer images.
  Rendered by `ui/screens/SceneView.kt` (tap a layer to replay its story; the explore view zooms). Also `assets/items/surf_face.webp` and `mandy_face.webp` (portraits).
- Mandy is customizable (Settings → Your baker; `game/Looks.kt`, `Settings.avatar`). She is drawn in grey parts
  (`common.MANDY_PARTS`: hair per style, skin, apron, face, glasses, hat); scene layers carry `tint` (hair/skin/outfit,
  multiplied by her chosen colour) and `variant` (hair style or "glasses"). Her portrait is built the same way from
  `assets/items/mandy/parts.json` (`MandyAvatar`; `ItemArt("mandy_face")` uses it). In scene files always add her
  with `mandy_layers(...)` / `mandy_standing_layers(...)`, never a single flat image.
  Her name is customizable too (`Avatar.name`): keep writing "Mandy" in story text; dialogue swaps in her chosen
  name (`Avatar.personalize`).
- `tools/` — `prepare_art.py` (downloads Fluent emoji + converts to WebP), `make_cinnamon_art.py`,
  `make_sfx.py`, `make_icon.py`,
  `scenes/` (`make_scenes.py` renders the scene SVGs in `inside.py` / `patio.py` / `common.py` with Chromium).

## Save compatibility (most important rule)

Updates must NEVER lose Mandy's progress.
- Never change `applicationId` (`com.surfswirl.bakery`) or the signing key `android/app/bakery-debug.keystore`.
- Release version numbers must always go up (versionCode = major*10000 + minor*100 + patch, set by the release
  workflow). Android refuses an update with a lower versionCode.
- `GameState` fields: every field has a default; never rename or remove one (add new ones). Unknown fields
  are ignored on load, nulls fall back to defaults.
- Item ids (`"bread:3"`, `"gen:surf"`), task ids (`c1_sweep`), event ids and puzzle type names are stored in the
  save. Never rename or reorder chain levels; only append.
- If data must be rewritten, bump `CURRENT_SAVE_VERSION` and add a step to `Migrations` in `SaveStore.kt`,
  plus a test in `SaveTest`.

## How to add things

- **Seasonal event**: add a chain in `Content.chains` with `event = "<id>"`, a `Theme` and an `EventDef` (dates,
  milestones) in `Events.kt`. Events switch automatically by date; leftover event items become coins.
- **Event story** (like Frosty Holidays' cottage): give the `EventDef` a `scene`, `place` name, `intro` lines and one
  `decor` entry (title + lines) per milestone. Claiming milestone n unlocks decoration `<event>_<n+1>` (kept forever in
  `GameState.eventDecor`); draw the scene with layers shown on `done:<event>_<n>`. Add a `Story.Place` with `event = id`
  so it appears in the places row while the event runs and afterwards.
- **Story chapter**: append a `Chapter` to `Story.chapters` (new task ids), add its `Story.Place`, and draw its scene in
  `tools/scenes/<scene>.py`: one layer per task with `show="done:<task id>"` (and optional `undone:` "before"
  layers), placed where the thing really belongs. Run `python3 tools/scenes/make_scenes.py <preview_dir>` and
  check the before/after previews. The "?" marker uses the centre of the task's done-layer automatically.
- **Music**: add a `Style` in `music/Styles.kt` (chords per section, parts with instrument, pattern and the
  intensity they join at) and map it in `Styles.forEvent` / `level`. New instruments go in `music/Instruments.kt`.
  `MusicEngineTest` renders listening samples to `android/app/build/music-samples/` (convert with ffmpeg to share).
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

## Releasing to Mandy

Nothing reaches her until a release is published. Edit `RELEASE_NOTES.md`, then GitHub → Actions →
"Publish release" → Run workflow with a version like `1.1.0` (or push a tag `v1.1.0`). It runs the tests, builds
the APK with that version, and publishes it. The download link never changes:
`https://github.com/StephenKMorgan/Learning-Merge-Game/releases/latest/download/surf-and-swirl.apk`.
Her download page is `bakery/index.html` in the `StephenKMorgan.github.io` repo
(https://stephenkmorgan.github.io/bakery/); it reads the latest release from the GitHub API.
Everyday CI builds are version "dev" (versionCode 1) and are only for testing.

## Build & test

- CI: `.github/workflows/android.yml` runs tests and uploads the APK + screenshots on every push.
- Local: `cd android && ./gradlew testDebugUnitTest assembleDebug` (JDK 17+, Android SDK 34).
- UI tests (Robolectric) play the real app and save screenshots to `android/app/build/screenshots/`.
  Frame loops must use `withInfiniteAnimationFrameNanos` so tests can idle.

## Roadmap / future updates

- More music styles and instruments (each event already has its own style).
- Optional: hand-drawn board items to match the scenes (the Fluent 3D emoji currently look richer on small tiles).
- Chapter 4 and more story; event stories for the other seasonal events (Harvest, Sweetheart, Spring, Summer, Spooky).
- More puzzle types (logic grid riddles, truth-tellers from the Therefore app, crosswords, spelling bee).
- Generator upgrades, a collection book with rewards, achievements.
- Weekly mini-events between the big seasonal ones.
