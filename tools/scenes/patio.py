"""Chapter 2 scene: Surf's sunny patio behind the bakery."""
import math
from common import *

GROUND = 560

def base():
    bricks = ""
    for row in range(0, 22):
        y = 96 + row * 22
        off = 0 if row % 2 == 0 else 24
        for x in range(-24 + off, 380, 48):
            bricks += f'<rect x="{x + 2}" y="{y + 2}" width="44" height="18" rx="4" fill="#f4b3a2" opacity="{0.55 + 0.25 * ((x + row * 7) % 3) / 2:.2f}"/>'
    posts = "".join(f'<path d="M{x},{GROUND} L{x},{432} L{x + 14},{414} L{x + 28},{432} L{x + 28},{GROUND} Z" fill="#fffdf8" stroke="#eadfce" stroke-width="2"/>' for x in range(392, W, 44))
    stones = ""
    for r in range(5):
        y = 600 + r * 70 + r * r * 4
        for c in range(-1, 9):
            x = c * 140 + (70 if r % 2 else 0) - r * 6
            stones += f'<rect x="{x}" y="{y}" width="{124 + r * 6}" height="{52 + r * 6}" rx="18" fill="#efe1cf" stroke="#d8c3aa" stroke-width="3"/>'
    return f'''
    <rect width="{W}" height="{GROUND}" fill="url(#sky)"/>
    <g fill="#ffffff" opacity="0.9"><circle cx="560" cy="120" r="34"/><circle cx="600" cy="108" r="44"/><circle cx="645" cy="122" r="30"/>
      <circle cx="820" cy="200" r="24"/><circle cx="850" cy="190" r="32"/><circle cx="882" cy="202" r="22"/></g>
    <path d="M380,420 Q520,330 680,400 Q820,320 1080,390 L1080,{GROUND} L380,{GROUND} Z" fill="#a6dc8c"/>
    <path d="M380,450 Q560,400 760,440 Q900,410 1080,440 L1080,{GROUND} L380,{GROUND} Z" fill="#8ccf73"/>
    <circle cx="470" cy="380" r="42" fill="#6cc46a"/><rect x="464" y="400" width="12" height="40" fill="#8a5a3c"/>
    <circle cx="1010" cy="360" r="50" fill="#5bb35d"/><rect x="1004" y="390" width="12" height="50" fill="#8a5a3c"/>
    {posts}
    <rect x="380" y="470" width="{W - 380}" height="12" fill="#fffdf8"/>
    <!-- bakery back wall -->
    <rect x="0" y="90" width="380" height="{GROUND - 90}" fill="#ffc9b8"/>
    <clipPath id="wallClip"><rect x="0" y="90" width="380" height="{GROUND - 90}"/></clipPath>
    <g clip-path="url(#wallClip)">{bricks}</g>
    <path d="M-10,96 L390,96 L370,60 L10,60 Z" fill="#e8705f"/>
    <rect x="-10" y="90" width="400" height="12" fill="#c95a4b"/>
    <rect x="40" y="170" width="110" height="110" rx="10" fill="#ffffff"/><rect x="50" y="180" width="90" height="90" fill="url(#sky)"/>
    <rect x="92" y="180" width="6" height="90" fill="#fff"/><rect x="50" y="222" width="90" height="6" fill="#fff"/>
    <rect x="200" y="300" width="130" height="{GROUND - 300}" rx="10" fill="#ffffff"/>
    <rect x="212" y="312" width="106" height="{GROUND - 312}" rx="6" fill="#ff8fb5"/>
    <circle cx="300" cy="440" r="7" fill="url(#brass)"/>
    <rect x="186" y="{GROUND - 10}" width="158" height="14" rx="4" fill="#d9c7b4"/>
    <!-- ground -->
    <rect y="{GROUND}" width="{W}" height="{H - GROUND}" fill="#9ed98c"/>
    <path d="M0,{GROUND + 30} L{W},{GROUND + 30} L{W},{H} L0,{H} Z" fill="#f6ead9"/>
    {stones}
    <rect y="{GROUND}" width="{W}" height="34" fill="#8ccf73"/>
    '''

def weeds():
    clumps = ""
    for i, (x, y) in enumerate([(120, 640), (330, 700), (520, 620), (720, 690), (900, 640), (240, 840), (620, 860), (980, 820), (450, 780), (820, 900)]):
        blades = "".join(f'<path d="M{x},{y} q{(j - 3) * 6},{-30 - (j % 3) * 14} {(j - 3) * 14},{-46 - (j % 2) * 20}" stroke="#6b8f3a" stroke-width="6" fill="none" stroke-linecap="round"/>' for j in range(7))
        flower = f'<circle cx="{x + 18}" cy="{y - 60}" r="9" fill="#ffe14d"/>' if i % 3 == 0 else ""
        clumps += f'<ellipse cx="{x}" cy="{y + 4}" rx="44" ry="10" fill="#7a6a4a" opacity="0.4"/>{blades}{flower}'
    return clumps

def tulips():
    colors = ["#ff5f8f", "#ffd34d", "#ff8f3d", "#b77dff", "#ff6f9a"]
    out = f'<rect x="384" y="{GROUND - 16}" width="{W - 384}" height="34" rx="10" fill="#8a5a3c"/>'
    for i, x in enumerate(range(404, W - 10, 34)):
        c = colors[i % 5]; h = 40 + (i % 3) * 12
        out += (f'<path d="M{x},{GROUND} q-4,{-h / 2} 0,{-h}" stroke="#4fae55" stroke-width="5" fill="none"/>'
                f'<ellipse cx="{x - 10}" cy="{GROUND - h / 2}" rx="5" ry="12" fill="#5bb35d" transform="rotate(-30 {x - 10} {GROUND - h / 2})"/>'
                f'<path d="M{x - 12},{GROUND - h} q0,-26 12,-26 q12,0 12,26 q-6,8 -12,8 q-6,0 -12,-8 Z" fill="{c}"/>'
                f'<path d="M{x},{GROUND - h - 24} l-5,14 l5,6 l5,-6 Z" fill="#ffffff" opacity="0.35"/>')
    return f'<g filter="url(#shadow)">{out}</g>'

def umbrella():
    scallops = ""
    for i in range(8):
        x = 390 + i * 42.5
        c = "#ff6f9a" if i % 2 == 0 else "#ffffff"
        scallops += f'<path d="M{x},{372} q21.25,22 42.5,0 Z" fill="{c}"/>'
    stripes = ""
    for i in range(8):
        x0 = 390 + i * 42.5
        c = "#ff6f9a" if i % 2 == 0 else "#ffffff"
        stripes += f'<path d="M560,272 L{x0},372 L{x0 + 42.5},372 Z" fill="{c}"/>'
    return f'''
    <g filter="url(#shadow)">
      <rect x="555" y="300" width="10" height="400" rx="4" fill="#d9d2c8"/>
      {stripes}{scallops}
      <circle cx="560" cy="268" r="10" fill="#ffd34d"/>
    </g>'''

def table():
    def chair(x):
        return f'''
        <rect x="{x - 32}" y="636" width="64" height="70" rx="24" fill="#6fcfb4"/>
        <rect x="{x - 40}" y="704" width="80" height="16" rx="8" fill="#56b39a"/>
        <line x1="{x - 30}" y1="718" x2="{x - 36}" y2="806" stroke="#56b39a" stroke-width="8" stroke-linecap="round"/>
        <line x1="{x + 30}" y1="718" x2="{x + 36}" y2="806" stroke="#56b39a" stroke-width="8" stroke-linecap="round"/>'''
    return f'''
    <g filter="url(#shadow)">
      <ellipse cx="560" cy="812" rx="200" ry="18" fill="#5b3220" opacity="0.18"/>
      {chair(410)}{chair(710)}
      <rect x="553" y="700" width="14" height="104" fill="#d9d2c8"/>
      <ellipse cx="560" cy="806" rx="42" ry="9" fill="#bfb6aa"/>
      <ellipse cx="560" cy="704" rx="120" ry="24" fill="#e9e2d8"/>
      <ellipse cx="560" cy="700" rx="120" ry="24" fill="#ffffff"/>
      {cinnamon_roll(510, 694, 15)}
      <path d="M600,680 l0,18 q11,7 22,0 l0,-18 Z" fill="#ffffff" stroke="#6fcfb4" stroke-width="3"/>
    </g>'''

def bouquet():
    blooms = "".join(f'<circle cx="{560 + dx}" cy="{638 + dy}" r="13" fill="{c}"/><circle cx="{560 + dx}" cy="{638 + dy}" r="5" fill="#ffd34d"/>'
                     for dx, dy, c in [(-20, -6, "#ff6f9a"), (0, -22, "#b77dff"), (20, -4, "#ff8f3d"), (-8, 8, "#ffffff"), (12, 10, "#ff5f8f")])
    return f'''
    <g filter="url(#shadow)">
      <path d="M560,690 l-10,-40 M560,690 l0,-46 M560,690 l12,-40" stroke="#4fae55" stroke-width="4"/>
      <path d="M544,664 Q540,700 552,702 L568,702 Q580,700 576,664 Z" fill="#7aa0ff"/>
      {blooms}
    </g>'''

def planter(x, y):
    flowers = "".join(f'<circle cx="{x + 18 + i * 30}" cy="{y - 18 - (i % 2) * 10}" r="14" fill="{c}"/><circle cx="{x + 18 + i * 30}" cy="{y - 18 - (i % 2) * 10}" r="5" fill="#fff6c8"/>'
                      for i, c in enumerate(["#ff6f9a", "#ffd34d", "#b77dff", "#ff8f3d", "#ff5f8f", "#ffffff"]))
    leaves = "".join(f'<ellipse cx="{x + 10 + i * 28}" cy="{y - 2}" rx="18" ry="10" fill="#5bb35d"/>' for i in range(7))
    return (f'<ellipse cx="{x + 95}" cy="{y + 74}" rx="110" ry="12" fill="#5b3220" opacity="0.18"/>'
            f'{leaves}{flowers}'
            f'<rect x="{x}" y="{y}" width="190" height="70" rx="8" fill="url(#wood)"/>'
            f'<rect x="{x}" y="{y + 22}" width="190" height="6" fill="#7a4628" opacity="0.5"/><rect x="{x}" y="{y + 46}" width="190" height="6" fill="#7a4628" opacity="0.5"/>')

def planters():
    return f'<g filter="url(#shadow)">{planter(860, 800)}{planter(20, 820)}</g>'

def lanterns():
    out = ""
    for (x0, y0, x1, y1, sag) in [(380, 110, 1080, 230, 90)]:
        out += f'<path d="M{x0},{y0} Q{(x0 + x1) / 2},{(y0 + y1) / 2 + sag} {x1},{y1}" stroke="#5b3220" stroke-width="3" fill="none"/>'
        for i in range(1, 12):
            t = i / 12
            x = (1 - t) ** 2 * x0 + 2 * (1 - t) * t * (x0 + x1) / 2 + t * t * x1
            y = (1 - t) ** 2 * y0 + 2 * (1 - t) * t * ((y0 + y1) / 2 + sag) + t * t * y1
            c = ["#ffd34d", "#ff9fbe", "#9fe0ff"][i % 3]
            out += f'<circle cx="{x:.0f}" cy="{y + 18:.0f}" r="30" fill="url(#glow)"/><rect x="{x - 3:.0f}" y="{y:.0f}" width="6" height="8" fill="#5b3220"/><ellipse cx="{x:.0f}" cy="{y + 18:.0f}" rx="10" ry="13" fill="{c}"/>'
    return f'<g>{out}</g>'

def balloons():
    out = ""
    for (x, y, c) in [(960, 270, "#ff6f9a"), (1000, 240, "#ffd34d"), (1034, 286, "#7aa0ff"), (990, 310, "#6fcfb4"), (940, 320, "#b77dff")]:
        out += (f'<path d="M{x},{y + 40} Q{x + 10},{y + 110} 1000,440" stroke="#ffffff" stroke-width="2" fill="none"/>'
                f'<ellipse cx="{x}" cy="{y}" rx="28" ry="36" fill="{c}"/><ellipse cx="{x - 9}" cy="{y - 13}" rx="6" ry="10" fill="#fff" opacity="0.5"/>')
    return f'<g filter="url(#shadow)">{out}</g>'

def sun():
    rays = "".join(f'<line x1="{960 + 70 * math.cos(a):.0f}" y1="{100 + 70 * math.sin(a):.0f}" x2="{960 + 96 * math.cos(a):.0f}" y2="{100 + 96 * math.sin(a):.0f}" stroke="#ffd34d" stroke-width="10" stroke-linecap="round"/>'
                   for a in [i * math.pi / 6 for i in range(12)])
    return f'<circle cx="960" cy="100" r="140" fill="url(#glow)"/>{rays}<circle cx="960" cy="100" r="56" fill="#ffd34d"/><circle cx="944" cy="86" r="18" fill="#fff6c8" opacity="0.6"/>'

def towel():
    stripes = "".join(f'<rect x="{600 + i * 34}" y="830" width="17" height="76" fill="#ffffff" opacity="0.9"/>' for i in range(7))
    return f'<g filter="url(#shadow)"><rect x="600" y="830" width="238" height="76" rx="10" fill="#3fa7f5"/>{stripes}</g>'

def party():
    colors = ["#ff6f9a", "#ffd34d", "#6fcfb4", "#7aa0ff", "#ff9f2e", "#b77dff"]
    flags = ""
    for i in range(12):
        t = (i + 0.5) / 12
        x = 380 + t * 700
        y = 300 + 60 * math.sin(math.pi * t)
        flags += f'<path d="M{x - 24},{y} L{x + 24},{y} L{x},{y + 40} Z" fill="{colors[i % 6]}"/>'
    banner = ('<rect x="560" y="430" width="320" height="56" rx="12" fill="#ff6f9a" stroke="#ffffff" stroke-width="4"/>'
              '<text x="720" y="469" font-family="Fredoka, Arial Rounded MT Bold, Arial" font-weight="700" font-size="30" fill="#ffffff" text-anchor="middle">PATIO PARTY!</text>')
    confetti = "".join(f'<rect x="{(i * 137) % 1060}" y="{120 + (i * 71) % 420}" width="10" height="5" fill="{colors[i % 6]}" transform="rotate({(i * 47) % 180} {(i * 137) % 1060} {120 + (i * 71) % 420})"/>' for i in range(34))
    return f'<g filter="url(#shadow)"><path d="M380,300 Q730,420 1080,300" stroke="#ffffff" stroke-width="3" fill="none"/>{flags}{banner}</g>{confetti}'

def layers():
    return [
        ("base", 0, "always", base()),
        ("sun", 5, "done:c2_sun", sun()),
        ("tulips", 10, "done:c2_tulips", tulips()),
        ("lanterns", 12, "done:c2_candles", lanterns()),
        ("balloons", 14, "done:c2_balloons", balloons()),
        ("party", 16, "done:c2_party", party()),
        ("weeds", 20, "undone:c2_weeds", weeds()),
        ("towel", 22, "done:c2_sun", towel()),
        ("umbrella", 30, "done:c2_umbrella", umbrella()),
        ("table", 32, "done:c2_tables", table()),
        ("bouquet", 34, "done:c2_bouquet", bouquet()),
        *mandy_standing_layers("mandy", 40, "always", 265, 790, 0.7),
        ("surf_sit", 44, "undone:c2_sun", surf_sitting(860, 830, 0.55)),
        ("surf_towel", 45, "done:c2_sun", surf_lying(735, 860, 0.62)),
        ("planters", 50, "done:c2_planters", planters()),
    ]
