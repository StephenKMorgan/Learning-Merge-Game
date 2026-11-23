# Surf & Swirl Bakery 🐶🥐

A bright, cozy merge game made for Mandy. Fix up an old bakery on Maple Lane with Surf the golden
retriever, where everything smells like cinnamon rolls.

It has everything those free merge games have (energy, coins, rubies, a VIP pass, daily rewards, seasonal
events), but nothing costs money. Every shop item is paid for with a **brain puzzle**, and bigger rewards
ask for harder puzzles.

## What's in it

- **Merge board** (7×9): drag two of the same item together to make the next one. 8 item chains (bread,
  cinnamon rolls, sweets, breakfast, fruit, café drinks, flowers, Surf's toys) from 7 generators, including
  Surf himself. Packing boxes open when you merge next to them. Gift boxes, coins, rubies and energy on the board.
- **Customer orders** from the townsfolk for coins and XP. Leveling up unlocks new generators.
- **Bakery renovation story**: 2 chapters, 22 tasks, with dialogue (and a cinnamon roll mystery).
- **Brain Shop**: energy, coins, rubies and gift boxes, each paid with a puzzle. Daily stock, plus a free daily treat.
- **6 puzzle types**, each with 10 levels: Word Scramble, Secret Word, Quick Math, Number Patterns, Memory
  Match, Mini Sudoku. No visible timers.
- **Adaptive difficulty**: word puzzles start easier and math harder. The game tracks how she does on each type
  and adjusts the level for each reward. **Settings → Brain stats** shows which puzzles are hardest for her.
- **Golden Whisk VIP**: unlocked each month with a 3-puzzle Brain Gauntlet (three different categories).
  Gives more energy, faster refills, a 4th order slot and the golden pass track.
- **Season pass**, **daily login calendar**, **seasonal events with themes** (Harvest Festival, Frosty Holidays,
  Sweetheart Days, Spring Bloom, Surf's Summer, Spooky Season), and a theme picker.
- Hand-drawn bakery and patio scenes that fill in as you renovate, with Mandy and Surf (a real golden retriever).
- Live background music composed on the phone as you play: café and patio styles by day, softer music at night, a calm
  puzzle style, and a style for each seasonal event. More instruments join as the board gets busier. Plus sound effects, vibration, an optional "energy full" notification. Settings to turn off the daily popup
  and nudges.

## Install on the phone

Every push runs the **Android APK** workflow (Actions tab). Open the latest run and download
**surf-and-swirl-apk** from *Artifacts*. Unzip it and open `app-debug.apk` on the phone (allow installs from
Files/Chrome the first time).

**Updates keep progress**: every build is signed with the same key, so a new APK installs over the old one.
Progress is also in the phone's Google backup, and Settings has Back up / Restore to a file.

## Build it yourself

`cd android && ./gradlew assembleDebug` (JDK 17+, Android SDK 34). Tests: `./gradlew testDebugUnitTest`.

## Credits

Item art: [Microsoft Fluent Emoji](https://github.com/microsoft/fluentui-emoji) (MIT). Fonts: Fredoka and
Nunito (SIL OFL). Scenes, characters, cinnamon roll art, the music engine, sounds and icon were made for this game (see `tools/`).
