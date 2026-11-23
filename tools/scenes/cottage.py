"""Frosty Holidays event scene: Grandma June's cottage on the hill, decorated one milestone at a time."""
import math
from common import *

FLOOR = 610

def base():
    logs = "".join(f'<rect x="0" y="{y}" width="{W}" height="40" rx="18" fill="{"#c98b5e" if i % 2 == 0 else "#b97c50"}"/>'
                   f'<path d="M0,{y + 34} H{W}" stroke="#9a6340" stroke-width="3" opacity="0.4"/>' for i, y in enumerate(range(50, FLOOR, 40)))
    planks = "".join(f'<rect x="0" y="{FLOOR + i * 56 + i * i * 2}" width="{W}" height="{54 + i * 4}" fill="{"#8a5a3c" if i % 2 == 0 else "#7d5034"}"/>' for i in range(6))
    flakes = "".join(f'<circle cx="{700 + (i * 53) % 220}" cy="{150 + (i * 37) % 230}" r="{2 + i % 3}" fill="#ffffff"/>' for i in range(22))
    return f'''
    <rect width="{W}" height="{FLOOR}" fill="#c98b5e"/>
    {logs}
    <rect width="{W}" height="54" fill="#6e3f26"/>
    {planks}
    <rect y="{FLOOR}" width="{W}" height="14" fill="#000" opacity="0.12"/>
    <!-- window with a snowy night -->
    <rect x="680" y="130" width="260" height="270" rx="14" fill="#f2e2d2"/>
    <rect x="696" y="146" width="228" height="238" rx="8" fill="#2f4f8f"/>
    <circle cx="880" cy="190" r="24" fill="#fff6c8"/>
    <path d="M696,330 Q760,300 820,320 Q880,296 924,316 L924,384 L696,384 Z" fill="#eef6ff"/>
    {flakes}
    <rect x="805" y="146" width="10" height="238" fill="#f2e2d2"/><rect x="696" y="262" width="228" height="10" fill="#f2e2d2"/>
    <rect x="696" y="146" width="228" height="238" fill="url(#glass)" opacity="0.5"/>
    <rect x="668" y="398" width="284" height="16" rx="6" fill="#e2cdb8"/>
    <!-- fireplace -->
    <g filter="url(#shadow)">
      <rect x="120" y="250" width="340" height="{FLOOR - 250}" rx="8" fill="#9aa4ae"/>
      {''.join(f'<rect x="{126 + (i % 5) * 66 + (8 if (i // 5) % 2 else 0)}" y="{258 + (i // 5) * 44}" width="60" height="38" rx="10" fill="#b4bec8"/>' for i in range(40) if 258 + (i // 5) * 44 < FLOOR - 40)}
      <path d="M190,{FLOOR} L190,440 Q190,370 290,366 Q390,370 390,440 L390,{FLOOR} Z" fill="#2b1d17"/>
      <rect x="100" y="236" width="380" height="26" rx="6" fill="url(#wood)"/>
      <rect x="230" y="{FLOOR - 26}" width="120" height="14" rx="5" fill="#6e3f26"/>
      <rect x="250" y="{FLOOR - 40}" width="90" height="16" rx="7" fill="#8a5234" transform="rotate(-8 295 {FLOOR - 32})"/>
    </g>
    <!-- cozy rug -->
    <ellipse cx="420" cy="800" rx="300" ry="70" fill="#d94a5f"/>
    <ellipse cx="420" cy="800" rx="250" ry="52" fill="none" stroke="#ffd34d" stroke-width="6" stroke-dasharray="14 10"/>
    <!-- door -->
    <rect x="980" y="230" width="100" height="{FLOOR - 230}" fill="#6e3f26"/>
    <rect x="990" y="240" width="90" height="{FLOOR - 240}" fill="#8a5234"/>
    <circle cx="1002" cy="440" r="7" fill="url(#brass)"/>
    '''

def fire():
    return f'''
    <circle cx="290" cy="530" r="150" fill="url(#glow)"/>
    <path d="M215,{FLOOR - 30} Q230,480 262,530 Q270,440 296,520 Q312,460 330,530 Q350,480 368,{FLOOR - 30} Z" fill="#ff7a2e"/>
    <path d="M240,{FLOOR - 30} Q258,520 276,548 Q288,490 304,546 Q318,512 336,{FLOOR - 30} Z" fill="#ffd34d"/>'''

def stockings():
    out = ""
    for i, (x, c, name) in enumerate([(170, "#e8424f", "MANDY"), (290, "#3fa7f5", "SURF"), (400, "#4fae55", "")]):
        out += (f'<path d="M{x},262 L{x},330 Q{x},356 {x + 22},356 L{x + 44},356 Q{x + 56},356 {x + 56},340 Q{x + 56},328 {x + 40},326 L{x + 34},326 L{x + 34},262 Z" fill="{c}"/>'
                f'<rect x="{x - 4}" y="262" width="42" height="20" rx="6" fill="#ffffff"/>')
        if name: out += f'<text x="{x + 17}" y="306" font-family="Fredoka, Arial" font-weight="700" font-size="10" fill="#ffffff" text-anchor="middle">{name}</text>'
    return f'<g filter="url(#shadow)">{out}</g>'

def tree():
    return '''
    <g filter="url(#shadow)">
      <rect x="560" y="690" width="40" height="60" fill="#8a5234"/>
      <path d="M580,330 L680,520 L480,520 Z" fill="#3f9a55"/>
      <path d="M580,430 L710,630 L450,630 Z" fill="#358a4b"/>
      <path d="M580,540 L740,730 L420,730 Z" fill="#2f7d43"/>
      <rect x="540" y="740" width="80" height="40" rx="8" fill="#c94a4a"/>
    </g>'''

def tree_decor():
    balls = [(560, 470, "#ff6f9a"), (610, 500, "#ffd34d"), (520, 580, "#7aa0ff"), (640, 600, "#ff6f9a"), (580, 620, "#ffd34d"),
             (480, 690, "#ffd34d"), (560, 700, "#7aa0ff"), (680, 700, "#ff9f2e"), (620, 660, "#b77dff"), (530, 640, "#ffffff")]
    out = "".join(f'<circle cx="{x}" cy="{y}" r="12" fill="{c}"/><circle cx="{x - 4}" cy="{y - 4}" r="3" fill="#fff" opacity="0.7"/>' for x, y, c in balls)
    tinsel = '<path d="M510,520 Q580,560 650,520 M470,630 Q580,680 690,630" stroke="#ffd34d" stroke-width="5" fill="none" stroke-dasharray="2 8" stroke-linecap="round"/>'
    star = '<path d="M580,300 L590,326 L618,326 L596,342 L604,368 L580,352 L556,368 L564,342 L542,326 L570,326 Z" fill="#ffd34d" stroke="#fff6c8" stroke-width="3"/>'
    return f'<circle cx="580" cy="334" r="60" fill="url(#glow)"/><g filter="url(#shadow)">{tinsel}{out}{star}</g>'

def garland():
    bulbs = ""
    for i in range(14):
        t = i / 13
        x = 100 + t * 380
        y = 262 + 22 * math.sin(math.pi * t * 2)
        bulbs += f'<circle cx="{x:.0f}" cy="{y + 10:.0f}" r="18" fill="url(#glow)"/><circle cx="{x:.0f}" cy="{y + 10:.0f}" r="6" fill="{["#ff6f9a", "#ffd34d", "#7aa0ff", "#6fcfb4"][i % 4]}"/>'
    ceiling = "".join(f'<circle cx="{x}" cy="{70 + 16 * math.sin(x / 60):.0f}" r="7" fill="{["#ff6f9a", "#ffd34d", "#7aa0ff", "#6fcfb4"][i % 4]}"/><circle cx="{x}" cy="{70 + 16 * math.sin(x / 60):.0f}" r="20" fill="url(#glow)"/>' for i, x in enumerate(range(20, W, 60)))
    return f'''<path d="M100,262 Q190,300 290,262 Q390,300 480,262" stroke="#2f7d43" stroke-width="16" fill="none" stroke-linecap="round"/>
    {bulbs}<path d="M0,64 Q270,96 540,64 Q810,96 1080,64" stroke="#2b1d17" stroke-width="2" fill="none"/>{ceiling}'''

def wreath():
    leaves = "".join(f'<circle cx="{1035 + 34 * math.cos(a):.0f}" cy="{330 + 34 * math.sin(a):.0f}" r="13" fill="{"#2f7d43" if i % 2 else "#3f9a55"}"/>' for i, a in enumerate([k * math.pi / 8 for k in range(16)]))
    berries = "".join(f'<circle cx="{1035 + 34 * math.cos(a):.0f}" cy="{330 + 34 * math.sin(a):.0f}" r="5" fill="#e8424f"/>' for a in [k * math.pi / 3 + 0.3 for k in range(6)])
    return f'<g filter="url(#shadow)">{leaves}{berries}<path d="M1022,364 l13,-10 l13,10 l-6,22 l-7,-12 l-7,12 Z" fill="#e8424f"/></g>'

def cocoa():
    mug = lambda x: (f'<rect x="{x}" y="666" width="34" height="38" rx="8" fill="#ffffff"/><path d="M{x + 34},676 q14,0 14,12 q0,12 -14,12" stroke="#ffffff" stroke-width="5" fill="none"/>'
                     f'<ellipse cx="{x + 17}" cy="668" rx="15" ry="5" fill="#7a4628"/><circle cx="{x + 12}" cy="664" r="5" fill="#fff"/><circle cx="{x + 21}" cy="664" r="5" fill="#fff"/>'
                     f'<path d="M{x + 10},652 q6,-10 0,-20 M{x + 22},652 q6,-10 0,-20" stroke="#ffffff" stroke-width="3" fill="none" opacity="0.6"/>')
    cookies = "".join(f'<circle cx="{x}" cy="700" r="13" fill="#d9a066"/><circle cx="{x - 4}" cy="697" r="2.5" fill="#6b3b22"/><circle cx="{x + 4}" cy="703" r="2.5" fill="#6b3b22"/>' for x in (868, 896, 924))
    return f'''
    <g filter="url(#shadow)">
      <ellipse cx="880" cy="850" rx="120" ry="14" fill="#2b1d17" opacity="0.3"/>
      <rect x="870" y="716" width="20" height="130" fill="#6e3f26"/>
      <ellipse cx="880" cy="846" rx="50" ry="10" fill="#6e3f26"/>
      <ellipse cx="880" cy="714" rx="110" ry="22" fill="#e8424f"/>
      <ellipse cx="880" cy="710" rx="110" ry="22" fill="#ffffff"/>
      {mug(800)}{cookies}{mug(940)}
    </g>'''

def presents():
    boxes = ""
    for (x, y, w, h, c, rib) in [(440, 700, 80, 70, "#ff6f9a", "#ffd34d"), (520, 730, 70, 50, "#7aa0ff", "#ffffff"), (640, 710, 90, 70, "#ffd34d", "#e8424f"), (590, 750, 60, 40, "#6fcfb4", "#ff6f9a")]:
        boxes += (f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="6" fill="{c}"/>'
                  f'<rect x="{x + w / 2 - 6}" y="{y}" width="12" height="{h}" fill="{rib}"/><rect x="{x}" y="{y + h / 2 - 5}" width="{w}" height="10" fill="{rib}"/>'
                  f'<path d="M{x + w / 2},{y} q-20,-22 -24,-4 q8,6 24,4 q16,2 24,-4 q-4,-18 -24,4" fill="{rib}"/>')
    return f'<g filter="url(#shadow)">{boxes}</g>'

def snowman():
    return '''
    <g>
      <circle cx="760" cy="350" r="34" fill="#ffffff"/>
      <circle cx="760" cy="300" r="24" fill="#ffffff"/>
      <circle cx="752" cy="296" r="3" fill="#2b1d17"/><circle cx="768" cy="296" r="3" fill="#2b1d17"/>
      <path d="M760,304 l14,4 l-14,2 Z" fill="#ff9f2e"/>
      <rect x="742" y="268" width="36" height="8" fill="#2b1d17"/><rect x="748" y="246" width="24" height="24" fill="#2b1d17"/>
      <path d="M740,322 q20,10 40,0" stroke="#e8424f" stroke-width="8" fill="none" stroke-linecap="round"/>
      <path d="M728,346 l-26,-18 M792,346 l24,-20" stroke="#8a5234" stroke-width="4" stroke-linecap="round"/>
    </g>'''

def santa_hat(cx, cy, s):
    def S(v): return v * s
    return (f'<path d="M{cx - S(46)},{cy} Q{cx - S(20)},{cy - S(90)} {cx + S(40)},{cy - S(70)} Q{cx + S(60)},{cy - S(40)} {cx + S(46)},{cy} Z" fill="#e8424f"/>'
            f'<rect x="{cx - S(56)}" y="{cy - S(10)}" width="{S(112)}" height="{S(24)}" rx="{S(12)}" fill="#ffffff"/>'
            f'<circle cx="{cx + S(46)}" cy="{cy - S(64)}" r="{S(14)}" fill="#ffffff"/>')

def layers():
    surf_x, surf_y, surf_s = 360, 790, 0.62
    head_x, head_y = surf_x - 95 * surf_s, surf_y - 40 * surf_s
    return [
        ("base", 0, "always", base()),
        ("snowman", 4, "done:frosty_9", snowman()),
        ("fire", 6, "done:frosty_1", fire()),
        ("stockings", 8, "done:frosty_2", stockings()),
        ("garland", 9, "done:frosty_5", garland()),
        ("wreath", 9, "done:frosty_6", wreath()),
        ("tree", 10, "done:frosty_3", tree()),
        ("tree_decor", 11, "done:frosty_4", tree_decor()),
        ("presents", 12, "done:frosty_8", presents()),
        *mandy_standing_layers("mandy", 20, "always", 800, 640, 0.6),
        ("cocoa", 30, "done:frosty_7", cocoa()),
        ("surf_rug", 40, "always", surf_lying(surf_x, surf_y, surf_s)),
        ("surf_hat", 41, "done:frosty_10", f'<g filter="url(#shadow)">{santa_hat(head_x, head_y - 38 * surf_s, surf_s * 0.9)}</g>'),
    ]
