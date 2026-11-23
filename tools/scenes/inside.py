"""Chapter 1 scene: the bakery interior. Every task's decoration has a fixed, sensible spot."""
import math
from common import *

FLOOR = 560          # where the back wall meets the floor
COUNTER_TOP = 462    # surface things sit on

def base():
    stripes = "".join(f'<rect x="{x}" y="50" width="34" height="{FLOOR - 50}" fill="#ffffff" opacity="0.28"/>' for x in range(0, W, 68))
    vx, vy = 540, 160
    planks = ""
    for i in range(-6, 19):
        bx = i * 90
        # line from vanishing direction through the floor
        t0 = (FLOOR - vy) / (H - vy)
        x0 = vx + (bx - vx) * t0
        planks += f'<line x1="{x0:.1f}" y1="{FLOOR}" x2="{bx}" y2="{H}" stroke="#8d5533" stroke-width="2.5" opacity="0.45"/>'
    seams = "".join(f'<line x1="0" y1="{y}" x2="{W}" y2="{y}" stroke="#8d5533" stroke-width="2" opacity="0.25"/>'
                    for y in [590, 628, 676, 736, 810, 900])
    panels = "".join(f'<rect x="{x}" y="462" width="104" height="72" rx="8" fill="none" stroke="#e39e78" stroke-width="4"/>' for x in range(16, W, 128))
    return f'''
    <rect width="{W}" height="{FLOOR}" fill="#ffd7df"/>
    <rect width="{W}" height="{FLOOR}" fill="url(#wallGrad)"/>
    <defs><linearGradient id="wallGrad" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#ffe6ec"/><stop offset="1" stop-color="#ffcfd8"/></linearGradient>
    <linearGradient id="floorGrad" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#cf9365"/><stop offset="1" stop-color="#a9693f"/></linearGradient></defs>
    {stripes}
    <rect y="440" width="{W}" height="{FLOOR - 440}" fill="#f8cdb0"/>
    {panels}
    <rect y="436" width="{W}" height="14" fill="#eaa985"/>
    <rect y="{FLOOR - 14}" width="{W}" height="14" fill="#c88660"/>
    <rect width="{W}" height="50" fill="url(#wood)"/>
    <rect y="46" width="{W}" height="8" fill="#6e3f26" opacity="0.5"/>
    <rect y="{FLOOR}" width="{W}" height="{H - FLOOR}" fill="url(#floorGrad)"/>
    {planks}{seams}
    <rect y="{FLOOR}" width="{W}" height="18" fill="#000" opacity="0.08"/>
    '''

def counter():
    return f'''
    <g filter="url(#shadow)">
      <ellipse cx="540" cy="728" rx="270" ry="18" fill="#5b3220" opacity="0.25"/>
      <rect x="300" y="{COUNTER_TOP + 10}" width="480" height="250" rx="14" fill="url(#wood)"/>
      <rect x="326" y="{COUNTER_TOP + 40}" width="130" height="190" rx="12" fill="#c88a5f" opacity="0.6"/>
      <rect x="475" y="{COUNTER_TOP + 40}" width="130" height="190" rx="12" fill="#c88a5f" opacity="0.6"/>
      <rect x="624" y="{COUNTER_TOP + 40}" width="130" height="190" rx="12" fill="#c88a5f" opacity="0.6"/>
      <rect x="282" y="{COUNTER_TOP - 6}" width="516" height="26" rx="10" fill="#fff6ea"/>
      <rect x="282" y="{COUNTER_TOP + 14}" width="516" height="6" rx="3" fill="#e9d3bd"/>
    </g>'''

def window_clean():
    return f'''
    <g filter="url(#shadow)">
      <rect x="80" y="140" width="260" height="250" rx="16" fill="#ffffff"/>
      <rect x="96" y="156" width="228" height="218" rx="8" fill="url(#sky)"/>
      <circle cx="140" cy="200" r="22" fill="#fff" opacity="0.9"/><circle cx="168" cy="196" r="28" fill="#fff" opacity="0.9"/><circle cx="196" cy="204" r="18" fill="#fff" opacity="0.9"/>
      <rect x="96" y="320" width="228" height="54" fill="#9ed98c"/>
      <rect x="240" y="250" width="14" height="80" fill="#8a5a3c"/><circle cx="247" cy="240" r="40" fill="#6cc46a"/><circle cx="226" cy="258" r="26" fill="#5bb35d"/>
      <rect x="120" y="270" width="60" height="60" fill="#ffb3a7"/><path d="M114,272 L150,240 L186,272 Z" fill="#e8705f"/><rect x="142" y="300" width="16" height="30" fill="#8a5a3c"/>
      <rect x="205" y="156" width="10" height="218" fill="#ffffff"/>
      <rect x="96" y="260" width="228" height="10" fill="#ffffff"/>
      <rect x="96" y="156" width="228" height="218" fill="url(#glass)"/>
      <rect x="66" y="388" width="288" height="18" rx="6" fill="#f2e2d2"/>
      <!-- curtains -->
      <path d="M70,130 Q100,250 84,392 L64,392 L64,130 Z" fill="#ff9fbe"/>
      <path d="M350,130 Q320,250 336,392 L356,392 L356,130 Z" fill="#ff9fbe"/>
      <rect x="56" y="122" width="308" height="14" rx="7" fill="#c98b62"/>
      <!-- flower box -->
      <rect x="96" y="404" width="228" height="30" rx="6" fill="#8fd16f"/>
      <circle cx="120" cy="402" r="11" fill="#ff6f9a"/><circle cx="160" cy="398" r="11" fill="#ffd34d"/><circle cx="200" cy="402" r="11" fill="#ff6f9a"/><circle cx="240" cy="398" r="11" fill="#b77dff"/><circle cx="282" cy="402" r="11" fill="#ffd34d"/>
    </g>'''

def window_dirty():
    return f'''
    <g>
      <rect x="80" y="140" width="260" height="250" rx="16" fill="#cbbfb4"/>
      <rect x="96" y="156" width="228" height="218" rx="8" fill="#a99c8c"/>
      <rect x="205" y="156" width="10" height="218" fill="#cbbfb4"/>
      <rect x="96" y="260" width="228" height="10" fill="#cbbfb4"/>
      <ellipse cx="150" cy="210" rx="50" ry="30" fill="#8d7d6c" opacity="0.6"/><ellipse cx="270" cy="320" rx="45" ry="35" fill="#8d7d6c" opacity="0.5"/>
      <path d="M230,170 L250,215 L238,240 L262,300" stroke="#5e5144" stroke-width="3" fill="none"/>
      <rect x="66" y="388" width="288" height="18" rx="6" fill="#bcae9f"/>
      <path d="M96,156 L150,156 Q120,170 96,200 Z" fill="none" stroke="#ffffff" stroke-width="1.5" opacity="0.8"/>
      <path d="M96,156 Q130,175 150,156 M96,156 Q112,185 96,200 M104,164 Q122,172 132,160" stroke="#ffffff" stroke-width="1.2" fill="none" opacity="0.8"/>
    </g>'''

def door_new():
    return f'''
    <g filter="url(#shadow)">
      <path d="M838,{FLOOR} L838,250 Q838,192 935,192 Q1032,192 1032,250 L1032,{FLOOR} Z" fill="#ffffff"/>
      <path d="M852,{FLOOR} L852,256 Q852,206 935,206 Q1018,206 1018,256 L1018,{FLOOR} Z" fill="#6fcfb4"/>
      <circle cx="935" cy="282" r="44" fill="url(#sky)" stroke="#ffffff" stroke-width="8"/>
      <circle cx="935" cy="282" r="44" fill="url(#glass)"/>
      <rect x="870" y="360" width="130" height="80" rx="10" fill="none" stroke="#56b39a" stroke-width="5"/>
      <rect x="870" y="456" width="130" height="80" rx="10" fill="none" stroke="#56b39a" stroke-width="5"/>
      <circle cx="1000" cy="420" r="9" fill="url(#brass)"/>
      <path d="M902,330 L935,312 L968,330" stroke="#a8701c" stroke-width="2" fill="none"/>
      <rect x="896" y="330" width="78" height="34" rx="8" fill="#ff6f9a" stroke="#ffffff" stroke-width="3"/>
      <text x="935" y="354" font-family="Fredoka, Arial Rounded MT Bold, Arial" font-weight="700" font-size="20" fill="#ffffff" text-anchor="middle">OPEN</text>
    </g>'''

def door_old():
    return f'''
    <g>
      <path d="M838,{FLOOR} L838,250 Q838,192 935,192 Q1032,192 1032,250 L1032,{FLOOR} Z" fill="#9b8b7c"/>
      <path d="M852,{FLOOR} L852,256 Q852,206 935,206 Q1018,206 1018,256 L1018,{FLOOR} Z" fill="#7a5a43"/>
      <rect x="836" y="300" width="200" height="30" rx="4" fill="#a87d5a" transform="rotate(-18 935 315)"/>
      <rect x="836" y="420" width="200" height="30" rx="4" fill="#a87d5a" transform="rotate(14 935 435)"/>
      <circle cx="860" cy="330" r="4" fill="#555"/><circle cx="1010" cy="280" r="4" fill="#555"/>
      <rect x="890" y="350" width="90" height="34" rx="6" fill="#d9cfc4" transform="rotate(-8 935 367)"/>
      <text x="935" y="374" font-family="Arial" font-weight="700" font-size="18" fill="#7a5a43" text-anchor="middle" transform="rotate(-8 935 367)">CLOSED</text>
    </g>'''

def picture():
    return '''
    <g filter="url(#shadow)">
      <rect x="392" y="150" width="150" height="112" rx="8" fill="url(#brass)"/>
      <rect x="404" y="162" width="126" height="88" fill="#bfe7ff"/>
      <circle cx="506" cy="182" r="11" fill="#ffd34d"/>
      <path d="M404,228 Q438,190 471,218 Q500,195 530,220 L530,250 L404,250 Z" fill="#7fcf7a"/>
      <path d="M404,238 Q450,218 530,240 L530,250 L404,250 Z" fill="#5bb35d"/>
      <circle cx="440" cy="228" r="4" fill="#ff6f9a"/><circle cx="488" cy="236" r="4" fill="#fff"/>
    </g>'''

def clock():
    hands = '<line x1="790" y1="180" x2="790" y2="152" stroke="#5b3220" stroke-width="5" stroke-linecap="round"/><line x1="790" y1="180" x2="810" y2="190" stroke="#5b3220" stroke-width="4" stroke-linecap="round"/>'
    ticks = "".join(f'<circle cx="{790 + 32 * math.cos(a):.1f}" cy="{180 + 32 * math.sin(a):.1f}" r="3" fill="#c98b62"/>' for a in [i * math.pi / 6 for i in range(12)])
    return f'''
    <g filter="url(#shadow)">
      <circle cx="790" cy="180" r="46" fill="url(#wood)"/>
      <circle cx="790" cy="180" r="38" fill="#fffaf2"/>
      {ticks}{hands}
      <circle cx="790" cy="180" r="5" fill="#ff6f9a"/>
    </g>'''

def broom():
    return '''
    <g filter="url(#shadow)">
      <line x1="62" y1="380" x2="34" y2="530" stroke="#b9805a" stroke-width="10" stroke-linecap="round"/>
      <path d="M18,520 L52,526 L64,572 L4,566 Z" fill="#f2c94c"/>
      <path d="M14,540 L58,546" stroke="#d9a62c" stroke-width="3"/>
      <rect x="20" y="514" width="34" height="10" rx="3" fill="#ff6f9a" transform="rotate(8 37 519)"/>
    </g>'''

def lights():
    out = ""
    for x in (230, 640, 965):
        out += f'''
        <circle cx="{x}" cy="176" r="120" fill="url(#glow)"/>
        <line x1="{x}" y1="50" x2="{x}" y2="140" stroke="#5b3220" stroke-width="3"/>
        <path d="M{x - 34},{172} Q{x - 30},{140} {x},{138} Q{x + 30},{140} {x + 34},{172} Z" fill="url(#brass)"/>
        <ellipse cx="{x}" cy="173" rx="34" ry="7" fill="#c98a2c"/>
        <circle cx="{x}" cy="178" r="11" fill="#fff6c8"/>'''
    return f'<g>{out}</g>'

def mandy():
    return mandy_bust(625, 352, 0.74, wave=False)

def display_case():
    rolls = "".join(cinnamon_roll(x, y, 26) for (x, y) in [(410, 438), (470, 438), (530, 438), (440, 402), (500, 402)])
    return f'''
    <g filter="url(#shadow)">
      <rect x="370" y="{COUNTER_TOP - 18}" width="200" height="18" rx="6" fill="url(#wood)"/>
      {rolls}
      <rect x="374" y="364" width="192" height="84" rx="10" fill="url(#glass)" stroke="#ffffff" stroke-width="4"/>
      <line x1="388" y1="372" x2="410" y2="440" stroke="#ffffff" stroke-width="5" opacity="0.6"/>
      <rect x="364" y="356" width="212" height="14" rx="7" fill="url(#wood)"/>
    </g>'''

def bell():
    return f'''
    <g filter="url(#shadow)">
      <ellipse cx="748" cy="{COUNTER_TOP - 4}" rx="40" ry="9" fill="#a8701c"/>
      <path d="M714,{COUNTER_TOP - 8} Q714,{COUNTER_TOP - 52} 748,{COUNTER_TOP - 54} Q782,{COUNTER_TOP - 52} 782,{COUNTER_TOP - 8} Z" fill="url(#brass)"/>
      <rect x="744" y="{COUNTER_TOP - 66}" width="8" height="14" rx="3" fill="#a8701c"/>
      <ellipse cx="748" cy="{COUNTER_TOP - 68}" rx="9" ry="5" fill="url(#brass)"/>
      <path d="M724,{COUNTER_TOP - 22} Q680,{COUNTER_TOP - 44} 742,{COUNTER_TOP - 46}" stroke="#fff6c8" stroke-width="5" fill="none" stroke-linecap="round" opacity="0.8"/>
    </g>'''

def dog_bed():
    return '''
    <g filter="url(#shadow)">
      <ellipse cx="560" cy="842" rx="170" ry="54" fill="#5a7fd6"/>
      <ellipse cx="560" cy="826" rx="170" ry="52" fill="#7aa0ff"/>
      <ellipse cx="560" cy="830" rx="132" ry="34" fill="#e9eefc"/>
      <path d="M440,830 L680,830 M460,812 L660,848 M460,848 L660,812" stroke="#c8d3f2" stroke-width="3"/>
      <text x="560" y="884" font-family="Fredoka, Arial Rounded MT Bold, Arial" font-weight="700" font-size="22" fill="#ffffff" text-anchor="middle">SURF</text>
    </g>'''

def surf_bed():
    return surf_lying(585, 800, 0.72)

def surf_sit():
    return surf_sitting(860, 900, 0.62)

def table_chairs():
    def chair(x, flip):
        d = -1 if flip else 1
        return f'''
        <path d="M{x},{690} q{d * 10},-60 {d * 4},-110" stroke="#8a5234" stroke-width="10" fill="none" stroke-linecap="round"/>
        <path d="M{x + d * 4},{580} q{d * 40},-14 {d * 56},10" stroke="#8a5234" stroke-width="10" fill="none" stroke-linecap="round"/>
        <rect x="{min(x, x + d * 70)}" y="682" width="70" height="16" rx="8" fill="#b07148"/>
        <line x1="{x + d * 8}" y1="696" x2="{x + d * 4}" y2="800" stroke="#8a5234" stroke-width="8" stroke-linecap="round"/>
        <line x1="{x + d * 62}" y1="696" x2="{x + d * 66}" y2="800" stroke="#8a5234" stroke-width="8" stroke-linecap="round"/>'''
    return f'''
    <g filter="url(#shadow)">
      <ellipse cx="230" cy="804" rx="150" ry="16" fill="#5b3220" opacity="0.2"/>
      {chair(70, False)}{chair(390, True)}
      <rect x="222" y="650" width="16" height="140" fill="#8a5234"/>
      <ellipse cx="230" cy="792" rx="46" ry="10" fill="#8a5234"/>
      <ellipse cx="230" cy="648" rx="110" ry="24" fill="#fff6ea"/>
      <ellipse cx="230" cy="644" rx="110" ry="24" fill="#ffffff"/>
      <path d="M140,650 Q230,690 320,650" stroke="#ff9fbe" stroke-width="6" fill="none"/>
      <ellipse cx="200" cy="636" rx="22" ry="7" fill="#e9eefc"/>
      <path d="M250,618 l0,22 q12,8 24,0 l0,-22 Z" fill="#ffffff" stroke="#ff9fbe" stroke-width="3"/>
      {cinnamon_roll(200, 628, 14)}
    </g>'''

def nook():
    books = "".join(f'<rect x="{872 + i * 18}" y="{604 - (i % 3) * 6}" width="15" height="{50 + (i % 3) * 6}" rx="3" fill="{c}"/>'
                    for i, c in enumerate(["#ff6f9a", "#6fcfb4", "#ffd34d", "#7aa0ff", "#ff9f2e", "#b77dff"]))
    return f'''
    <g filter="url(#shadow)">
      <!-- bookshelf against the wall -->
      <rect x="860" y="590" width="130" height="150" rx="8" fill="url(#wood)"/>
      <rect x="868" y="598" width="114" height="64" fill="#7a4628"/>
      {books}
      <rect x="868" y="668" width="114" height="64" fill="#7a4628"/>
      <circle cx="900" cy="706" r="18" fill="#6cc46a"/><rect x="890" y="712" width="20" height="18" rx="4" fill="#ff9f2e"/>
      <!-- floor lamp -->
      <circle cx="1040" cy="560" r="90" fill="url(#glow)"/>
      <line x1="1040" y1="580" x2="1040" y2="900" stroke="#5b3220" stroke-width="6"/>
      <ellipse cx="1040" cy="902" rx="30" ry="8" fill="#5b3220"/>
      <path d="M1006,590 L1018,540 L1062,540 L1074,590 Z" fill="#ffe08a"/>
      <!-- armchair -->
      <ellipse cx="880" cy="904" rx="150" ry="16" fill="#5b3220" opacity="0.2"/>
      <rect x="770" y="700" width="220" height="150" rx="40" fill="#4f7fe0"/>
      <rect x="790" y="770" width="180" height="90" rx="24" fill="#6f9bff"/>
      <rect x="752" y="790" width="56" height="120" rx="26" fill="#4f7fe0"/>
      <rect x="952" y="790" width="56" height="120" rx="26" fill="#4f7fe0"/>
      <rect x="790" y="872" width="14" height="30" rx="5" fill="#5b3220"/><rect x="956" y="872" width="14" height="30" rx="5" fill="#5b3220"/>
      <rect x="840" y="740" width="70" height="44" rx="14" fill="#ffd34d" transform="rotate(-8 875 762)"/>
    </g>'''

def party():
    colors = ["#ff6f9a", "#ffd34d", "#6fcfb4", "#7aa0ff", "#ff9f2e", "#b77dff"]
    flags = ""
    for i in range(16):
        x = 20 + i * 66
        sag = 34 * math.sin(math.pi * ((x % 540) / 540))
        y = 58 + sag
        flags += f'<path d="M{x},{y} L{x + 50},{y + 2} L{x + 25},{y + 46} Z" fill="{colors[i % 6]}"/>'
    rope = "M0,56 Q270,128 540,56 Q810,128 1080,56"
    balloons = ""
    for (x, y, c) in [(790, 300, "#ff6f9a"), (830, 270, "#ffd34d"), (812, 340, "#7aa0ff")]:
        balloons += (f'<path d="M{x},{y + 42} q-10,60 4,130" stroke="#ffffff" stroke-width="2" fill="none"/>'
                     f'<ellipse cx="{x}" cy="{y}" rx="30" ry="38" fill="{c}"/><ellipse cx="{x - 10}" cy="{y - 14}" rx="7" ry="11" fill="#fff" opacity="0.5"/>'
                     f'<path d="M{x - 6},{y + 38} l6,8 l6,-8 Z" fill="{c}"/>')
    confetti = "".join(f'<rect x="{(i * 137) % 1060}" y="{100 + (i * 71) % 300}" width="10" height="5" fill="{colors[i % 6]}" transform="rotate({(i * 47) % 180} {(i * 137) % 1060} {100 + (i * 71) % 300})"/>' for i in range(30))
    return f'<g filter="url(#shadow)"><path d="{rope}" stroke="#ffffff" stroke-width="4" fill="none"/>{flags}</g><g filter="url(#shadow)">{balloons}</g>{confetti}'

def dust():
    piles = "".join(f'<ellipse cx="{x}" cy="{y}" rx="{r}" ry="{r * 0.35}" fill="#8a7d70" opacity="0.75"/><ellipse cx="{x - r * 0.2}" cy="{y - r * 0.12}" rx="{r * 0.6}" ry="{r * 0.22}" fill="#a39686" opacity="0.8"/>'
                    for (x, y, r) in [(150, 790, 60), (420, 880, 46), (700, 820, 70), (960, 900, 54), (90, 900, 40)])
    web = lambda x, y, sx: (f'<g stroke="#ffffff" stroke-width="2" fill="none" opacity="0.85">'
                            f'<path d="M{x},{y} l{sx * 120},0 M{x},{y} l0,120 M{x},{y} l{sx * 90},90 M{x},{y} l{sx * 110},50 M{x},{y} l{sx * 50},110"/>'
                            f'<path d="M{x + sx * 40},{y} q{sx * 4},{30} {-sx * 40 + sx * 0},{40}" /><path d="M{x + sx * 80},{y} q{-sx * 4},{60} {-sx * 80},{80}"/></g>')
    specks = "".join(f'<circle cx="{(i * 97) % 1080}" cy="{60 + (i * 53) % 820}" r="{2 + i % 3}" fill="#fff" opacity="0.5"/>' for i in range(40))
    papers = '<path d="M300,700 l30,-10 l14,24 l-26,14 Z" fill="#efe6d8"/><path d="M820,790 l26,4 l-6,26 l-24,-6 Z" fill="#e6dccb"/>'
    return f'<rect width="{W}" height="{H}" fill="#6b5b4b" opacity="0.22"/>{piles}{papers}{web(0, 50, 1)}{web(W, 50, -1)}{specks}'

def layers():
    """(id, z, show, svg body). show: "always", "done:<task>" or "undone:<task>"."""
    return [
        ("base", 0, "always", base()),
        ("window_dirty", 10, "undone:c1_windows", window_dirty()),
        ("window", 10, "done:c1_windows", window_clean()),
        ("door_old", 10, "undone:c1_door", door_old()),
        ("door", 10, "done:c1_door", door_new()),
        ("picture", 10, "done:c1_picture", picture()),
        ("clock", 10, "done:c1_clock", clock()),
        ("broom", 12, "done:c1_sweep", broom()),
        ("lights", 15, "done:c1_lights", lights()),
        *mandy_layers("mandy", 20, "always", 625, 352, 0.74, wave=False),
        ("counter", 30, "always", counter()),
        ("display", 35, "done:c1_display", display_case()),
        ("bell", 36, "done:c1_bell", bell()),
        ("nook", 38, "done:c1_nook", nook()),
        ("bed", 40, "done:c1_bed", dog_bed()),
        ("table", 42, "done:c1_chairs", table_chairs()),
        ("surf_sit", 44, "undone:c1_bed", surf_sit()),
        ("surf_bed", 45, "done:c1_bed", surf_bed()),
        ("party", 60, "done:c1_party", party()),
        ("dust", 70, "undone:c1_sweep", dust()),
    ]
