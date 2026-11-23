"""The Maple Lane town map. Each location has a spot (exported to assets/scenes/map.json) where the app puts a
pin. New locations: add a building here and a Location in Story.kt with the same id.

Run: python3 tools/scenes/map.py [preview_dir]   (from the repo root)
"""
import json, math, os, sys, tempfile
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from common import svg, DEFS
from render import render
from PIL import Image

MW, MH = 1080, 1920
SCALE = 1.25
OUT = "android/app/src/main/assets/scenes"

# id -> (x, y) of the pin, in map units
SPOTS = {
    "orchard": (250, 330),
    "hill": (800, 300),
    "market": (210, 760),
    "bakery": (545, 760),
    "tearoom": (880, 760),
    "fair": (280, 1180),
    "greenhouse": (820, 1200),
    "beach": (560, 1650),
}

def tree(x, y, s=1.0, c="#5bb35d", fruit=None):
    f = ""
    if fruit:
        f = "".join(f'<circle cx="{x + dx * s}" cy="{y - 40 * s + dy * s}" r="{5 * s}" fill="{fruit}"/>' for dx, dy in [(-14, -6), (10, -14), (16, 6), (-6, 10), (2, -24)])
    return (f'<ellipse cx="{x}" cy="{y + 4 * s}" rx="{26 * s}" ry="{8 * s}" fill="#3d7a3a" opacity="0.25"/>'
            f'<rect x="{x - 5 * s}" y="{y - 22 * s}" width="{10 * s}" height="{26 * s}" fill="#8a5a3c"/>'
            f'<circle cx="{x}" cy="{y - 40 * s}" r="{30 * s}" fill="{c}"/><circle cx="{x - 10 * s}" cy="{y - 50 * s}" r="{12 * s}" fill="#ffffff" opacity="0.18"/>{f}')

def house(x, y, w, h, wall, roof, door="#8a5234", awning=None, sign=None, chimney=False):
    """A little building with its front-bottom centre at (x, y)."""
    l = x - w / 2
    out = f'<ellipse cx="{x}" cy="{y + 6}" rx="{w * 0.62}" ry="12" fill="#2f5a2c" opacity="0.22"/>'
    if chimney:
        out += f'<rect x="{x + w * 0.22}" y="{y - h - 58}" width="18" height="44" fill="#a5503c"/>'
        out += "".join(f'<circle cx="{x + w * 0.22 + 9 + i * 8}" cy="{y - h - 70 - i * 18}" r="{9 + i * 3}" fill="#ffffff" opacity="{0.7 - i * 0.18}"/>' for i in range(3))
    out += f'<rect x="{l}" y="{y - h}" width="{w}" height="{h}" rx="6" fill="{wall}"/>'
    out += f'<path d="M{l - 14},{y - h + 4} L{x},{y - h - 58} L{l + w + 14},{y - h + 4} Z" fill="{roof}"/>'
    out += f'<rect x="{x - 14}" y="{y - 46}" width="28" height="46" rx="5" fill="{door}"/>'
    out += f'<rect x="{l + 12}" y="{y - h + 22}" width="26" height="24" rx="4" fill="#cfefff" stroke="#ffffff" stroke-width="3"/>'
    out += f'<rect x="{l + w - 38}" y="{y - h + 22}" width="26" height="24" rx="4" fill="#cfefff" stroke="#ffffff" stroke-width="3"/>'
    if awning:
        out += "".join(f'<path d="M{l + i * w / 6},{y - 56} L{l + (i + 1) * w / 6},{y - 56} L{l + (i + 1) * w / 6},{y - 44} Q{l + (i + 0.5) * w / 6},{y - 34} {l + i * w / 6},{y - 44} Z" fill="{awning if i % 2 == 0 else "#ffffff"}"/>' for i in range(6))
    if sign:
        out += f'<rect x="{x - 44}" y="{y - h - 16}" width="88" height="20" rx="6" fill="#ffffff" stroke="{roof}" stroke-width="3"/>'
        out += f'<text x="{x}" y="{y - h - 1}" font-family="Fredoka, Arial Rounded MT Bold, Arial" font-weight="700" font-size="12" fill="{roof}" text-anchor="middle">{sign}</text>'
    return f'<g filter="url(#shadow)">{out}</g>'

def stall(x, y, c):
    return (f'<g filter="url(#shadow)"><rect x="{x - 36}" y="{y - 40}" width="72" height="40" fill="#d9a066"/>'
            + "".join(f'<path d="M{x - 42 + i * 14},{y - 64} L{x - 28 + i * 14},{y - 64} L{x - 28 + i * 14},{y - 46} Q{x - 35 + i * 14},{y - 38} {x - 42 + i * 14},{y - 46} Z" fill="{c if i % 2 == 0 else "#ffffff"}"/>' for i in range(6))
            + f'<circle cx="{x - 16}" cy="{y - 44}" r="7" fill="#ff5a5a"/><circle cx="{x}" cy="{y - 46}" r="7" fill="#ffd34d"/><circle cx="{x + 16}" cy="{y - 44}" r="7" fill="#7fcf7a"/></g>')

def build():
    # ground, hills, river, roads
    body = f'<rect width="{MW}" height="{MH}" fill="#a8e08f"/>'
    body += '<path d="M0,0 H1080 V430 Q860,520 640,450 Q420,380 220,470 Q90,530 0,470 Z" fill="#8fd27a"/>'
    body += '<path d="M560,560 Q640,170 820,150 Q1000,140 1080,240 V560 Z" fill="#7cc46a"/>'
    # river
    body += '<path d="M-20,980 Q200,940 360,1010 Q540,1090 700,1020 Q880,940 1100,1000" stroke="#7fd0f2" stroke-width="70" fill="none" stroke-linecap="round"/>'
    body += '<path d="M-20,980 Q200,940 360,1010 Q540,1090 700,1020 Q880,940 1100,1000" stroke="#b5e7fa" stroke-width="18" fill="none" opacity="0.7" stroke-dasharray="30 40"/>'
    # beach and sea
    body += f'<path d="M0,1470 Q300,1420 560,1460 Q820,1500 1080,1450 V{MH} H0 Z" fill="#f6dfa6"/>'
    body += f'<path d="M0,1700 Q270,1660 540,1700 Q810,1740 1080,1690 V{MH} H0 Z" fill="#5cc3e8"/>'
    body += "".join(f'<path d="M{x},{1750 + (i % 3) * 40} q20,-14 40,0 q20,14 40,0" stroke="#ffffff" stroke-width="5" fill="none" opacity="0.8" stroke-linecap="round"/>' for i, x in enumerate(range(40, 1080, 130)))
    # roads (cream paths)
    road = 'stroke="#f7ead2" stroke-width="44" fill="none" stroke-linecap="round" stroke-linejoin="round"'
    body += f'<path d="M-20,820 L1100,820" {road}/>'                                 # Maple Lane, main street
    body += f'<path d="M545,820 L545,1130 Q545,1200 470,1250 L300,1250" {road}/>'      # down to the fair (bridge)
    body += f'<path d="M545,1130 Q600,1210 760,1250" {road}/>'                         # to the greenhouse
    body += f'<path d="M545,1250 Q520,1420 560,1560" {road}/>'                         # down to the beach
    body += f'<path d="M700,820 Q740,560 800,380" {road}/>'                            # up the hill
    body += f'<path d="M300,820 Q280,600 260,420" {road}/>'                            # to the orchard
    body += '<rect x="500" y="1000" width="90" height="80" rx="8" fill="#c98b5e"/><rect x="500" y="1000" width="90" height="10" fill="#a5703f"/><rect x="500" y="1070" width="90" height="10" fill="#a5703f"/>'  # bridge
    body += f'<path d="M-20,820 L1100,820" stroke="#ffffff" stroke-width="3" stroke-dasharray="18 22" fill="none" opacity="0.7"/>'
    # pond
    body += '<ellipse cx="900" cy="560" rx="70" ry="34" fill="#7fd0f2"/><ellipse cx="880" cy="552" rx="30" ry="8" fill="#ffffff" opacity="0.4"/>'
    # trees all around
    for (x, y, s) in [(60, 120, 1.1), (140, 200, 0.9), (420, 160, 1.0), (520, 260, 0.8), (980, 470, 1.0), (60, 640, 0.9), (420, 620, 0.8),
                      (1010, 650, 0.9), (60, 1120, 1.0), (440, 1350, 0.9), (1000, 1360, 1.0), (140, 1390, 0.8), (700, 1390, 0.8),
                      (660, 640, 0.7), (980, 120, 0.9), (40, 900, 0.8), (1040, 900, 0.7)]:
        body += tree(x, y, s)
    # orchard: rows of fruit trees + barn
    for r in range(2):
        for c in range(3):
            body += tree(150 + c * 80, 300 + r * 70, 0.8, "#5bb35d", "#ff5a5a" if (r + c) % 2 == 0 else "#ffb33d")
    body += house(360, 420, 110, 80, "#e8543f", "#a5332a", door="#7a2a20")
    # hill cottage
    body += house(820, 330, 140, 100, "#f6ead8", "#7a4a34", chimney=True)
    body += '<path d="M700,380 Q760,350 820,380 Q880,350 940,380" stroke="#ffffff" stroke-width="6" fill="none" opacity="0.6"/>'
    # main street: market, bakery (with patio behind), tea room
    body += stall(150, 800, "#ff6f9a") + stall(250, 800, "#3fa7f5") + stall(200, 730, "#ffd34d")
    body += '<rect x="600" y="620" width="120" height="70" rx="12" fill="#c9f0b8" stroke="#ffffff" stroke-width="5"/>'  # patio
    body += tree(700, 650, 0.5) + '<circle cx="640" cy="650" r="14" fill="#ff6f9a"/><circle cx="640" cy="650" r="5" fill="#ffffff"/>'
    body += house(545, 800, 190, 130, "#ffc9d6", "#e8543f", awning="#ff6f9a", sign="SURF &amp; SWIRL", chimney=True)
    body += house(880, 800, 140, 110, "#fff1f4", "#d94a86", awning="#ff9fbe", sign="TEA ROOM")
    # fair: tent + flags
    body += ('<g filter="url(#shadow)"><path d="M200,1260 L280,1130 L360,1260 Z" fill="#ff6f9a"/><path d="M240,1260 L280,1130 L320,1260 Z" fill="#ffffff"/>'
             '<rect x="276" y="1100" width="6" height="34" fill="#8a5234"/><path d="M282,1100 l26,8 l-26,8 Z" fill="#ffd34d"/></g>')
    body += "".join(f'<path d="M{150 + i * 30},{1290} l12,18 l12,-18 Z" fill="{["#ff6f9a", "#ffd34d", "#6fcfb4", "#7aa0ff"][i % 4]}"/>' for i in range(9))
    # greenhouse
    body += ('<g filter="url(#shadow)"><rect x="740" y="1170" width="160" height="80" rx="6" fill="#d9f4ff" stroke="#ffffff" stroke-width="5"/>'
             '<path d="M730,1172 Q820,1100 910,1172 Z" fill="#c4ecff" stroke="#ffffff" stroke-width="5"/>'
             + "".join(f'<line x1="{760 + i * 30}" y1="1172" x2="{760 + i * 30}" y2="1250" stroke="#ffffff" stroke-width="3"/>' for i in range(5))
             + "".join(f'<circle cx="{760 + i * 32}" cy="1236" r="10" fill="{["#ff6f9a", "#ffd34d", "#b77dff", "#7fcf7a", "#ff8f3d"][i]}"/>' for i in range(5)) + '</g>')
    # beach shack + surfboard + umbrella
    body += house(600, 1610, 130, 80, "#ffe7a8", "#3fa7f5", awning="#3fa7f5")
    body += '<g filter="url(#shadow)"><ellipse cx="470" cy="1580" rx="14" ry="50" fill="#ff6f9a" transform="rotate(-15 470 1580)"/><path d="M462,1548 l16,64" stroke="#ffffff" stroke-width="4"/></g>'
    body += '<g filter="url(#shadow)"><path d="M780,1590 Q820,1540 860,1590 Z" fill="#ffd34d"/><path d="M800,1590 Q820,1555 840,1590 Z" fill="#ffffff"/><rect x="818" y="1588" width="4" height="40" fill="#8a5a3c"/></g>'
    # little flowers sprinkled
    body += "".join(f'<circle cx="{(i * 173) % 1060 + 10}" cy="{(i * 97) % 1350 + 60}" r="4" fill="{["#ff6f9a", "#ffffff", "#ffd34d"][i % 3]}" opacity="0.8"/>' for i in range(60))
    return svg(body, MW, MH)

if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    w, h = int(MW * SCALE), int(MH * SCALE)
    with tempfile.TemporaryDirectory() as d:
        png = os.path.join(d, "map.png")
        render([(build(), png, w, h)])
        im = Image.open(png).convert("RGB")
        im.save(os.path.join(OUT, "map.webp"), "WEBP", quality=86, method=6)
        if len(sys.argv) > 1:
            im.resize((540, 960)).save(os.path.join(sys.argv[1], "map_preview.png"))
    json.dump({"w": w, "h": h, "spots": {k: [round(x * SCALE), round(y * SCALE)] for k, (x, y) in SPOTS.items()}},
              open(os.path.join(OUT, "map.json"), "w"), indent=1)
    print("map", w, "x", h, os.path.getsize(os.path.join(OUT, "map.webp")) // 1024, "KB")
