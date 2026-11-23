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
    "orchard": (300, 420), "hill": (830, 350),
    "market": (230, 860), "bakery": (545, 800), "tearoom": (880, 815),
    "fair": (250, 1260), "greenhouse": (745, 1290), "beach": (520, 1650),
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

# ------------------------------------------------------------------ layout checker

def sample_path(d, step=4.0):
    """Points along a path made of M / L / Q commands (absolute), every few pixels."""
    toks = d.replace(",", " ").replace("M", " M ").replace("L", " L ").replace("Q", " Q ").split()
    pts, i, cur = [], 0, (0.0, 0.0)
    while i < len(toks):
        c = toks[i]; i += 1
        if c == "M":
            cur = (float(toks[i]), float(toks[i + 1])); i += 2; pts.append(cur)
        elif c == "L":
            p = (float(toks[i]), float(toks[i + 1])); i += 2
            n = max(1, int(math.dist(cur, p) / step))
            pts += [(cur[0] + (p[0] - cur[0]) * k / n, cur[1] + (p[1] - cur[1]) * k / n) for k in range(1, n + 1)]; cur = p
        elif c == "Q":
            q = (float(toks[i]), float(toks[i + 1])); p = (float(toks[i + 2]), float(toks[i + 3])); i += 4
            n = max(1, int((math.dist(cur, q) + math.dist(q, p)) / step))
            for k in range(1, n + 1):
                t = k / n; u = 1 - t
                pts.append((u * u * cur[0] + 2 * u * t * q[0] + t * t * p[0], u * u * cur[1] + 2 * u * t * q[1] + t * t * p[1]))
            cur = p
    return pts

def box_dist(box, p):
    x0, y0, x1, y1 = box
    dx = max(x0 - p[0], 0, p[0] - x1); dy = max(y0 - p[1], 0, p[1] - y1)
    return math.hypot(dx, dy)

class Layout:
    """Everything solid on the map claims a box, so nothing can end up on top of something else.
    bbox: what you see (buildings may not overlap each other); ground: where it stands (may not touch water or roads)."""
    def __init__(self):
        self.solids, self.water, self.roads, self.problems = [], [], [], []
    def add_water(self, d, half): self.water.append((sample_path(d), half))
    def add_pond(self, cx, cy, rx, ry):
        self.water.append(([(cx + rx * math.cos(a / 20 * math.pi) * k, cy + ry * math.sin(a / 20 * math.pi) * k) for a in range(40) for k in (0.3, 0.7, 1.0)], 4))
    def add_road(self, d, half): self.roads.append((sample_path(d), half))
    def solid(self, name, bbox, ground=None, road_ok=False, water_ok=False):
        ground = ground or bbox
        for (n, b, _) in self.solids:
            if min(b[2], bbox[2]) - max(b[0], bbox[0]) > 0 and min(b[3], bbox[3]) - max(b[1], bbox[1]) > 0:
                self.problems.append(f"{name} overlaps {n}")
        if not water_ok:
            for pts, half in self.water:
                if any(box_dist(ground, p) < half + 3 for p in pts): self.problems.append(f"{name} is in the water"); break
        if not road_ok:
            for pts, half in self.roads:
                if any(box_dist(ground, p) < half + 1 for p in pts): self.problems.append(f"{name} is on a road"); break
        self.solids.append((name, bbox, ground))
    def free(self, x, y, r):
        """Is there room for something small (a tree, a flower) at (x, y)?"""
        if not (0 < x < MW and 0 < y < MH): return False
        if any(box_dist(b, (x, y)) < r for (_, b, _) in self.solids): return False
        if any(math.dist(p, (x, y)) < half + r for pts, half in self.water for p in pts[::3]): return False
        if any(math.dist(p, (x, y)) < half + r * 0.6 for pts, half in self.roads for p in pts[::3]): return False
        return True
    def check(self):
        if self.problems:
            raise SystemExit("Map layout problems:\n  " + "\n  ".join(self.problems))

L = Layout()
ANIM = {"smoke": [], "glints": [], "boats": [], "wheel": None, "beam": None, "fountain": None}

def put_house(name, x, y, w=64, h=46, depth=18, chimney=True, **kw):
    peak = y - h - h * 0.75
    L.solid(name, (x - w / 2 - 5, peak - depth * 0.6, x + w / 2 + depth + 4, y + 4), (x - w / 2, y - depth * 0.6, x + w / 2 + depth, y))
    if chimney: ANIM["smoke"].append((x + w * 0.18 + 4, peak + 2))
    return house(x, y, w, h, depth=depth, chimney=chimney, **kw)

def put_tree(x, y, s=1.0, kind="round", c=None, check=True):
    if check and not L.free(x, y - 20 * s, 18 * s): return ""
    return tree(x, y, s, kind, c)

def forest(cx, cy, rx, ry, n, kinds=("round",), s=(0.7, 1.05)):
    pts, tries = [], 0
    while len(pts) < n and tries < n * 40:
        tries += 1
        a = R.random() * 2 * math.pi; d = math.sqrt(R.random())
        x, y = cx + math.cos(a) * rx * d, cy + math.sin(a) * ry * d
        sc = R.uniform(*s)
        if not L.free(x, y - 20 * sc, 16 * sc): continue
        if any((x - px) ** 2 + (y - py) ** 2 < 20 ** 2 for (px, py, _, _) in pts): continue
        pts.append((x, y, R.choice(kinds), sc))
    return "".join(tree(x, y, sc, k) for (x, y, k, sc) in sorted(pts, key=lambda p: p[1]))

# ------------------------------------------------------------------ the map

RIVER = "M70,300 Q118,430 86,560 Q52,700 92,830 Q110,1035 300,1032 Q430,1035 480,1075 Q540,1100 640,1085 Q820,1080 905,1220 Q965,1380 1005,1600 Q1025,1690 1060,1760"
LANE_Y = 872
ROADS = {
    "bridge road": ("M545,904 L545,1060", 18),
    "fair road": ("M545,1115 Q535,1190 470,1225 Q440,1240 415,1250", 18),
    "greenhouse road": ("M548,1115 Q585,1235 640,1292", 18),
    "beach road": ("M545,1170 Q505,1360 515,1590", 18),
    "hill road": ("M720,845 Q760,640 818,470 Q836,430 832,412", 18),
    "farm road": ("M318,845 Q312,700 302,600 Q294,520 330,472", 18),
}

def build():
    b = []
    add = b.append
    # sky, then the land
    add('<defs><linearGradient id="sky" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#9fd6f2"/><stop offset="1" stop-color="#e6f5fb"/></linearGradient></defs>')
    add(f'<rect width="{MW}" height="{MH}" fill="url(#sky)"/>')
    for (cx, cy, s) in [(160, 70, 1.0), (520, 40, 0.8), (880, 90, 1.1)]:
        add("".join(f'<ellipse cx="{cx + dx * s}" cy="{cy + dy * s}" rx="{rx * s}" ry="{ry * s}" fill="#ffffff" opacity="0.9"/>'
                    for (dx, dy, rx, ry) in [(-40, 6, 40, 18), (0, -4, 46, 26), (44, 6, 38, 18)]))
    add(f'<path d="M-20,250 L1100,250 L1100,{MH} L-20,{MH} Z" fill="#93bf6e"/>')
    # one mountain range: every peak rises out of the same rock, and the foothills cover the base
    peaks = [(-30, 120), (110, 70), (260, 130), (420, 60), (590, 125), (760, 50), (930, 110), (1090, 80)]
    ridge = "M-40,380 " + " ".join(f"L{x - 75},{top + 120} L{x},{top}" for (x, top) in peaks) + " L1120,200 L1120,380 Z"
    add(f'<path d="{ridge}" fill="#8d97a6"/>')
    for (x, top) in peaks:
        add(f'<path d="M{x},{top} L{x + 120},{top + 200} L{x + 40},{top + 260} L{x + 6},{top + 260} Z" fill="#717c8c"/>')
        add(f'<path d="M{x},{top} L{x + 30},{top + 46} L{x + 12},{top + 40} L{x - 4},{top + 56} L{x - 24},{top + 42} Z" fill="#f4f7fb"/>')
    add('<path d="M-20,300 Q140,215 300,270 Q460,205 640,262 Q800,200 1100,255 V470 H-20 Z" fill="#7faa5c"/>')
    add('<path d="M-20,345 Q200,275 380,335 Q560,285 760,345 Q920,300 1100,335 V480 H-20 Z" fill="#88b363"/>')
    add('<path d="M640,480 Q700,260 860,235 Q1020,230 1100,320 V540 Z" fill="#7aa758"/>')        # June's hill
    for (cx, cy, rx, ry, c) in [(300, 640, 380, 220, "#8bb766"), (800, 1000, 360, 240, "#9bc677"), (200, 1150, 260, 200, "#89b464"),
                                (700, 600, 300, 160, "#9fc87a"), (500, 1350, 400, 140, "#97c272")]:
        add(f'<ellipse cx="{cx}" cy="{cy}" rx="{rx}" ry="{ry}" fill="{c}" opacity="0.8"/>')
    # sea and beach
    add(f'<path d="M-20,1500 Q240,1440 520,1480 Q820,1530 1100,1470 V{MH} H-20 Z" fill="#e8d29b"/>')
    add(f'<path d="M-20,1540 Q260,1480 520,1520 Q820,1570 1100,1510 V{MH} H-20 Z" fill="#f1dfae"/>')
    add('<defs><linearGradient id="sea" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#7fcbe0"/><stop offset="1" stop-color="#2f7fa8"/></linearGradient></defs>')
    shore = "M-20,1720 Q260,1680 540,1715 Q820,1755 1100,1700"
    add(f'<path d="{shore} V{MH} H-20 Z" fill="url(#sea)"/>')
    add(f'<path d="{shore}" stroke="#ffffff" stroke-width="7" fill="none" opacity="0.75"/>')
    L.water.append(([(x, 1720 + 40 * ((x / 1100) ** 2)) for x in range(-20, 1100, 20)], 0))
    for i in range(26):
        x = R.uniform(20, 1060); y = R.uniform(1770, 1900)
        add(f'<path d="M{x:.0f},{y:.0f} q12,-8 24,0 q12,8 24,0" stroke="#ffffff" stroke-width="3" fill="none" opacity="{R.uniform(0.35, 0.7):.2f}" stroke-linecap="round"/>')
    for i in range(14): ANIM["glints"].append((R.uniform(60, 1040), R.uniform(1770, 1900)))
    # the river, from the mountains to the sea
    add(f'<path d="{RIVER}" stroke="#d8c48f" stroke-width="58" fill="none" stroke-linecap="round"/>')
    add(f'<path d="{RIVER}" stroke="#5fb3d6" stroke-width="40" fill="none" stroke-linecap="round"/>')
    add(f'<path d="{RIVER}" stroke="#8fd0e8" stroke-width="12" fill="none" stroke-linecap="round" stroke-dasharray="26 34" opacity="0.8"/>')
    L.add_water(RIVER, 29)
    rp = sample_path(RIVER, 60)
    ANIM["glints"] += [p for p in rp[1:-1]]
    # waterfall where the river leaves the mountains
    add('<path d="M58,300 Q70,282 82,300 L86,330 L54,330 Z" fill="#cdeefa"/>')
    # pond by June's hill
    add('<ellipse cx="960" cy="580" rx="70" ry="36" fill="#d8c48f"/><ellipse cx="960" cy="580" rx="62" ry="30" fill="#5fb3d6"/>')
    add("".join(f'<circle cx="{x}" cy="{y}" r="6" fill="#4f8c43"/><circle cx="{x + 2}" cy="{y - 2}" r="2" fill="#ff8fb5"/>' for (x, y) in [(935, 572), (975, 586), (990, 568)]))
    L.add_pond(960, 580, 66, 34)
    ANIM["glints"] += [(940, 590), (975, 575)]
    # roads (claimed before anything is placed, so nothing is built on them)
    for name, (d, half) in ROADS.items():
        add(road(d)); L.add_road(d, half)
    add('<path d="M-20,872 L1100,872" stroke="#9e9488" stroke-width="64" fill="none"/>')
    add('<path d="M-20,872 L1100,872" stroke="#cfc6b8" stroke-width="56" fill="none"/>')
    add("".join(f'<rect x="{x}" y="{850 + (i % 3) * 15}" width="22" height="11" rx="3" fill="#bdb3a3" opacity="0.8"/>' for i, x in enumerate(range(-10, 1090, 17))))
    L.add_road("M-20,872 L1100,872", 32)
    # stone bridges where Maple Lane and the bridge road cross the river
    add('<rect x="56" y="834" width="80" height="8" rx="3" fill="#8f8579"/><rect x="56" y="902" width="80" height="8" rx="3" fill="#8f8579"/>'
        + "".join(f'<rect x="{60 + i * 17}" y="824" width="6" height="12" fill="#8f8579"/><rect x="{60 + i * 17}" y="908" width="6" height="12" fill="#8f8579"/>' for i in range(5)))
    add('<path d="M505,1055 L585,1055 L585,1120 L505,1120 Z" fill="#b8ada0"/><path d="M505,1060 h80 M505,1115 h80" stroke="#8f8579" stroke-width="5"/>'
        '<path d="M515,1072 q30,-14 60,0" stroke="#d6ccbe" stroke-width="3" fill="none"/>')
    L.add_road("M545,1055 L545,1120", 40)

    # --- the farm and orchard (north-west, east of the river)
    add(field(140, 470, 125, 72, "#d9b85c", 6, -4)); L.solid("wheat field", (132, 460, 273, 552))
    add(field(140, 562, 125, 66, "#9cbf5a", 5, 3)); L.solid("cabbage field", (132, 554, 273, 636))
    orchard = ""
    for r_ in range(3):
        for c_ in range(4):
            x, y = 175 + c_ * 40 + (r_ % 2) * 18, 335 + r_ * 34
            orchard += tree(x, y, 0.6, "round", "#5b9a4a") + f'<circle cx="{x - 5}" cy="{y - 20}" r="3" fill="#e8442f"/><circle cx="{x + 5}" cy="{y - 14}" r="3" fill="#e8442f"/>'
    add(orchard); L.solid("orchard", (160, 300, 310, 410))
    add(put_house("red barn", 385, 452, 70, 50, depth=20, chimney=False, wall="#b8443a", roof="#7a2d26", windows=1))
    add('<rect x="482" y="382" width="22" height="70" rx="10" fill="#c9cfd6"/><ellipse cx="493" cy="382" rx="11" ry="6" fill="#9aa3ad"/>'); L.solid("silo", (480, 374, 506, 454))
    add("".join(f'<ellipse cx="{x}" cy="{y}" rx="12" ry="9" fill="#e3c26a"/><path d="M{x - 10},{y} q10,-6 20,0" stroke="#c9a54f" stroke-width="2" fill="none"/>' for (x, y) in [(440, 500), (468, 512), (496, 498)]))
    L.solid("haystacks", (426, 488, 510, 522))
    # windmill by the fields (its sails turn in the app)
    add('<path d="M206,742 L216,688 L238,688 L248,742 Z" fill="#efe2c9"/><path d="M212,692 L227,670 L242,692 Z" fill="#b5553c"/><rect x="222" y="722" width="10" height="20" rx="2" fill="#8a5a44"/>')
    L.solid("windmill", (188, 648, 266, 744)); ANIM["mill"] = (227, 690)

    # --- Maple Lane, north side: market stalls, houses, the bakery and its patio, the tea room
    for (sx, c) in [(160, 0), (195, 1), (240, 2), (275, 3)]:
        col = ["#d9572b", "#3f7fb5", "#e0a42f", "#5b9a4a"][c]
        add(f'<rect x="{sx - 14}" y="814" width="28" height="22" fill="#c9a074"/>'
            + "".join(f'<path d="M{sx - 16 + k * 8},{804} l8,0 l0,10 q-4,5 -8,0 Z" fill="{col if k % 2 == 0 else "#f6efe2"}"/>' for k in range(4)))
    L.solid("market stalls", (142, 800, 292, 838))
    palette = [("#f1e3cc", "#b5553c"), ("#e5d8c3", "#5d7389"), ("#efe1d6", "#6d8a4f"), ("#e8d2b8", "#8a5a44"), ("#dfe5ea", "#b5553c")]
    add(put_house("house by the farm road", 384, 838, 56, 42, depth=16, wall=palette[0][0], roof=palette[0][1]))
    add(put_house("bakery", 545, 838, 110, 70, depth=26, windows=3, wall="#f6cfd6", roof="#d0523e",
                  extra='<rect x="498" y="793" width="94" height="10" fill="#ff8fb5"/>' + "".join(f'<path d="M{490 + k * 14},{803} l14,0 l0,8 q-7,6 -14,0 Z" fill="{"#ff8fb5" if k % 2 == 0 else "#ffffff"}"/>' for k in range(8))))
    add('<rect x="510" y="738" width="70" height="16" rx="4" fill="#ffffff" stroke="#d0523e" stroke-width="2"/>'
        '<text x="545" y="750" font-family="Fredoka, Arial Rounded MT Bold, Arial" font-weight="700" font-size="9" fill="#d0523e" text-anchor="middle">SURF &amp; SWIRL</text>')
    add('<rect x="636" y="786" width="56" height="46" rx="6" fill="#bfe0a5" stroke="#efe6d6" stroke-width="3"/>'
        '<circle cx="652" cy="802" r="7" fill="#ff8fb5"/><circle cx="676" cy="816" r="7" fill="#ffffff" stroke="#ff8fb5" stroke-width="2"/>')
    L.solid("patio", (634, 784, 694, 834))
    add(put_house("tea room", 880, 838, 86, 58, depth=22, windows=2, wall="#fbe9ee", roof="#b6466c",
                  extra='<path d="M872,796 q8,-10 16,0 q-8,10 -16,0" fill="#e8476f"/>'))
    add(put_house("house east", 1020, 838, 56, 42, depth=16, wall=palette[1][0], roof=palette[1][1]))

    # --- south side: the market plaza with its fountain, houses, the great maple
    add('<ellipse cx="292" cy="928" rx="90" ry="30" fill="#cfc6b8" stroke="#9e9488" stroke-width="4"/>')
    add('<ellipse cx="292" cy="928" rx="24" ry="10" fill="#9e9488"/><ellipse cx="292" cy="925" rx="18" ry="7" fill="#8fd0e8"/>')
    L.solid("plaza", (200, 904, 384, 960), road_ok=True); ANIM["fountain"] = (292, 922)
    add(put_house("house south 1", 655, 968, 52, 40, depth=14, wall=palette[2][0], roof=palette[2][1]))
    add(put_house("house south 2", 770, 968, 52, 40, depth=14, wall=palette[3][0], roof=palette[3][1]))
    add(put_house("house south 3", 960, 968, 52, 40, depth=14, wall=palette[4][0], roof=palette[4][1]))
    add(tree(430, 990, 1.6, "maple", "#d9572b")); L.solid("great maple", (395, 905, 470, 994), (420, 975, 440, 994))
    # lamps along the street (south kerb)
    for x in (500, 600, 720, 870, 1040):
        add(lamp(x, 940)); L.solid(f"lamp {x}", (x - 6, 906, x + 6, 942))

    # --- June's cottage on the hill
    add('<path d="M740,392 Q830,352 930,394" stroke="#b5ab9c" stroke-width="7" fill="none" stroke-dasharray="10 4"/>')
    add(put_house("June's cottage", 830, 380, 84, 54, depth=22, wall="#efe2c9", roof="#6d5546"))
    add(put_tree(760, 360, 0.8, "pine") + put_tree(915, 352, 0.85, "pine") + put_tree(945, 378, 0.7, "pine"))

    # --- the fairground: tent, the Ferris wheel's stand (the wheel turns in the app), bunting
    add('<ellipse cx="250" cy="1290" rx="150" ry="60" fill="#a8cf86"/>'); L.solid("fair field", (100, 1230, 400, 1350), road_ok=True)
    add('<path d="M180,1310 L240,1210 L300,1310 Z" fill="#d9572b"/><path d="M215,1310 L240,1210 L265,1310 Z" fill="#fbefe0"/><rect x="237" y="1190" width="5" height="22" fill="#5b3a26"/><path d="M242,1190 l18,6 l-18,6 Z" fill="#e0a42f"/>')
    add('<path d="M326,1310 L345,1240 L364,1310" stroke="#5d7389" stroke-width="6" fill="none"/><circle cx="345" cy="1240" r="5" fill="#5d7389"/>')
    ANIM["wheel"] = (345, 1240, 48)
    add("".join(f'<path d="M{140 + i * 22},1335 l10,14 l10,-14 Z" fill="{["#d9572b", "#e0a42f", "#3f7fb5", "#5b9a4a"][i % 4]}"/>' for i in range(10)))

    # --- Bea's greenhouse, garden beds, potting shed
    add('<rect x="680" y="1262" width="130" height="68" rx="4" fill="#d8f0f7" stroke="#ffffff" stroke-width="4"/>'
        '<path d="M672,1264 Q745,1200 818,1264 Z" fill="#c6e7f2" stroke="#ffffff" stroke-width="4"/>'
        + "".join(f'<line x1="{700 + i * 22}" y1="1264" x2="{700 + i * 22}" y2="1330" stroke="#ffffff" stroke-width="2.5"/>' for i in range(5)))
    L.solid("greenhouse", (668, 1205, 822, 1334))
    add("".join(f'<rect x="{675 + i * 48}" y="1348" width="40" height="22" rx="4" fill="#8a5a44"/>' + "".join(f'<circle cx="{681 + i * 48 + k * 9}" cy="{1354 + (k % 2) * 8}" r="4" fill="{["#e8476f", "#e0a42f", "#a86bd9"][(i + k) % 3]}"/>' for k in range(4)) for i in range(3)))
    L.solid("garden beds", (673, 1346, 813, 1372))
    add(put_house("potting shed", 850, 1330, 40, 30, depth=10, chimney=False, wall="#c9a074", roof="#6d5546", windows=1))

    # --- the beach: Kai's shack, umbrellas, surfboard, pier, lighthouse on the rocks
    add(put_house("beach shack", 515, 1640, 80, 46, depth=18, chimney=False, wall="#f6e2a8", roof="#3f7fb5"))
    add('<ellipse cx="440" cy="1625" rx="9" ry="34" fill="#e8476f" transform="rotate(-14 440 1625)"/>')
    for (ux, uc) in [(660, "#d9572b"), (740, "#3f7fb5"), (330, "#e0a42f")]:
        add(f'<path d="M{ux - 26},{1640} Q{ux},{1608} {ux + 26},{1640} Z" fill="{uc}"/><rect x="{ux - 1.5}" y="1638" width="3" height="26" fill="#6b4630"/>'
            f'<rect x="{ux - 16}" y="1666" width="30" height="12" rx="3" fill="#ffffff" opacity="0.85"/>')
        L.solid(f"umbrella {ux}", (ux - 28, 1606, ux + 28, 1680))
    add('<path d="M594,1712 L606,1712 L606,1870 L594,1870 Z" fill="#a87d55"/><path d="M582,1712 h36" stroke="#8a5a44" stroke-width="3"/>'
        + "".join(f'<line x1="590" y1="{1724 + i * 14}" x2="610" y2="{1724 + i * 14}" stroke="#8a5a44" stroke-width="2"/>' for i in range(11)))
    ANIM["boats"] = [(660, 1810, 0), (480, 1850, 1), (830, 1880, 2)]
    add('<path d="M90,1730 Q120,1670 170,1690 Q220,1660 250,1730 Z" fill="#8d97a6"/><path d="M120,1708 Q150,1688 180,1700" stroke="#6f7a8a" stroke-width="4" fill="none"/>')
    add('<path d="M150,1692 L158,1562 L182,1562 L190,1692 Z" fill="#fbefe0"/><path d="M153,1650 L187,1650 L185,1625 L155,1625 Z" fill="#d9572b"/>'
        '<rect x="152" y="1542" width="36" height="22" rx="4" fill="#5d7389"/><path d="M148,1542 L170,1522 L192,1542 Z" fill="#d9572b"/>')
    L.solid("lighthouse", (86, 1520, 254, 1732), water_ok=True); ANIM["beam"] = (170, 1553)

    # --- trees: forests fill whatever free ground is left in each area
    add(forest(560, 360, 130, 70, 30, ("pine", "round"), s=(0.75, 1.05)))
    add(forest(40, 640, 40, 200, 10, ("round", "maple")))
    add(forest(1030, 680, 70, 150, 18, ("pine", "round")))
    add(forest(640, 640, 90, 90, 14, ("maple", "round")))
    add(forest(820, 560, 70, 60, 8, ("round", "pine")))
    add(forest(130, 1080, 140, 110, 24, ("round", "maple", "pine")))
    add(forest(990, 1050, 90, 110, 16, ("pine", "round")))
    add(forest(400, 1430, 130, 50, 12, ("round",)))
    add(forest(760, 1440, 120, 50, 10, ("round", "maple")))
    add(forest(880, 1150, 60, 60, 6, ("round", "maple")))
    add(forest(470, 600, 110, 90, 12, ("round", "maple")))
    # dune grass, rocks, flowers
    add("".join(f'<path d="M{x},{y} q-3,-14 -8,-18 M{x},{y} q2,-16 0,-22 M{x},{y} q4,-12 10,-16" stroke="#8fa85a" stroke-width="2.5" fill="none"/>' for (x, y) in [(300, 1580), (340, 1592), (820, 1600), (870, 1590), (420, 1585)]))
    for i in range(220):
        x, y = R.uniform(10, 1070), R.uniform(400, 1460)
        if not L.free(x, y, 6): continue
        add(f'<circle cx="{x:.0f}" cy="{y:.0f}" r="2.6" fill="{R.choice(["#ffffff", "#ffd34d", "#ff8fb5", "#c9a2ff"])}" opacity="0.85"/>')
    # compass rose on the sea
    cx, cy = 960, 1850
    add(f'<circle cx="{cx}" cy="{cy}" r="44" fill="#fbefe0" opacity="0.85" stroke="#8a5a44" stroke-width="3"/>'
        f'<path d="M{cx},{cy - 40} L{cx + 9},{cy} L{cx},{cy + 40} L{cx - 9},{cy} Z" fill="#8a5a44"/>'
        f'<path d="M{cx - 40},{cy} L{cx},{cy - 9} L{cx + 40},{cy} L{cx},{cy + 9} Z" fill="#b58a6a"/>'
        f'<path d="M{cx},{cy - 40} L{cx + 9},{cy} L{cx},{cy} Z" fill="#d9572b"/>'
        f'<text x="{cx}" y="{cy - 48}" font-family="Fredoka, Arial" font-weight="700" font-size="16" fill="#5b3220" text-anchor="middle">N</text>')
    add(f'<image href="{noise_overlay()}" x="0" y="0" width="{MW}" height="{MH}" preserveAspectRatio="none" style="mix-blend-mode:multiply" opacity="0.55"/>')
    add('<defs><radialGradient id="vig" cx="0.5" cy="0.5" r="0.75"><stop offset="0.6" stop-color="#000" stop-opacity="0"/><stop offset="1" stop-color="#3b2a12" stop-opacity="0.35"/></radialGradient></defs>'
        f'<rect width="{MW}" height="{MH}" fill="url(#vig)"/>')
    L.check()
    return svg("".join(b), MW, MH)

def anim_json():
    sc = lambda p: [round(v * SCALE) for v in p]
    return {"smoke": [sc(p) for p in ANIM["smoke"]], "glints": [sc(p) for p in ANIM["glints"]],
            "boats": [sc(p[:2]) + [p[2]] for p in ANIM["boats"]], "wheel": sc(ANIM["wheel"]),
            "beam": sc(ANIM["beam"]), "fountain": sc(ANIM["fountain"]), "mill": sc(ANIM["mill"])}

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
               "reveal": {k: round(REVEAL.get(k, DEFAULT_REVEAL) * SCALE) for k in SPOTS}, "anims": anim_json()},
              open(os.path.join(OUT, "map.json"), "w"), indent=1)
    for f in ("map.webp", "fog.webp"):
        print(f, os.path.getsize(os.path.join(OUT, f)) // 1024, "KB")
