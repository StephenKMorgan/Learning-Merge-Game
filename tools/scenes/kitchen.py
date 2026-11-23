"""Chapter 3 scene: the bakery's back kitchen, readied for the famous food critic."""
import math
from common import *

FLOOR = 600
COUNTER_TOP = 470    # right-hand counter
TABLE_TOP = 640      # work table in the middle

def base():
    tiles = ""
    for r in range(0, 9):
        y = 330 + r * 30
        off = 0 if r % 2 == 0 else 30
        for x in range(-60 + off, W, 60):
            tiles += f'<rect x="{x + 2}" y="{y + 2}" width="56" height="26" rx="4" fill="#ffffff" stroke="#e7ddd2" stroke-width="2"/>'
    floor = ""
    for r in range(6):
        y0 = FLOOR + r * 58 + r * r * 3
        h = 58 + r * 6
        for c in range(-2, 14):
            x = c * 96 - r * 10
            fill = "#e9b38c" if (r + c) % 2 == 0 else "#d98f62"
            floor += f'<rect x="{x}" y="{y0}" width="96" height="{h}" fill="{fill}"/>'
    return f'''
    <rect width="{W}" height="{FLOOR}" fill="#fff4e4"/>
    <rect y="330" width="{W}" height="{FLOOR - 330}" fill="#f4ece2"/>
    {tiles}
    <rect y="322" width="{W}" height="10" fill="#e8b98a"/>
    <rect width="{W}" height="50" fill="url(#wood)"/>
    <rect y="46" width="{W}" height="8" fill="#6e3f26" opacity="0.5"/>
    {floor}
    <rect y="{FLOOR}" width="{W}" height="16" fill="#000" opacity="0.08"/>
    <!-- right counter (always there) -->
    <g filter="url(#shadow)">
      <rect x="770" y="{COUNTER_TOP + 10}" width="320" height="{FLOOR - COUNTER_TOP + 20}" fill="#7fb7d9"/>
      <rect x="790" y="{COUNTER_TOP + 34}" width="120" height="90" rx="8" fill="#9ccbe8"/>
      <rect x="930" y="{COUNTER_TOP + 34}" width="130" height="90" rx="8" fill="#9ccbe8"/>
      <circle cx="896" cy="{COUNTER_TOP + 80}" r="5" fill="url(#brass)"/><circle cx="944" cy="{COUNTER_TOP + 80}" r="5" fill="url(#brass)"/>
      <rect x="756" y="{COUNTER_TOP - 6}" width="340" height="22" rx="6" fill="#fff6ea"/>
    </g>
    <!-- plain window -->
    <rect x="460" y="110" width="190" height="190" rx="14" fill="#ffffff"/>
    <rect x="474" y="124" width="162" height="162" rx="8" fill="url(#sky)"/>
    <circle cx="520" cy="170" r="20" fill="#fff" opacity="0.9"/><circle cx="545" cy="164" r="26" fill="#fff" opacity="0.9"/>
    <path d="M474,250 Q520,220 560,240 Q600,215 636,236 L636,286 L474,286 Z" fill="#9ed98c"/>
    <rect x="550" y="124" width="10" height="162" fill="#ffffff"/><rect x="474" y="200" width="162" height="8" fill="#ffffff"/>
    <rect x="474" y="124" width="162" height="162" fill="url(#glass)"/>
    <rect x="450" y="296" width="210" height="14" rx="5" fill="#f2e2d2"/>
    '''

def junk():
    crates = ""
    for (x, y, w, h) in [(150, 470, 150, 130), (300, 500, 120, 100), (210, 380, 110, 92), (620, 690, 140, 110), (330, 770, 130, 110)]:
        crates += (f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="6" fill="#c9a074"/>'
                   f'<path d="M{x},{y} L{x + w},{y + h} M{x + w},{y} L{x},{y + h}" stroke="#a87d55" stroke-width="6"/>'
                   f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="6" fill="none" stroke="#a87d55" stroke-width="6"/>')
    web = '<g stroke="#ffffff" stroke-width="2" fill="none" opacity="0.85"><path d="M0,50 l120,0 M0,50 l0,120 M0,50 l90,90 M0,50 l110,50 M0,50 l50,110"/><path d="M40,50 q4,30 -40,40"/><path d="M80,50 q-4,60 -80,80"/></g>'
    dust = "".join(f'<ellipse cx="{x}" cy="{y}" rx="{r}" ry="{r * 0.35}" fill="#8a7d70" opacity="0.6"/>' for (x, y, r) in [(480, 860, 60), (760, 640, 40), (120, 880, 50)])
    return f'<rect width="{W}" height="{H}" fill="#6b5b4b" opacity="0.18"/><g filter="url(#shadow)">{crates}</g>{dust}{web}'

def oven():
    bricks = ""
    for r in range(12):
        y = 230 + r * 32
        off = 0 if r % 2 == 0 else 26
        for x in range(110 - off, 440, 52):
            bricks += f'<rect x="{max(x, 110) + 2}" y="{y + 2}" width="{min(48, 438 - max(x, 110))}" height="28" rx="5" fill="#d9775c" opacity="{0.8 + 0.2 * ((x + r) % 3) / 2:.2f}"/>'
    return f'''
    <g filter="url(#shadow)">
      <path d="M100,{FLOOR} L100,300 Q100,190 275,180 Q450,190 450,300 L450,{FLOOR} Z" fill="#b85a43"/>
      <clipPath id="ovenClip"><path d="M108,{FLOOR} L108,302 Q108,198 275,188 Q442,198 442,302 L442,{FLOOR} Z"/></clipPath>
      <g clip-path="url(#ovenClip)">{bricks}</g>
      <path d="M170,470 Q170,360 275,356 Q380,360 380,470 Z" fill="#3a2016"/>
      <circle cx="275" cy="450" r="110" fill="url(#glow)"/>
      <path d="M210,470 Q230,410 250,450 Q262,390 280,440 Q298,400 312,452 Q330,420 340,470 Z" fill="#ff9f2e"/>
      <path d="M236,470 Q252,432 266,458 Q276,420 290,456 Q304,436 314,470 Z" fill="#ffd34d"/>
      <rect x="150" y="470" width="250" height="20" rx="6" fill="#8d4433"/>
      <rect x="236" y="164" width="78" height="40" rx="6" fill="#8d4433"/>
      <rect x="150" y="510" width="250" height="80" rx="8" fill="#a5503c"/>
      <g fill="#7a3b2c">{''.join(f'<ellipse cx="{180 + i * 40}" cy="560" rx="14" ry="7"/>' for i in range(5))}</g>
    </g>'''

def table():
    return f'''
    <g filter="url(#shadow)">
      <ellipse cx="560" cy="826" rx="230" ry="16" fill="#5b3220" opacity="0.2"/>
      <rect x="360" y="{TABLE_TOP + 10}" width="22" height="170" fill="#8a5234"/>
      <rect x="738" y="{TABLE_TOP + 10}" width="22" height="170" fill="#8a5234"/>
      <rect x="370" y="760" width="380" height="16" rx="6" fill="#8a5234"/>
      <rect x="340" y="{TABLE_TOP - 10}" width="440" height="34" rx="8" fill="url(#woodLight)"/>
      <ellipse cx="520" cy="{TABLE_TOP - 12}" rx="90" ry="14" fill="#ffffff" opacity="0.8"/>
      <ellipse cx="520" cy="{TABLE_TOP - 22}" rx="56" ry="20" fill="#fbe7c8"/>
      <rect x="600" y="{TABLE_TOP - 30}" width="120" height="16" rx="8" fill="#e3b27f" transform="rotate(-8 660 {TABLE_TOP - 22})"/>
      <rect x="590" y="{TABLE_TOP - 32}" width="18" height="20" rx="6" fill="#c98b62" transform="rotate(-8 660 {TABLE_TOP - 22})"/>
      <rect x="712" y="{TABLE_TOP - 48}" width="18" height="20" rx="6" fill="#c98b62" transform="rotate(-8 660 {TABLE_TOP - 22})"/>
      <circle cx="420" cy="{TABLE_TOP - 16}" r="5" fill="#fff"/><circle cx="440" cy="{TABLE_TOP - 10}" r="3" fill="#fff"/><circle cx="610" cy="{TABLE_TOP - 6}" r="4" fill="#fff"/>
    </g>'''

def shelves():
    jars = ""
    labels = [("FLOUR", "#ffffff"), ("SUGAR", "#ffe9f1"), ("CINNAMON", "#f3c08a"), ("OATS", "#fff3d6")]
    for row, y in enumerate([160, 300]):
        for i in range(3):
            x = 800 + i * 80
            name, c = labels[(row * 3 + i) % 4]
            jars += (f'<rect x="{x}" y="{y - 90}" width="62" height="90" rx="12" fill="{c}" stroke="#ffffff" stroke-width="3"/>'
                     f'<rect x="{x + 4}" y="{y - 100}" width="54" height="16" rx="6" fill="#c98b62"/>'
                     f'<rect x="{x + 6}" y="{y - 56}" width="50" height="22" rx="4" fill="#fffaf2"/>'
                     f'<text x="{x + 31}" y="{y - 40}" font-family="Fredoka, Arial Rounded MT Bold, Arial" font-weight="700" font-size="{11 if len(name) > 6 else 13}" fill="#7a4a34" text-anchor="middle">{name}</text>')
    return f'''
    <g filter="url(#shadow)">
      <rect x="780" y="164" width="270" height="16" rx="5" fill="url(#wood)"/>
      <rect x="780" y="304" width="270" height="16" rx="5" fill="url(#wood)"/>
      {jars}
    </g>'''

def mixer():
    return f'''
    <g filter="url(#shadow)">
      <ellipse cx="900" cy="{COUNTER_TOP - 4}" rx="60" ry="10" fill="#e8749e"/>
      <path d="M856,{COUNTER_TOP - 6} Q856,{COUNTER_TOP - 54} 900,{COUNTER_TOP - 60} Q944,{COUNTER_TOP - 54} 944,{COUNTER_TOP - 6} Z" fill="#dfe8ef"/>
      <path d="M948,{COUNTER_TOP - 6} L948,{COUNTER_TOP - 120} Q948,{COUNTER_TOP - 150} 920,{COUNTER_TOP - 150} L860,{COUNTER_TOP - 150} Q836,{COUNTER_TOP - 150} 836,{COUNTER_TOP - 126} Q836,{COUNTER_TOP - 106} 862,{COUNTER_TOP - 104} L924,{COUNTER_TOP - 104} L924,{COUNTER_TOP - 6} Z" fill="#ff8fb5"/>
      <rect x="896" y="{COUNTER_TOP - 104}" width="8" height="40" fill="#c9c9c9"/>
      <circle cx="932" cy="{COUNTER_TOP - 128}" r="6" fill="#ffffff"/>
      <path d="M862,{COUNTER_TOP - 146} Q900,{COUNTER_TOP - 160} 930,{COUNTER_TOP - 146}" stroke="#ffffff" stroke-width="5" fill="none" opacity="0.6" stroke-linecap="round"/>
    </g>'''

def rack():
    trays = ""
    for i, y in enumerate([560, 650, 740]):
        rolls = "".join(cinnamon_roll(80 + k * 44, y - 18, 18, iced=(i != 1)) for k in range(3))
        trays += f'<rect x="54" y="{y}" width="150" height="10" rx="3" fill="#b9c3cf"/>{rolls}'
    return f'''
    <g filter="url(#shadow)">
      <ellipse cx="130" cy="872" rx="100" ry="12" fill="#5b3220" opacity="0.2"/>
      <rect x="44" y="500" width="10" height="370" rx="4" fill="#9aa6b3"/>
      <rect x="204" y="500" width="10" height="370" rx="4" fill="#9aa6b3"/>
      {trays}
      <rect x="54" y="830" width="150" height="10" rx="3" fill="#b9c3cf"/>
    </g>'''

def herbs():
    leaves = "".join(f'<ellipse cx="{472 + i * 18}" cy="{288 - (i % 3) * 8}" rx="9" ry="18" fill="{["#5bb35d", "#4fae55", "#78c86a"][i % 3]}"/>' for i in range(10))
    return f'''
    <g filter="url(#shadow)">
      {leaves}
      <rect x="456" y="294" width="198" height="34" rx="8" fill="#ff9fbe"/>
      <rect x="456" y="294" width="198" height="8" rx="4" fill="#ffffff" opacity="0.4"/>
      <!-- curtains -->
      <path d="M452,100 Q480,200 462,300 L446,300 L446,100 Z" fill="#ffd34d"/>
      <path d="M658,100 Q630,200 648,300 L664,300 L664,100 Z" fill="#ffd34d"/>
      <rect x="440" y="94" width="230" height="12" rx="6" fill="#c98b62"/>
    </g>'''

def recipe():
    return '''
    <g filter="url(#shadow)">
      <rect x="672" y="130" width="96" height="150" rx="8" fill="#a87d55"/>
      <rect x="680" y="138" width="80" height="134" rx="4" fill="#3b4a45"/>
      <text x="720" y="162" font-family="Fredoka, Arial Rounded MT Bold, Arial" font-weight="700" font-size="13" fill="#ffffff" text-anchor="middle">SECRET</text>
      <text x="720" y="178" font-family="Fredoka, Arial Rounded MT Bold, Arial" font-weight="700" font-size="11" fill="#ffd34d" text-anchor="middle">RECIPE</text>
      <path d="M690,194 h52 M690,210 h40 M690,226 h56 M690,242 h30" stroke="#ffffff" stroke-width="3" opacity="0.7" stroke-linecap="round"/>
      <circle cx="744" cy="250" r="9" fill="#ff6f9a"/><path d="M738,250 l5,5 l8,-10" stroke="#fff" stroke-width="2.5" fill="none"/>
    </g>'''

def surf_mat():
    return '''
    <g filter="url(#shadow)">
      <ellipse cx="900" cy="872" rx="130" ry="30" fill="#ff8fb5"/>
      <ellipse cx="900" cy="866" rx="112" ry="22" fill="#ffb3cf"/>
      <rect x="980" y="720" width="8" height="130" fill="#8a5234"/>
      <rect x="930" y="700" width="120" height="56" rx="8" fill="#ffffff" stroke="#ff6f9a" stroke-width="4"/>
      <text x="990" y="724" font-family="Fredoka, Arial Rounded MT Bold, Arial" font-weight="700" font-size="14" fill="#d94a86" text-anchor="middle">QUALITY</text>
      <text x="990" y="744" font-family="Fredoka, Arial Rounded MT Bold, Arial" font-weight="700" font-size="14" fill="#d94a86" text-anchor="middle">CONTROL</text>
    </g>'''

def award():
    colors = ["#ff6f9a", "#ffd34d", "#6fcfb4", "#7aa0ff", "#ff9f2e", "#b77dff"]
    confetti = "".join(f'<rect x="{(i * 137) % 1060}" y="{70 + (i * 71) % 480}" width="10" height="5" fill="{colors[i % 6]}" transform="rotate({(i * 47) % 180} {(i * 137) % 1060} {70 + (i * 71) % 480})"/>' for i in range(34))
    return f'''
    <g filter="url(#shadow)">
      <path d="M275,60 L300,60 L318,150 L232,150 L250,60 Z" fill="#ff6f9a"/>
      <circle cx="275" cy="120" r="46" fill="url(#brass)" stroke="#ffffff" stroke-width="5"/>
      <circle cx="275" cy="120" r="34" fill="#ffe08a"/>
      <path d="M262,104 L262,128 Q262,140 275,140 Q288,140 288,128 L288,104 M275,140 L275,150" stroke="#a8701c" stroke-width="5" fill="none" stroke-linecap="round"/>
      <path d="M266,104 q9,-12 18,0" stroke="#a8701c" stroke-width="4" fill="none"/>
    </g>{confetti}'''

def layers():
    return [
        ("base", 0, "always", base()),
        ("herbs", 8, "done:c3_window", herbs()),
        ("recipe", 8, "done:c3_recipe", recipe()),
        ("shelves", 8, "done:c3_shelves", shelves()),
        ("oven", 10, "done:c3_oven", oven()),
        ("award", 12, "done:c3_award", award()),
        ("mixer", 14, "done:c3_mixer", mixer()),
        ("mandy_stand", 20, "undone:c3_table", mandy_standing(600, 780, 0.62)),
        ("mandy", 20, "done:c3_table", mandy_bust(560, 470, 0.72, wave=False)),
        ("table", 30, "done:c3_table", table()),
        ("rack", 32, "done:c3_rack", rack()),
        ("mat", 34, "done:c3_surf", surf_mat()),
        ("surf_sit", 40, "undone:c3_surf", surf_sitting(880, 880, 0.55)),
        ("surf_mat", 41, "done:c3_surf", surf_sitting(880, 868, 0.55)),
        ("junk", 60, "undone:c3_clear", junk()),
    ]
