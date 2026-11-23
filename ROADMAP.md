# Surf & Swirl Bakery: content roadmap

New content ships in **batches**, one release about **10 days before each seasonal event**, so the event's story
and music are in her hands on day one. Each batch has the same shape, so it can be planned, built and tested
together.

## What a batch contains

| Piece | Size | Notes |
|---|---|---|
| Story chapter | 10 tasks, 1 new place (hand-drawn scene) | Script first, then scene layers, then costs |
| Event story | 1 place, 10 decorations (one per milestone), intro | Ships before the event; activates by date |
| Gameplay extra | 1 of: new puzzle type, new item chain, or a system (below) | Rotates so every release has something new to *do* |
| Polish | fixes from her feedback | Collected between releases |

## Release calendar

Event dates are fixed in `Events.kt`. "Ship by" leaves ~10 days of slack before the event starts.

| Release | Ship by | Event (dates) | Chapter | Event story place | Gameplay extra |
|---|---|---|---|---|---|
| ✅ 1.2.0 | Nov 23 | Frosty Holidays (Dec 1–Jan 6) | 3 · Cinnamon Famous (kitchen) | Grandma June's Cottage | Places, explore, customizer |
| 1.3.0 | **mid-Dec** | (architecture) | – | – | Map of Maple Lane, board obstacles per chapter, daily specials + weekly mini-events, time-released chapters, scene kit |
| 1.4.0 | **Jan 20** | Sweetheart Days (Feb 1–15) | 4 · Home Above the Bakery (loft) + 5 · Saturday Market (opens Mar 1) | Rose Tea Room | Puzzle: Logic Grid riddles |
| 1.5.0 | **Mar 10** | Spring Bloom (Mar 20–Apr 30) | 6 · Surf's Beach Café (opens Jun 1) | Bea's Greenhouse | Item chain: Pies & Tarts + generator |
| 1.6.0 | **Jun 8** | Surf's Summer (Jun 20–Aug 15) | 7 · The Bakery Garden | Kai's Beach Shack | Collection book with rewards |
| 1.7.0 | **Sep 20** | Spooky Season (Oct 1–31) | 8 · The Great Maple Lane Bake-Off | The Pumpkin Patch | Puzzle: Truth-tellers (from Therefore) |
| 1.8.0 | **Oct 20** | Harvest Festival (Nov 1–30) | 9 · (tbd) | Apple Orchard Barn | Generator upgrades |
| 1.9.0 | **Nov 20** | Frosty Holidays, year 2 | 10 · (tbd) | Cottage, year 2: new decorations | Achievements |

From 1.4.0 each release carries **two chapters**: one open right away and one that opens on a date (`Chapter.opens`).
Map locations for every planned place already exist (under clouds), so a release just fills one in.

**A second map (idea, after 1.9.0):** Maple Bay Harbour is already on the map (under fog). Later, Mandy earns a
ferry ticket (e.g. finishing a chapter arc or a big event) and sails to a new map with its own places and chapters.

Gaps (Feb 15–Mar 20, May–June, Aug 15–Oct 1) are where **weekly mini-events** (planned for 1.5.0+) keep things
fresh without a release.

## Story outline (so chapters connect)

- **Ch 4 · Home Above the Bakery:** Mandy finally fixes up the dusty loft upstairs to live in. Cozy, personal,
  Surf gets a window seat. Valentine's tie-in. The roll thief is retired; new running gag: Surf's growing toy pile.
- **Ch 5 · Saturday Market:** word spreads after the Golden Whisk; Mandy runs a stall at the farmers' market.
  Meets new townsfolk (good for new customers).
- **Ch 6 · Surf's Beach Café:** a summer café cart on the beach with Kai. Surf learns to "surf".
- **Ch 7 · The Bakery Garden:** an orchard and herb garden behind the patio. Ingredients grown at home.
- **Ch 8 · The Great Maple Lane Bake-Off:** a town competition; Penny Pepper returns as a judge.
- **Ch 9+:** open; decide after Ch 6 based on what she enjoys.

## Pacing (important)

Chapters cost roughly: Ch1 ≈ 2,600 coins, Ch2 ≈ 5,300, Ch3 ≈ 12,000. An engaged player earns very roughly
1,000–2,000 coins a day, so a chapter can be finished in **1–2 weeks**, but releases are **6–12 weeks** apart.
Before 1.3.0, decide one of:
1. **Bigger chapters** (14–16 tasks) and steeper costs (Ch4 ≈ 20,000), or
2. **Time-released content:** ship 2 chapters per release, with the second unlocking on a date
   (e.g. "Chapter 5 opens March 1"), or
3. **Repeatable goals** between chapters (daily specials, mini-events) so there's always something to work toward.

Decided: **2 + 3** (two chapters per release, one time-released; daily specials and weekly mini-events, in 1.3.0).
Check how far she is on the map before each release to tune costs.

## Per-release checklist

1. Script: chapter lines + event intro/decor lines (keep writing "Mandy"; it's personalized automatically).
2. Data: tasks, costs, `Story.Place`, `EventDef.scene/decor/intro` (see CLAUDE.md "How to add things").
3. Art: build the scene with the kit (`room()` + `item()` on anchors, `kit.py`); draw new props only when the
   catalog doesn't have one, and add them to `PROPS`. Mandy via the kit's `mandy=` or `mandy_layers(...)`.
   Render with previews and check before/after. Add the place to its map location in `Story.locations`.
4. Gameplay extra + its tests.
5. Tests: engine (task order, rewards), a places/flow test, store screenshots refresh.
6. Listen check if music changed (`MusicEngineTest` samples).
7. `RELEASE_NOTES.md`, publish (`publish 1.x.0`), refresh website screenshots.
8. After release: note her feedback below for the next batch.

## Feedback log

- 2026-11: wanted hand-drawn art with proper layout (done 1.1.0), dynamic music (done 1.1.0), places to revisit,
  event stories, customizable character and name (done 1.2.0).
- 2026-11: wanted a map instead of a row of places, a parts library for cheaper levels, and a board that changes
  with chapters (the packing boxes were gone fast). All in 1.3.0.
- 2026-11 (1.3.0 testing): map felt kiddie/sparse with odd cloud blobs → detailed storybook map, merged fog of war,
  layout checker, animations, seasons and time of day, a harbour pin for a future ferry to a new map.
