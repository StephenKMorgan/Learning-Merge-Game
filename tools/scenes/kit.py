"""The scene kit: a library of props and room pieces for building new places quickly.

How it works
------------
* Every prop is drawn once (most are reused from the existing scenes) and registered in PROPS with how it
  mounts: "floor" (stands on the floor), "surface" (sits on a counter/table), "wall" (hangs on a wall),
  "ceiling" (hangs from the ceiling) or "sky".
* `measure()` renders each prop once and records its footprint, so it can be placed by its *foot*: the bottom
  centre for floor/surface/wall props, the top centre for ceiling props.
* A room (`room(...)`) is a wall + floor + optional counter, and comes with named anchors: "wall_left",
  "counter_top_right", "floor_front_center", "ceiling_left"... Placing a prop on an anchor puts its foot exactly
  there, so things always sit on the counter / hang from the ceiling properly.
* `scene_layers(room, items)` turns a short list of items into layers for make_scenes.py.

Example (a whole new place):

    r = room("stripes", "wood", counter=True)
    layers = scene_layers(r, [
        item("lights", "pendant_lights", "ceiling_center", task="c4_lights"),
        item("bell", "service_bell", "counter_top_right", task="c4_bell"),
        item("plant", "planter_box", "floor_front_left", task="c4_plants", scale=0.8),
    ], mandy=("behind_counter",), surf=("floor_front_right", "c4_bed"))

Run `python3 tools/scenes/kit.py <out_dir>` to render the catalog sheet of every prop.
"""
import json, math, os, sys, tempfile
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from common import W, H, svg, mandy_layers, mandy_standing_layers, surf_sitting, surf_lying, cinnamon_roll
import inside, patio, kitchen, cottage

HERE = os.path.dirname(os.path.abspath(__file__))
CACHE = os.path.join(HERE, "kit_footprints.json")

# ------------------------------------------------------------------ props
# name: (draw function returning SVG at its native spot, mount, category, description)
PROPS = {
    # bakery interior
    "window_curtains": (inside.window_clean, "wall", "walls", "Window with pink curtains and a flower box"),
    "window_dirty": (inside.window_dirty, "wall", "walls", "Grimy, cracked window (a 'before' prop)"),
    "door_open": (inside.door_new, "floor", "walls", "Mint door with an OPEN sign"),
    "door_boarded": (inside.door_old, "floor", "walls", "Boarded-up door (a 'before' prop)"),
    "painting": (inside.picture, "wall", "walls", "Framed landscape painting"),
    "wall_clock": (inside.clock, "wall", "walls", "Round wall clock"),
    "broom": (inside.broom, "floor", "cleaning", "Broom leaning on the wall"),
    "pendant_lights": (inside.lights, "ceiling", "lights", "Three brass pendant lamps with glow"),
    "service_bell": (inside.bell, "surface", "counter", "Brass service bell"),
    "display_case": (inside.display_case, "surface", "counter", "Glass case of cinnamon rolls"),
    "bakery_counter": (inside.counter, "floor", "furniture", "Wooden shop counter with a cream top"),
    "dog_bed": (inside.dog_bed, "floor", "surf", "Surf's round bed"),
    "cafe_table_set": (inside.table_chairs, "floor", "furniture", "Round café table with two chairs"),
    "reading_nook": (inside.nook, "floor", "furniture", "Armchair, bookshelf and floor lamp"),
    "bunting_balloons": (inside.party, "ceiling", "party", "Bunting, balloons and confetti"),
    "dust_and_webs": (inside.dust, "sky", "before", "Dust piles, cobwebs and haze (a 'before' overlay)"),
    # patio
    "tulip_bed": (patio.tulips, "floor", "garden", "Long bed of tulips"),
    "patio_umbrella": (patio.umbrella, "floor", "garden", "Striped patio umbrella"),
    "bistro_table": (patio.table, "floor", "furniture", "Bistro table with two mint chairs"),
    "flower_vase": (patio.bouquet, "surface", "counter", "Vase of flowers"),
    "planter_box": (lambda: patio.planter(20, 820), "floor", "garden", "Wooden planter box of flowers"),
    "string_lights": (patio.lanterns, "ceiling", "lights", "Colourful string lights"),
    "balloon_bunch": (patio.balloons, "floor", "party", "Bunch of balloons tied down"),
    "sunshine": (patio.sun, "sky", "outdoor", "Sun with rays"),
    "beach_towel": (patio.towel, "floor", "surf", "Striped towel for napping on"),
    "weeds": (patio.weeds, "sky", "before", "Weeds everywhere (a 'before' overlay)"),
    # kitchen
    "brick_oven": (kitchen.oven, "floor", "kitchen", "Brick oven with a fire"),
    "work_table": (kitchen.table, "floor", "kitchen", "Flour-dusted work table with a rolling pin"),
    "jar_shelves": (kitchen.shelves, "wall", "kitchen", "Two shelves of labelled jars"),
    "stand_mixer": (kitchen.mixer, "surface", "kitchen", "Pink stand mixer"),
    "cooling_rack": (kitchen.rack, "floor", "kitchen", "Cooling rack of cinnamon rolls"),
    "herb_window_box": (kitchen.herbs, "wall", "kitchen", "Herb box and yellow curtains for a window"),
    "recipe_board": (kitchen.recipe, "wall", "kitchen", "Chalkboard with the secret recipe"),
    "quality_mat": (kitchen.surf_mat, "floor", "surf", "Surf's QUALITY CONTROL mat and sign"),
    "award_medal": (kitchen.award, "wall", "party", "Golden Whisk medal and confetti"),
    "crates": (kitchen.junk, "sky", "before", "Stacked crates and dust (a 'before' overlay)"),
    # cottage
    "fireplace_fire": (cottage.fire, "floor", "cozy", "Crackling fire (put inside a fireplace)"),
    "stockings": (cottage.stockings, "wall", "holiday", "Three stockings on a mantel"),
    "fir_tree": (cottage.tree, "floor", "holiday", "Plain fir tree in a pot"),
    "tree_ornaments": (cottage.tree_decor, "floor", "holiday", "Ornaments and star for the fir tree"),
    "garland_lights": (cottage.garland, "ceiling", "holiday", "Garland on the mantel and lights on the beams"),
    "wreath": (cottage.wreath, "wall", "holiday", "Holly wreath"),
    "cocoa_table": (cottage.cocoa, "floor", "cozy", "Table with cocoa and cookies"),
    "presents": (cottage.presents, "floor", "holiday", "Pile of wrapped presents"),
    "snowman": (cottage.snowman, "floor", "holiday", "Little snowman"),
}

def measure(force=False):
    """Footprint of every prop (bbox in native coords), cached in kit_footprints.json."""
    cache = {} if force or not os.path.exists(CACHE) else json.load(open(CACHE))
    todo = [n for n in PROPS if n not in cache]
    if todo:
        from render import render
        from PIL import Image
        with tempfile.TemporaryDirectory() as d:
            render([(svg(PROPS[n][0]()), os.path.join(d, n + ".png"), W, H) for n in todo])
            for n in todo:
                box = Image.open(os.path.join(d, n + ".png")).getbbox() or (0, 0, 1, 1)
                cache[n] = list(box)
        json.dump(cache, open(CACHE, "w"), indent=1, sort_keys=True)
    return cache

def place(name, x, y, scale=1.0, flip=False):
    """SVG for a prop with its foot at (x, y)."""
    fn, mount = PROPS[name][0], PROPS[name][1]
    if mount == "sky":
        return fn()                      # full-scene overlays stay where they are
    x0, y0, x1, y1 = measure()[name]
    fx = (x0 + x1) / 2
    fy = y0 if mount == "ceiling" else y1
    sx = -scale if flip else scale
    tx = x - fx * sx
    ty = y - fy * scale
    return f'<g transform="translate({tx:.1f},{ty:.1f}) scale({sx:.3f},{scale:.3f})">{fn()}</g>'

# ------------------------------------------------------------------ rooms

FLOOR = 600

WALLS = {
    "stripes": ("#ffdde5", "#ffffff", "stripes"),
    "mint": ("#dff5ec", "#ffffff", "stripes"),
    "butter": ("#fff1c9", "#ffffff", "stripes"),
    "logs": ("#c98b5e", "#b97c50", "logs"),
    "tiles": ("#f4ece2", "#ffffff", "tiles"),
    "brick": ("#ffc9b8", "#f4b3a2", "brick"),
}
FLOORS = {"wood": ("#cf9365", "#a9693f"), "checker": ("#e9b38c", "#d98f62"), "planks": ("#8a5a3c", "#7d5034"),
          "stone": ("#efe1cf", "#d8c3aa"), "grass": ("#9ed98c", "#8ccf73")}

class Room:
    def __init__(self, base, anchors, fixtures=()):
        self.base = base
        self.anchors = anchors
        self.fixtures = list(fixtures)   # (id, z, svg) always-there layers drawn above the base (e.g. the counter)

def room(wall="stripes", floor="wood", counter=False, ceiling=True):
    """A room base and its anchors. counter=True adds a shop counter in the middle."""
    c1, c2, kind = WALLS[wall]
    f1, f2 = FLOORS[floor]
    walls = f'<rect width="{W}" height="{FLOOR}" fill="{c1}"/>'
    if kind == "stripes":
        walls += "".join(f'<rect x="{x}" y="0" width="34" height="{FLOOR}" fill="{c2}" opacity="0.35"/>' for x in range(0, W, 68))
        walls += f'<rect y="470" width="{W}" height="{FLOOR - 470}" fill="{c2}" opacity="0.4"/><rect y="466" width="{W}" height="10" fill="#000" opacity="0.06"/>'
    elif kind == "logs":
        walls += "".join(f'<rect x="0" y="{y}" width="{W}" height="40" rx="18" fill="{c1 if i % 2 == 0 else c2}"/>' for i, y in enumerate(range(50, FLOOR, 40)))
    elif kind == "tiles":
        for r in range(9):
            off = 0 if r % 2 == 0 else 30
            walls += "".join(f'<rect x="{x + 2}" y="{332 + r * 30}" width="56" height="26" rx="4" fill="{c2}" stroke="#e7ddd2" stroke-width="2"/>' for x in range(-60 + off, W, 60))
    elif kind == "brick":
        for r in range(26):
            off = 0 if r % 2 == 0 else 24
            walls += "".join(f'<rect x="{x + 2}" y="{r * 23 + 2}" width="44" height="19" rx="4" fill="{c2}" opacity="0.7"/>' for x in range(-24 + off, W, 48))
    floor_svg = f'<rect y="{FLOOR}" width="{W}" height="{H - FLOOR}" fill="{f1}"/>'
    if floor in ("wood", "planks"):
        floor_svg += "".join(f'<line x1="{540 + (bx - 540) * 0.6:.0f}" y1="{FLOOR}" x2="{bx}" y2="{H}" stroke="{f2}" stroke-width="3" opacity="0.5"/>' for bx in range(-540, 1700, 90))
        floor_svg += "".join(f'<line x1="0" y1="{y}" x2="{W}" y2="{y}" stroke="{f2}" stroke-width="2" opacity="0.35"/>' for y in (630, 676, 736, 810, 900))
    elif floor == "checker":
        for r in range(6):
            y0 = FLOOR + r * 58 + r * r * 3
            floor_svg += "".join(f'<rect x="{c * 96 - r * 10}" y="{y0}" width="96" height="{58 + r * 6}" fill="{f1 if (r + c) % 2 == 0 else f2}"/>' for c in range(-2, 14))
    elif floor == "stone":
        for r in range(5):
            y = FLOOR + 20 + r * 70 + r * r * 4
            floor_svg += "".join(f'<rect x="{c * 140 + (70 if r % 2 else 0) - r * 6}" y="{y}" width="{124 + r * 6}" height="{52 + r * 6}" rx="18" fill="{f1}" stroke="{f2}" stroke-width="3"/>' for c in range(-1, 9))
    top = f'<rect width="{W}" height="50" fill="url(#wood)"/><rect y="46" width="{W}" height="8" fill="#6e3f26" opacity="0.5"/>' if ceiling else ""
    base = walls + top + floor_svg + f'<rect y="{FLOOR}" width="{W}" height="16" fill="#000" opacity="0.08"/>'
    anchors = {
        "ceiling_left": (240, 50), "ceiling_center": (540, 50), "ceiling_right": (840, 50),
        "wall_left": (210, 420), "wall_center_left": (420, 300), "wall_center": (540, 300), "wall_center_right": (660, 300), "wall_right": (870, 420),
        "wall_high_left": (220, 250), "wall_high_right": (860, 250),
        "floor_back_left": (150, FLOOR + 10), "floor_back_center": (540, FLOOR + 10), "floor_back_right": (930, FLOOR + 10),
        "floor_front_left": (200, 880), "floor_front_center": (540, 900), "floor_front_right": (880, 880),
        "floor_mid_left": (230, 760), "floor_mid_right": (850, 760),
    }
    fixtures = []
    if counter:
        fixtures.append(("counter", 30, place("bakery_counter", 540, 730)))
        anchors.update({"counter_top_left": (400, 466), "counter_top_center": (540, 466), "counter_top_right": (690, 466),
                        "behind_counter": (560, 352)})
    return Room(base, anchors, fixtures)

def item(id, prop, at, task=None, before=False, scale=1.0, z=None, flip=False):
    """One thing in a scene. task: shown once this task is done (or, with before=True, until it is done)."""
    return dict(id=id, prop=prop, at=at, task=task, before=before, scale=scale, z=z, flip=flip)

Z_BY_MOUNT = {"sky": 60, "ceiling": 15, "wall": 10, "surface": 36, "floor": 30}

def scene_layers(r, items, mandy=None, surf=None):
    """Layers for make_scenes.py. mandy=(anchor, ) puts Mandy there (behind_counter = waist-up behind it).
    surf=(anchor, bed_task) sits Surf there, or lies him down once bed_task is done."""
    out = [("base", 0, "always", r.base)] + [(fid, z, "always", body) for (fid, z, body) in r.fixtures]
    for it in items:
        x, y = r.anchors[it["at"]] if isinstance(it["at"], str) else it["at"]
        mount = PROPS[it["prop"]][1]
        z = it["z"] if it["z"] is not None else Z_BY_MOUNT[mount] + (5 if y > 760 else 0)
        show = "always" if not it["task"] else (("undone:" if it["before"] else "done:") + it["task"])
        out.append((it["id"], z, show, place(it["prop"], x, y, it["scale"], it["flip"])))
    if mandy:
        a = mandy[0]
        if a == "behind_counter":
            out += mandy_layers("mandy", 20, "always", *r.anchors[a], 0.74, wave=False)
        else:
            x, y = r.anchors[a] if isinstance(a, str) else a
            out += mandy_standing_layers("mandy", 40, "always", x, y, 0.62)
    if surf:
        x, y = r.anchors[surf[0]] if isinstance(surf[0], str) else surf[0]
        if len(surf) > 1 and surf[1]:
            out.append(("surf_sit", 44, "undone:" + surf[1], surf_sitting(x, y, 0.55)))
            out.append(("surf_lie", 45, "done:" + surf[1], surf_lying(x - 20, y - 30, 0.62)))
        else:
            out.append(("surf_sit", 44, "always", surf_sitting(x, y, 0.55)))
    return out

# ------------------------------------------------------------------ catalog

def catalog(out_dir):
    """A contact sheet of every prop with its name and mount, for browsing."""
    from render import render
    from PIL import Image, ImageDraw
    measure()
    cell = 300
    names = sorted(PROPS, key=lambda n: (PROPS[n][2], n))
    with tempfile.TemporaryDirectory() as d:
        jobs = []
        for n in names:
            jobs.append((svg(PROPS[n][0]()), os.path.join(d, n + ".png"), W, H))
        render(jobs)
        cols = 6
        rows = (len(names) + cols - 1) // cols
        sheet = Image.new("RGB", (cols * cell, rows * (cell + 40)), (255, 248, 238))
        dr = ImageDraw.Draw(sheet)
        for i, n in enumerate(names):
            im = Image.open(os.path.join(d, n + ".png")).convert("RGBA")
            box = im.getbbox()
            if box: im = im.crop(box)
            im.thumbnail((cell - 20, cell - 20))
            x, y = (i % cols) * cell, (i // cols) * (cell + 40)
            sheet.paste(im, (x + (cell - im.width) // 2, y + (cell - im.height) // 2), im)
            dr.text((x + 8, y + cell + 4), n, fill=(91, 50, 32))
            dr.text((x + 8, y + cell + 20), f"{PROPS[n][1]} · {PROPS[n][2]}", fill=(150, 110, 90))
        path = os.path.join(out_dir, "kit-catalog.png")
        sheet.save(path)
        return path

if __name__ == "__main__":
    measure(force=True)
    print(catalog(sys.argv[1] if len(sys.argv) > 1 else "."))
