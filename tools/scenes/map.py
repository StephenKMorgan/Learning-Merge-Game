"""The Maple Lane town map (storybook style) and its fog of war.

* map.webp: the whole town, drawn in detail. Each location's pin spot is exported to assets/scenes/map.json.
* fog.webp: one continuous cloud layer. The app cuts soft clearings in it around open locations, so the town
  is revealed bit by bit (like fog of war in a strategy game).

New locations: add a spot to SPOTS, draw it here, and add a Location with the same id in Story.kt.
Run from the repo root:  python3 tools/scenes/map.py [preview_dir]
"""
import base64, io, json, math, os, random, sys, tempfile
import numpy as np
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from common import svg
from render import render
from PIL import Image, ImageFilter

MW, MH = 1080, 1920
SCALE = 1.25
OUT = "android/app/src/main/assets/scenes"
R = random.Random(7)

SPOTS = {
    "orchard": (250, 380), "hill": (830, 330),
    "market": (215, 820), "bakery": (545, 805), "tearoom": (880, 815),
    "fair": (250, 1260), "greenhouse": (830, 1250), "beach": (520, 1650),
}
# How far around an open location the fog clears (map units). Exported for the app.
REVEAL = {"bakery": 420, "hill": 280}
DEFAULT_REVEAL = 270

# ------------------------------------------------------------------ textures

def fbm(w, h, octaves=5, seed=0, base=4):
    rng = np.random.default_rng(seed)
    out = np.zeros((h, w))
    amp, total = 1.0, 0.0
    for o in range(octaves):
        n = base * 2 ** o
        g = rng.random((n + 1, n + 1))
        im = Image.fromarray((g * 255).astype(np.uint8)).resize((w, h), Image.BICUBIC)
        out += np.asarray(im, dtype=float) / 255 * amp
        total += amp; amp *= 0.5
    return out / total

def data_uri(im):
    buf = io.BytesIO(); im.save(buf, "PNG")
    return "data:image/png;base64," + base64.b64encode(buf.getvalue()).decode()

def noise_overlay():
    """A soft greyscale texture laid over everything with multiply, so flat colours look painted."""
    n = fbm(540, 960, 6, seed=3, base=6)
    v = (0.80 + 0.20 * (n - n.min()) / (n.max() - n.min())) * 255
    return data_uri(Image.fromarray(v.astype(np.uint8)).convert("RGB"))

def fog_texture(path, w, h):
    """Billowy clouds: white tops, soft grey-blue undersides, nearly opaque everywhere."""
    n = fbm(w, h, 6, seed=11, base=5)
    n = (n - n.min()) / (n.max() - n.min())
    detail = fbm(w, h, 4, seed=12, base=16)
    shade = np.clip(0.55 + 0.6 * n + 0.15 * (detail - 0.5), 0, 1)
    r = 200 + 55 * shade; g = 208 + 47 * shade; b = 222 + 33 * shade
    a = np.clip(232 + 23 * n, 0, 255)
    im = Image.fromarray(np.dstack([r, g, b, a]).astype(np.uint8), "RGBA").filter(ImageFilter.GaussianBlur(1.2))
    im.save(path, "WEBP", quality=80, method=6)

# ------------------------------------------------------------------ painting helpers

def shade(c, f):
    c = c.lstrip("#"); r, g, b = int(c[0:2], 16), int(c[2:4], 16), int(c[4:6], 16)
    if f < 0: r, g, b = (int(v * (1 + f)) for v in (r, g, b))
    else: r, g, b = (int(v + (255 - v) * f) for v in (r, g, b))
    return f"#{r:02x}{g:02x}{b:02x}"

def tree(x, y, s=1.0, kind="round", c=None):
    if kind == "pine":
        c = c or R.choice(["#2f6b45", "#356f4a", "#2b5f40"])
        return (f'<ellipse cx="{x + 4 * s}" cy="{y + 3 * s}" rx="{16 * s}" ry="{5 * s}" fill="#1e3b25" opacity="0.3"/>'
                f'<rect x="{x - 3 * s}" y="{y - 8 * s}" width="{6 * s}" height="{10 * s}" fill="#5b3a26"/>'
                f'<path d="M{x},{y - 58 * s} L{x + 17 * s},{y - 4 * s} L{x - 17 * s},{y - 4 * s} Z" fill="{c}"/>'
                f'<path d="M{x},{y - 58 * s} L{x + 17 * s},{y - 4 * s} L{x},{y - 4 * s} Z" fill="{shade(c, -0.25)}"/>'
                f'<path d="M{x},{y - 58 * s} L{x - 6 * s},{y - 36 * s} L{x + 2 * s},{y - 40 * s} Z" fill="{shade(c, 0.25)}"/>')
    if kind == "maple":
        c = c or R.choice(["#e0782f", "#d9572b", "#e89a33", "#c94a2c"])
    else:
        c = c or R.choice(["#5b9a4a", "#4f8c43", "#67a552", "#4a843f"])
    return (f'<ellipse cx="{x + 5 * s}" cy="{y + 3 * s}" rx="{22 * s}" ry="{7 * s}" fill="#1e3b25" opacity="0.3"/>'
            f'<rect x="{x - 3.5 * s}" y="{y - 16 * s}" width="{7 * s}" height="{18 * s}" fill="#6b4630"/>'
            f'<circle cx="{x}" cy="{y - 30 * s}" r="{21 * s}" fill="{c}"/>'
            f'<circle cx="{x + 7 * s}" cy="{y - 24 * s}" r="{14 * s}" fill="{shade(c, -0.18)}"/>'
            f'<circle cx="{x - 8 * s}" cy="{y - 38 * s}" r="{10 * s}" fill="{shade(c, 0.18)}"/>')

def forest(cx, cy, rx, ry, n, kinds=("round",), avoid=(), s=(0.7, 1.05)):
    pts = []
    tries = 0
    while len(pts) < n and tries < n * 30:
        tries += 1
        a = R.random() * 2 * math.pi; d = math.sqrt(R.random())
        x, y = cx + math.cos(a) * rx * d, cy + math.sin(a) * ry * d
        if any((x - ax) ** 2 + (y - ay) ** 2 < ar ** 2 for (ax, ay, ar) in avoid): continue
        if any((x - px) ** 2 + (y - py) ** 2 < 18 ** 2 for (px, py, _, _) in pts): continue
        pts.append((x, y, R.choice(kinds), R.uniform(*s)))
    return "".join(tree(x, y, sc, k) for (x, y, k, sc) in sorted(pts, key=lambda p: p[1]))

def house(x, y, w=64, h=46, wall="#f1e3cc", roof="#b5553c", depth=18, chimney=True, door=True, windows=2, extra=""):
    """A little 3/4-view house; (x, y) is the front-bottom centre."""
    l = x - w / 2
    side = shade(wall, -0.18)
    out = f'<ellipse cx="{x + depth / 2}" cy="{y + 4}" rx="{w * 0.62}" ry="8" fill="#1e3b25" opacity="0.25"/>'
    out += f'<path d="M{l + w},{y} L{l + w + depth},{y - depth * 0.6} L{l + w + depth},{y - h - depth * 0.6} L{l + w},{y - h} Z" fill="{side}"/>'
    out += f'<rect x="{l}" y="{y - h}" width="{w}" height="{h}" fill="{wall}"/>'
    peak = y - h - h * 0.75
    out += f'<path d="M{l - 5},{y - h + 2} L{x},{peak} L{l + w + 5},{y - h + 2} Z" fill="{roof}"/>'
    out += f'<path d="M{x},{peak} L{l + w + depth + 4},{peak - depth * 0.6 + 4} L{l + w + depth + 4},{y - h - depth * 0.6 + 2} L{l + w + 5},{y - h + 2} Z" fill="{shade(roof, -0.22)}"/>'
    out += f'<path d="M{l - 5},{y - h + 2} L{l + w + 5},{y - h + 2}" stroke="{shade(roof, -0.35)}" stroke-width="2.5"/>'
    if chimney:
        out += f'<rect x="{x + w * 0.18}" y="{peak + 2}" width="8" height="{h * 0.45}" fill="#8a5a44"/>'
    for i in range(windows):
        wx = l + 8 + i * (w - 26) / max(1, windows - 1) if windows > 1 else x - 6
        out += f'<rect x="{wx}" y="{y - h + 9}" width="11" height="12" rx="1.5" fill="#fff4c9" stroke="{shade(wall, -0.35)}" stroke-width="1.5"/>'
    if door:
        out += f'<rect x="{x - 6}" y="{y - 20}" width="12" height="20" rx="2" fill="{shade(roof, -0.3)}"/>'
    return out + extra

def lamp(x, y):
    return (f'<rect x="{x - 1.5}" y="{y - 30}" width="3" height="30" fill="#3d3a36"/>'
            f'<circle cx="{x}" cy="{y - 32}" r="5" fill="#ffe7a3"/><circle cx="{x}" cy="{y - 32}" r="12" fill="#fff1c2" opacity="0.35"/>')

def field(x, y, w, h, color, rows, angle=0):
    lines = "".join(f'<line x1="{x + 6}" y1="{y + 6 + i * (h - 12) / max(1, rows - 1):.1f}" x2="{x + w - 6}" y2="{y + 6 + i * (h - 12) / max(1, rows - 1):.1f}" stroke="{shade(color, -0.2)}" stroke-width="3" stroke-linecap="round"/>' for i in range(rows))
    return (f'<g transform="rotate({angle} {x + w / 2} {y + h / 2})"><rect x="{x}" y="{y}" width="{w}" height="{h}" rx="6" fill="{color}"/>'
            f'{lines}<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="6" fill="none" stroke="#7a8f4a" stroke-width="3" opacity="0.6"/></g>')

def road(d, w=30, color="#d7bd8c", edge="#b49868"):
    return (f'<path d="{d}" stroke="{edge}" stroke-width="{w + 6}" fill="none" stroke-linecap="round" stroke-linejoin="round"/>'
            f'<path d="{d}" stroke="{color}" stroke-width="{w}" fill="none" stroke-linecap="round" stroke-linejoin="round"/>')

# ------------------------------------------------------------------ the map

def build():
    b = []
    add = b.append
    # land
    add(f'<rect width="{MW}" height="{MH}" fill="#93bf6e"/>')
    for (cx, cy, rx, ry, c) in [(300, 600, 380, 260, "#8bb766"), (800, 1000, 360, 240, "#9bc677"), (200, 1150, 260, 200, "#89b464"),
                                (700, 520, 300, 180, "#9fc87a"), (500, 1350, 400, 140, "#97c272")]:
        add(f'<ellipse cx="{cx}" cy="{cy}" rx="{rx}" ry="{ry}" fill="{c}" opacity="0.8"/>')
    # mountains along the top, then rolling hills
    peaks = [(-40, 230, 200), (110, 210, 260), (260, 235, 190), (420, 205, 250), (600, 230, 200), (760, 200, 270), (930, 225, 210), (1100, 215, 240)]
    for (x, base, hgt) in peaks:
        top = base - hgt
        add(f'<path d="M{x - 150},{base} L{x},{top} L{x + 150},{base} Z" fill="#8d97a6"/>')
        add(f'<path d="M{x},{top} L{x + 150},{base} L{x + 30},{base} Z" fill="#6f7a8a"/>')
        add(f'<path d="M{x},{top} L{x + 34},{top + 52} L{x + 12},{top + 44} L{x - 4},{top + 60} L{x - 26},{top + 44} Z" fill="#f4f7fb"/>')
    add('<path d="M-20,280 Q140,180 300,250 Q460,170 640,240 Q800,160 1100,230 V420 H-20 Z" fill="#7faa5c"/>')
    add('<path d="M-20,330 Q200,250 380,320 Q560,260 760,330 Q920,280 1100,320 V460 H-20 Z" fill="#88b363"/>')
    add('<path d="M640,470 Q700,230 860,210 Q1020,205 1100,300 V520 Z" fill="#7aa758"/>')        # June's hill
    add('<path d="M700,440 Q760,300 860,280 Q980,280 1040,360" stroke="#a7cf84" stroke-width="14" fill="none" opacity="0.6"/>')
    # sea, beach
    add(f'<path d="M-20,1500 Q240,1440 520,1480 Q820,1530 1100,1470 V{MH} H-20 Z" fill="#e8d29b"/>')
    add(f'<path d="M-20,1540 Q260,1480 520,1520 Q820,1570 1100,1510 V{MH} H-20 Z" fill="#f1dfae"/>')
    add('<defs><linearGradient id="sea" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#7fcbe0"/><stop offset="1" stop-color="#2f7fa8"/></linearGradient></defs>')
    add(f'<path d="M-20,1720 Q260,1680 540,1715 Q820,1755 1100,1700 V{MH} H-20 Z" fill="url(#sea)"/>')
    add('<path d="M-20,1720 Q260,1680 540,1715 Q820,1755 1100,1700" stroke="#ffffff" stroke-width="7" fill="none" opacity="0.75"/>')
    for i in range(26):
        x = R.uniform(20, 1060); y = R.uniform(1760, 1900)
        add(f'<path d="M{x:.0f},{y:.0f} q12,-8 24,0 q12,8 24,0" stroke="#ffffff" stroke-width="3" fill="none" opacity="{R.uniform(0.35, 0.7):.2f}" stroke-linecap="round"/>')
    # river: from the mountains, under Maple Lane's bridge, to the sea
    river = "M90,240 Q170,420 120,560 Q70,700 92,830 Q115,1010 300,1020 Q430,1035 480,1075 Q540,1100 640,1085 Q820,1080 900,1220 Q960,1380 1010,1600 Q1030,1690 1060,1740"
    add(f'<path d="{river}" stroke="#d8c48f" stroke-width="58" fill="none" stroke-linecap="round"/>')
    add(f'<path d="{river}" stroke="#5fb3d6" stroke-width="40" fill="none" stroke-linecap="round"/>')
    add(f'<path d="{river}" stroke="#8fd0e8" stroke-width="12" fill="none" stroke-linecap="round" stroke-dasharray="26 34" opacity="0.8"/>')
    # pond by the hill with lily pads
    add('<ellipse cx="960" cy="560" rx="70" ry="36" fill="#d8c48f"/><ellipse cx="960" cy="560" rx="62" ry="30" fill="#5fb3d6"/>')
    add("".join(f'<circle cx="{x}" cy="{y}" r="6" fill="#4f8c43"/><circle cx="{x + 2}" cy="{y - 2}" r="2" fill="#ff8fb5"/>' for (x, y) in [(935, 552), (975, 566), (990, 548)]))
    # farm fields + orchard (north-west)
    add(field(40, 400, 120, 80, "#d9b85c", 6, -6) + field(30, 500, 130, 70, "#9cbf5a", 5, -4) + field(175, 470, 90, 70, "#b98a5b", 5, 3))
    for r_ in range(3):
        for c_ in range(4):
            add(tree(200 + c_ * 42 + (r_ % 2) * 18, 300 + r_ * 36, 0.62, "round", "#5b9a4a"))
            add(f'<circle cx="{196 + c_ * 42 + (r_ % 2) * 18}" cy="{282 + r_ * 36}" r="3" fill="#e8442f"/><circle cx="{206 + c_ * 42 + (r_ % 2) * 18}" cy="{288 + r_ * 36}" r="3" fill="#e8442f"/>')
    add(house(330, 420, 70, 50, "#b8443a", "#7a2d26", depth=20, chimney=False, windows=1))           # red barn
    add('<rect x="378" y="350" width="22" height="70" rx="10" fill="#c9cfd6"/><ellipse cx="389" cy="350" rx="11" ry="6" fill="#9aa3ad"/>')  # silo
    add("".join(f'<ellipse cx="{x}" cy="{y}" rx="12" ry="9" fill="#e3c26a"/><path d="M{x - 10},{y} q10,-6 20,0" stroke="#c9a54f" stroke-width="2" fill="none"/>' for (x, y) in [(70, 600), (100, 615), (130, 598)]))
    # roads
    add(road("M545,860 L545,1060"))                                                      # down to the bridge
    add(road("M545,1110 Q540,1200 420,1240 Q330,1260 260,1290"))                       # to the fair
    add(road("M545,1110 Q600,1190 740,1230 Q790,1245 830,1270"))                       # to the greenhouse
    add(road("M545,1180 Q500,1350 520,1600"))                                          # to the beach
    add(road("M760,860 Q790,640 830,470 Q840,420 830,380"))                            # up June's hill
    add(road("M300,860 Q290,700 300,560 Q310,480 330,440"))                            # to the farm
    # Maple Lane: a cobbled main street
    add('<path d="M-20,872 L1100,872" stroke="#9e9488" stroke-width="64" fill="none"/>')
    add('<path d="M-20,872 L1100,872" stroke="#cfc6b8" stroke-width="56" fill="none"/>')
    add("".join(f'<rect x="{x}" y="{850 + (i % 3) * 15}" width="22" height="11" rx="3" fill="#bdb3a3" opacity="0.8"/>' for i, x in enumerate(range(-10, 1090, 17))))
    # stone bridges: on Maple Lane at the west end, and south of the bakery
    add('<rect x="58" y="836" width="72" height="8" rx="3" fill="#8f8579"/><rect x="58" y="900" width="72" height="8" rx="3" fill="#8f8579"/>'
        + "".join(f'<rect x="{62 + i * 16}" y="826" width="6" height="12" fill="#8f8579"/><rect x="{62 + i * 16}" y="906" width="6" height="12" fill="#8f8579"/>' for i in range(5)))
    add('<path d="M505,1055 L585,1055 L585,1115 L505,1115 Z" fill="#b8ada0"/><path d="M505,1060 h80 M505,1110 h80" stroke="#8f8579" stroke-width="5"/>'
        '<path d="M515,1070 q30,-14 60,0" stroke="#d6ccbe" stroke-width="3" fill="none"/>')
    # forests
    add(forest(560, 330, 120, 70, 26, ("pine", "round"), s=(0.75, 1.05)))
    add(forest(40, 760, 70, 90, 14, ("round", "maple")))
    add(forest(1050, 700, 60, 120, 14, ("pine", "round")))
    add(forest(640, 620, 70, 50, 9, ("maple", "round")))
    add(forest(110, 1050, 110, 100, 22, ("round", "maple", "pine"), avoid=[(250, 1260, 90)]))
    add(forest(980, 980, 90, 90, 16, ("pine", "round")))
    add(forest(380, 1430, 110, 40, 10, ("round",)))
    add(forest(700, 1420, 100, 40, 9, ("round", "maple")))
    # June's cottage on the hill: stone wall, garden, pines
    add('<path d="M740,370 Q830,330 930,372" stroke="#b5ab9c" stroke-width="7" fill="none" stroke-dasharray="10 4"/>')
    add(house(830, 360, 84, 54, "#efe2c9", "#6d5546", depth=22))
    add("".join(f'<circle cx="{850 + i * 10}" cy="{262 - i * 20}" r="{8 + i * 3}" fill="#ffffff" opacity="{0.65 - i * 0.15:.2f}"/>' for i in range(3)))
    add(tree(760, 340, 0.8, "pine") + tree(905, 330, 0.85, "pine") + tree(930, 360, 0.7, "pine"))
    # the town: houses on the north side of Maple Lane, a few to the south
    palette = [("#f1e3cc", "#b5553c"), ("#e5d8c3", "#5d7389"), ("#efe1d6", "#6d8a4f"), ("#e8d2b8", "#8a5a44"), ("#dfe5ea", "#b5553c")]
    north = [150, 330, 400, 680, 750, 1000, 1060]
    add("".join(f'<rect x="{x - 30}" y="770" width="60" height="22" rx="4" fill="#a8cf86" stroke="#e8dcc5" stroke-width="2"/>' for x in north))
    for i, x in enumerate(north):
        wall, roof = palette[i % len(palette)]
        add(house(x, 838, 56, 42, wall, roof, depth=16, windows=2))
    south = [170, 330, 760, 990, 1060]
    for i, x in enumerate(south):
        wall, roof = palette[(i + 2) % len(palette)]
        add(house(x, 960, 52, 40, wall, roof, depth=14, windows=2))
    # market square: plaza, fountain, stalls
    add('<ellipse cx="215" cy="910" rx="95" ry="38" fill="#cfc6b8" stroke="#9e9488" stroke-width="4"/>')
    add('<ellipse cx="215" cy="905" rx="22" ry="10" fill="#9e9488"/><ellipse cx="215" cy="902" rx="16" ry="7" fill="#8fd0e8"/><path d="M215,900 q-6,-16 0,-22 q6,6 0,22" fill="#cbeefa"/>')
    for (sx, c) in [(160, "#d9572b"), (195, "#3f7fb5"), (240, "#e0a42f"), (275, "#5b9a4a")]:
        add(f'<rect x="{sx - 14}" y="814" width="28" height="22" fill="#c9a074"/>'
            + "".join(f'<path d="M{sx - 16 + k * 8},{804} l8,0 l0,10 q-4,5 -8,0 Z" fill="{c if k % 2 == 0 else "#f6efe2"}"/>' for k in range(4)))
    # the bakery (pink, with awning and sign) and the tea room
    add(house(545, 845, 110, 70, "#f6cfd6", "#d0523e", depth=26, windows=3,
              extra='<rect x="498" y="800" width="94" height="10" fill="#ff8fb5"/>' + "".join(f'<path d="M{490 + k * 14},{810} l14,0 l0,8 q-7,6 -14,0 Z" fill="{"#ff8fb5" if k % 2 == 0 else "#ffffff"}"/>' for k in range(8))))
    add('<rect x="510" y="745" width="70" height="16" rx="4" fill="#ffffff" stroke="#d0523e" stroke-width="2"/>'
        '<text x="545" y="757" font-family="Fredoka, Arial Rounded MT Bold, Arial" font-weight="700" font-size="9" fill="#d0523e" text-anchor="middle">SURF &amp; SWIRL</text>')
    add('<rect x="590" y="760" width="62" height="40" rx="6" fill="#bfe0a5" stroke="#efe6d6" stroke-width="3"/>'       # patio
        '<circle cx="608" cy="778" r="7" fill="#ff8fb5"/><circle cx="634" cy="782" r="7" fill="#ffffff" stroke="#ff8fb5" stroke-width="2"/>')
    add(house(880, 848, 86, 58, "#fbe9ee", "#b6466c", depth=22, windows=2,
              extra='<path d="M872,806 q8,-10 16,0 q-8,10 -16,0" fill="#e8476f"/>'))
    # the great maple of Maple Lane
    add(tree(420, 975, 1.7, "maple", "#d9572b"))
    # lamps along the street
    add("".join(lamp(x, 842) for x in (40, 250, 470, 640, 820, 960)))
    # fairground: field, striped tent, ferris wheel, bunting
    add('<ellipse cx="250" cy="1290" rx="150" ry="60" fill="#a8cf86"/>')
    add('<path d="M190,1300 L250,1200 L310,1300 Z" fill="#d9572b"/><path d="M225,1300 L250,1200 L275,1300 Z" fill="#fbefe0"/><rect x="247" y="1180" width="5" height="22" fill="#5b3a26"/><path d="M252,1180 l18,6 l-18,6 Z" fill="#e0a42f"/>')
    add('<circle cx="350" cy="1230" r="46" fill="none" stroke="#5d7389" stroke-width="5"/>'
        + "".join(f'<line x1="350" y1="1230" x2="{350 + 46 * math.cos(a):.0f}" y2="{1230 + 46 * math.sin(a):.0f}" stroke="#5d7389" stroke-width="2"/>'
                  f'<rect x="{344 + 46 * math.cos(a):.0f}" y="{1226 + 46 * math.sin(a):.0f}" width="12" height="9" rx="2" fill="{["#d9572b", "#e0a42f", "#3f7fb5", "#5b9a4a"][i % 4]}"/>'
                  for i, a in enumerate([k * math.pi / 4 for k in range(8)]))
        + '<path d="M332,1290 L350,1230 L368,1290" stroke="#5d7389" stroke-width="5" fill="none"/>')
    add("".join(f'<path d="M{130 + i * 22},1330 l10,14 l10,-14 Z" fill="{["#d9572b", "#e0a42f", "#3f7fb5", "#5b9a4a"][i % 4]}"/>' for i in range(11)))
    # greenhouse with garden beds and a potting shed
    add('<rect x="770" y="1200" width="130" height="70" rx="4" fill="#d8f0f7" stroke="#ffffff" stroke-width="4"/>'
        '<path d="M762,1202 Q835,1140 908,1202 Z" fill="#c6e7f2" stroke="#ffffff" stroke-width="4"/>'
        + "".join(f'<line x1="{790 + i * 22}" y1="1202" x2="{790 + i * 22}" y2="1270" stroke="#ffffff" stroke-width="2.5"/>' for i in range(5)))
    add("".join(f'<rect x="{760 + i * 48}" y="1290" width="40" height="22" rx="4" fill="#8a5a44"/>' + "".join(f'<circle cx="{766 + i * 48 + k * 9}" cy="{1296 + (k % 2) * 8}" r="4" fill="{["#e8476f", "#e0a42f", "#a86bd9"][(i + k) % 3]}"/>' for k in range(4)) for i in range(3)))
    add(house(935, 1270, 40, 30, "#c9a074", "#6d5546", depth=10, chimney=False, windows=1))
    # beach: shack, umbrellas, rocks, pier with boats, lighthouse
    add(house(500, 1640, 80, 46, "#f6e2a8", "#3f7fb5", depth=18, chimney=False))
    add('<ellipse cx="430" cy="1625" rx="9" ry="34" fill="#e8476f" transform="rotate(-14 430 1625)"/>')
    for (ux, uc) in [(640, "#d9572b"), (720, "#3f7fb5"), (300, "#e0a42f")]:
        add(f'<path d="M{ux - 26},{1640} Q{ux},{1608} {ux + 26},{1640} Z" fill="{uc}"/><rect x="{ux - 1.5}" y="1638" width="3" height="26" fill="#6b4630"/>'
            f'<rect x="{ux - 16}" y="1666" width="30" height="12" rx="3" fill="#ffffff" opacity="0.85"/>')
    add('<path d="M560,1700 L572,1700 L572,1860 L560,1860 Z" fill="#a87d55"/><path d="M548,1700 h36" stroke="#8a5a44" stroke-width="3"/>'
        + "".join(f'<line x1="556" y1="{1712 + i * 14}" x2="576" y2="{1712 + i * 14}" stroke="#8a5a44" stroke-width="2"/>' for i in range(11)))
    for (bx, by, bc) in [(620, 1800, "#d9572b"), (470, 1840, "#3f7fb5")]:
        add(f'<path d="M{bx - 22},{by} L{bx + 22},{by} L{bx + 14},{by + 12} L{bx - 14},{by + 12} Z" fill="{bc}"/>'
            f'<line x1="{bx}" y1="{by}" x2="{bx}" y2="{by - 30}" stroke="#5b3a26" stroke-width="2.5"/><path d="M{bx + 1},{by - 30} L{bx + 18},{by - 6} L{bx + 1},{by - 6} Z" fill="#fbefe0"/>')
    add('<path d="M860,1720 Q900,1670 960,1690 Q1020,1660 1060,1720 Z" fill="#8d97a6"/><path d="M900,1700 Q930,1680 960,1695" stroke="#6f7a8a" stroke-width="4" fill="none"/>')
    add('<path d="M940,1690 L948,1560 L972,1560 L980,1690 Z" fill="#fbefe0"/><path d="M943,1650 L977,1650 L975,1625 L945,1625 Z" fill="#d9572b"/>'
        '<rect x="942" y="1540" width="36" height="22" rx="4" fill="#5d7389"/><path d="M938,1540 L960,1520 L982,1540 Z" fill="#d9572b"/>'
        '<path d="M960,1550 L1080,1520 L1080,1580 Z" fill="#fff6c8" opacity="0.35"/>')
    # dune grass and rocks
    add("".join(f'<path d="M{x},{y} q-3,-14 -8,-18 M{x},{y} q2,-16 0,-22 M{x},{y} q4,-12 10,-16" stroke="#8fa85a" stroke-width="2.5" fill="none"/>' for (x, y) in [(120, 1580), (160, 1592), (820, 1600), (780, 1590), (360, 1585)]))
    add("".join(f'<ellipse cx="{x}" cy="{y}" rx="{r}" ry="{r * 0.6}" fill="#9aa3ad"/>' for (x, y, r) in [(200, 1690, 14), (222, 1696, 9), (760, 1700, 12)]))
    # little flowers and bushes sprinkled on the grass
    for i in range(140):
        x, y = R.uniform(10, 1070), R.uniform(380, 1450)
        if 820 < y < 930 or any((x - sx) ** 2 + (y - sy) ** 2 < 70 ** 2 for (sx, sy) in SPOTS.values()): continue
        add(f'<circle cx="{x:.0f}" cy="{y:.0f}" r="2.6" fill="{R.choice(["#ffffff", "#ffd34d", "#ff8fb5", "#c9a2ff"])}" opacity="0.85"/>')
    # compass rose on the sea
    cx, cy = 120, 1830
    add(f'<circle cx="{cx}" cy="{cy}" r="44" fill="#fbefe0" opacity="0.85" stroke="#8a5a44" stroke-width="3"/>'
        f'<path d="M{cx},{cy - 40} L{cx + 9},{cy} L{cx},{cy + 40} L{cx - 9},{cy} Z" fill="#8a5a44"/>'
        f'<path d="M{cx - 40},{cy} L{cx},{cy - 9} L{cx + 40},{cy} L{cx},{cy + 9} Z" fill="#b58a6a"/>'
        f'<path d="M{cx},{cy - 40} L{cx + 9},{cy} L{cx},{cy} Z" fill="#d9572b"/>'
        f'<text x="{cx}" y="{cy - 48}" font-family="Fredoka, Arial" font-weight="700" font-size="16" fill="#5b3220" text-anchor="middle">N</text>')
    # painted texture over everything, then a soft vignette
    add(f'<image href="{noise_overlay()}" x="0" y="0" width="{MW}" height="{MH}" preserveAspectRatio="none" style="mix-blend-mode:multiply" opacity="0.55"/>')
    add('<defs><radialGradient id="vig" cx="0.5" cy="0.5" r="0.75"><stop offset="0.6" stop-color="#000" stop-opacity="0"/><stop offset="1" stop-color="#3b2a12" stop-opacity="0.35"/></radialGradient></defs>'
        f'<rect width="{MW}" height="{MH}" fill="url(#vig)"/>')
    return svg("".join(b), MW, MH)

if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    w, h = int(MW * SCALE), int(MH * SCALE)
    with tempfile.TemporaryDirectory() as d:
        png = os.path.join(d, "map.png")
        render([(build(), png, w, h)])
        im = Image.open(png).convert("RGB")
        im.save(os.path.join(OUT, "map.webp"), "WEBP", quality=86, method=6)
        fog_texture(os.path.join(OUT, "fog.webp"), w // 2, h // 2)
        if len(sys.argv) > 1:
            im.resize((675, 1200)).save(os.path.join(sys.argv[1], "map_preview.png"))
    json.dump({"w": w, "h": h, "spots": {k: [round(x * SCALE), round(y * SCALE)] for k, (x, y) in SPOTS.items()},
               "reveal": {k: round(REVEAL.get(k, DEFAULT_REVEAL) * SCALE) for k in SPOTS}},
              open(os.path.join(OUT, "map.json"), "w"), indent=1)
    for f in ("map.webp", "fog.webp"):
        print(f, os.path.getsize(os.path.join(OUT, f)) // 1024, "KB")
