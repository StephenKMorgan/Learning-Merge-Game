"""Shared SVG pieces for the scene art: gradients, shadows, and the two stars, Mandy and Surf."""

W, H = 1080, 940

DEFS = """
<defs>
  <filter id="shadow" x="-30%" y="-30%" width="160%" height="160%">
    <feDropShadow dx="0" dy="6" stdDeviation="6" flood-color="#5b3220" flood-opacity="0.28"/>
  </filter>
  <filter id="soft" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="10"/></filter>
  <filter id="blur4"><feGaussianBlur stdDeviation="4"/></filter>
  <radialGradient id="glow"><stop offset="0" stop-color="#fff3b0" stop-opacity="0.85"/><stop offset="1" stop-color="#ffd27a" stop-opacity="0"/></radialGradient>
  <linearGradient id="fur" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#f0b562"/><stop offset="1" stop-color="#d48a35"/></linearGradient>
  <linearGradient id="furDark" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#d9913f"/><stop offset="1" stop-color="#b8712a"/></linearGradient>
  <linearGradient id="furLight" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#fbe0ad"/><stop offset="1" stop-color="#f0c27f"/></linearGradient>
  <linearGradient id="wood" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#b07148"/><stop offset="1" stop-color="#8a5234"/></linearGradient>
  <linearGradient id="woodLight" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#d9a074"/><stop offset="1" stop-color="#b9805a"/></linearGradient>
  <linearGradient id="brass" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#ffe08a"/><stop offset="0.5" stop-color="#e3a93a"/><stop offset="1" stop-color="#a8701c"/></linearGradient>
  <linearGradient id="glass" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#ffffff" stop-opacity="0.55"/><stop offset="0.5" stop-color="#d8f1ff" stop-opacity="0.18"/><stop offset="1" stop-color="#ffffff" stop-opacity="0.4"/></linearGradient>
  <linearGradient id="skin" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#ffd9bd"/><stop offset="1" stop-color="#f3bb96"/></linearGradient>
  <linearGradient id="roll" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#e9a565"/><stop offset="1" stop-color="#b86e34"/></linearGradient>
  <linearGradient id="sky" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#8fd3ff"/><stop offset="1" stop-color="#e6f7ff"/></linearGradient>
</defs>
"""

def svg(body, w=W, h=H):
    return f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {w} {h}" width="{w}" height="{h}">{DEFS}{body}</svg>'

# ---------------------------------------------------------------- small props

def cinnamon_roll(x, y, r, iced=True):
    """Top-down-ish roll with a spiral, used in display cases."""
    sp = []
    import math
    pts = []
    for i in range(80):
        t = i / 79; a = t * 3 * 2 * math.pi; rr = r * 0.85 * t
        pts.append(f"{x + rr * math.cos(a):.1f},{y + rr * math.sin(a) * 0.8:.1f}")
    ice = ""
    if iced:
        ice = (f'<path d="M{x - r*0.7},{y - r*0.2} q{r*0.35},{-r*0.3} {r*0.7},0 t{r*0.7},0" stroke="#fffaf2" stroke-width="{r*0.16}" fill="none" stroke-linecap="round"/>'
               f'<path d="M{x - r*0.6},{y + r*0.25} q{r*0.3},{-r*0.25} {r*0.6},0 t{r*0.6},0" stroke="#fffaf2" stroke-width="{r*0.14}" fill="none" stroke-linecap="round"/>')
    return (f'<ellipse cx="{x}" cy="{y + r*0.22}" rx="{r}" ry="{r*0.8}" fill="#9a5826"/>'
            f'<ellipse cx="{x}" cy="{y}" rx="{r}" ry="{r*0.8}" fill="url(#roll)"/>'
            f'<polyline points="{" ".join(pts)}" fill="none" stroke="#7a3d14" stroke-width="{r*0.12}" stroke-linecap="round"/>'
            + ice)

# ---------------------------------------------------------------- Mandy

def mandy_bust(cx, cy, s=1.0, wave=True):
    """Mandy from the waist up, centred on her face at (cx, cy). s = scale (face radius 58*s)."""
    def S(v): return v * s
    arm = ""
    if wave:
        arm = (f'<path d="M{cx + S(70)},{cy + S(120)} q{S(60)},{-S(30)} {S(70)},{-S(110)}" stroke="#ffffff" stroke-width="{S(34)}" fill="none" stroke-linecap="round"/>'
               f'<circle cx="{cx + S(142)}" cy="{cy + S(2)}" r="{S(20)}" fill="url(#skin)"/>')
    return f'''
    <g filter="url(#shadow)">
      <!-- body: chef jacket -->
      <path d="M{cx - S(95)},{cy + S(240)} Q{cx - S(100)},{cy + S(95)} {cx},{cy + S(85)} Q{cx + S(100)},{cy + S(95)} {cx + S(95)},{cy + S(240)} Z" fill="#ffffff"/>
      <path d="M{cx - S(60)},{cy + S(240)} L{cx - S(50)},{cy + S(140)} L{cx + S(50)},{cy + S(140)} L{cx + S(60)},{cy + S(240)} Z" fill="#ff8fb5"/>
      <path d="M{cx - S(50)},{cy + S(140)} L{cx + S(50)},{cy + S(140)}" stroke="#e86a98" stroke-width="{S(6)}"/>
      <circle cx="{cx - S(22)}" cy="{cy + S(118)}" r="{S(5)}" fill="#e8d6c8"/><circle cx="{cx + S(22)}" cy="{cy + S(118)}" r="{S(5)}" fill="#e8d6c8"/>
      <!-- neckerchief -->
      <path d="M{cx - S(30)},{cy + S(78)} L{cx},{cy + S(112)} L{cx + S(30)},{cy + S(78)} Z" fill="#ff5f8f"/>
      {arm}
      <!-- hair behind -->
      <ellipse cx="{cx}" cy="{cy + S(10)}" rx="{S(68)}" ry="{S(66)}" fill="#7a4630"/>
      <!-- face -->
      <circle cx="{cx}" cy="{cy}" r="{S(58)}" fill="url(#skin)"/>
      <path d="M{cx - S(58)},{cy - S(8)} Q{cx - S(40)},{cy - S(52)} {cx + S(4)},{cy - S(46)} Q{cx + S(46)},{cy - S(50)} {cx + S(60)},{cy - S(6)} Q{cx + S(30)},{cy - S(34)} {cx - S(2)},{cy - S(26)} Q{cx - S(34)},{cy - S(30)} {cx - S(58)},{cy - S(8)} Z" fill="#8a5038"/>
      <ellipse cx="{cx - S(22)}" cy="{cy + S(4)}" rx="{S(6)}" ry="{S(8)}" fill="#3b2216"/>
      <ellipse cx="{cx + S(22)}" cy="{cy + S(4)}" rx="{S(6)}" ry="{S(8)}" fill="#3b2216"/>
      <circle cx="{cx - S(20)}" cy="{cy + S(1)}" r="{S(2)}" fill="#fff"/><circle cx="{cx + S(24)}" cy="{cy + S(1)}" r="{S(2)}" fill="#fff"/>
      <ellipse cx="{cx - S(36)}" cy="{cy + S(22)}" rx="{S(10)}" ry="{S(6)}" fill="#ff9aa8" opacity="0.6"/>
      <ellipse cx="{cx + S(36)}" cy="{cy + S(22)}" rx="{S(10)}" ry="{S(6)}" fill="#ff9aa8" opacity="0.6"/>
      <path d="M{cx - S(14)},{cy + S(26)} Q{cx},{cy + S(40)} {cx + S(14)},{cy + S(26)}" stroke="#b5533c" stroke-width="{S(4)}" fill="none" stroke-linecap="round"/>
      <!-- chef hat -->
      <rect x="{cx - S(52)}" y="{cy - S(78)}" width="{S(104)}" height="{S(30)}" rx="{S(8)}" fill="#ffffff" stroke="#efe6dc" stroke-width="{S(2)}"/>
      <circle cx="{cx - S(36)}" cy="{cy - S(98)}" r="{S(30)}" fill="#ffffff"/>
      <circle cx="{cx + S(36)}" cy="{cy - S(98)}" r="{S(30)}" fill="#ffffff"/>
      <circle cx="{cx}" cy="{cy - S(116)}" r="{S(38)}" fill="#ffffff"/>
      <path d="M{cx - S(30)},{cy - S(100)} q{S(10)},{-S(14)} {S(24)},{-S(10)}" stroke="#efe6dc" stroke-width="{S(3)}" fill="none"/>
    </g>'''

def mandy_standing(cx, feet, s=1.0):
    def S(v): return v * s
    top = feet - S(420)
    return (f'<ellipse cx="{cx}" cy="{feet}" rx="{S(70)}" ry="{S(14)}" fill="#5b3220" opacity="0.18"/>'
            f'<rect x="{cx - S(40)}" y="{feet - S(110)}" width="{S(28)}" height="{S(105)}" rx="{S(12)}" fill="#6a4a3a"/>'
            f'<rect x="{cx + S(12)}" y="{feet - S(110)}" width="{S(28)}" height="{S(105)}" rx="{S(12)}" fill="#6a4a3a"/>'
            f'<ellipse cx="{cx - S(28)}" cy="{feet - S(4)}" rx="{S(22)}" ry="{S(10)}" fill="#4a2c20"/>'
            f'<ellipse cx="{cx + S(28)}" cy="{feet - S(4)}" rx="{S(22)}" ry="{S(10)}" fill="#4a2c20"/>'
            + mandy_bust(cx, top + S(120), s))

# ---------------------------------------------------------------- Surf the golden retriever

def surf_head(cx, cy, s=1.0, tongue=True, look=0):
    def S(v): return v * s
    t = ""
    if tongue:
        t = f'<path d="M{cx - S(14)},{cy + S(52)} q0,{S(34)} {S(14)},{S(34)} q{S(14)},0 {S(14)},{-S(34)} Z" fill="#ff7a93"/><path d="M{cx},{cy + S(56)} v{S(22)}" stroke="#e85a78" stroke-width="{S(3)}"/>'
    return f'''
      <!-- ears -->
      <path d="M{cx - S(52)},{cy - S(40)} C{cx - S(100)},{cy - S(30)} {cx - S(98)},{cy + S(50)} {cx - S(70)},{cy + S(62)} C{cx - S(52)},{cy + S(66)} {cx - S(40)},{cy + S(20)} {cx - S(36)},{cy - S(20)} Z" fill="url(#furDark)"/>
      <path d="M{cx + S(52)},{cy - S(40)} C{cx + S(100)},{cy - S(30)} {cx + S(98)},{cy + S(50)} {cx + S(70)},{cy + S(62)} C{cx + S(52)},{cy + S(66)} {cx + S(40)},{cy + S(20)} {cx + S(36)},{cy - S(20)} Z" fill="url(#furDark)"/>
      <!-- head -->
      <ellipse cx="{cx}" cy="{cy}" rx="{S(60)}" ry="{S(56)}" fill="url(#fur)"/>
      <ellipse cx="{cx}" cy="{cy + S(34)}" rx="{S(38)}" ry="{S(28)}" fill="url(#furLight)"/>
      <!-- brows/fur tuft -->
      <path d="M{cx - S(16)},{cy - S(52)} q{S(16)},{-S(14)} {S(32)},0" stroke="#f6c47c" stroke-width="{S(6)}" fill="none" stroke-linecap="round"/>
      <!-- eyes -->
      <ellipse cx="{cx - S(24) + S(look)}" cy="{cy - S(6)}" rx="{S(8)}" ry="{S(9)}" fill="#2b1a10"/>
      <ellipse cx="{cx + S(24) + S(look)}" cy="{cy - S(6)}" rx="{S(8)}" ry="{S(9)}" fill="#2b1a10"/>
      <circle cx="{cx - S(21) + S(look)}" cy="{cy - S(9)}" r="{S(3)}" fill="#fff"/><circle cx="{cx + S(27) + S(look)}" cy="{cy - S(9)}" r="{S(3)}" fill="#fff"/>
      <!-- nose & mouth -->
      <ellipse cx="{cx}" cy="{cy + S(24)}" rx="{S(13)}" ry="{S(9)}" fill="#2b1a10"/>
      <ellipse cx="{cx - S(4)}" cy="{cy + S(21)}" rx="{S(4)}" ry="{S(2.5)}" fill="#6b5446"/>
      {t}
      <path d="M{cx},{cy + S(33)} v{S(10)} M{cx - S(20)},{cy + S(46)} q{S(20)},{S(14)} {S(40)},0" stroke="#7a4a26" stroke-width="{S(4)}" fill="none" stroke-linecap="round"/>
    '''

def surf_sitting(cx, feet, s=1.0):
    """Surf sitting, facing us, paws at y=feet."""
    def S(v): return v * s
    return f'''
    <g filter="url(#shadow)">
      <ellipse cx="{cx}" cy="{feet}" rx="{S(90)}" ry="{S(14)}" fill="#5b3220" opacity="0.18"/>
      <!-- tail -->
      <path d="M{cx + S(60)},{feet - S(30)} C{cx + S(140)},{feet - S(40)} {cx + S(150)},{feet - S(110)} {cx + S(120)},{feet - S(140)}" stroke="url(#fur)" stroke-width="{S(26)}" fill="none" stroke-linecap="round"/>
      <path d="M{cx + S(126)},{feet - S(70)} c{S(16)},{-S(10)} {S(18)},{-S(30)} {S(4)},{-S(60)}" stroke="#f7cf8a" stroke-width="{S(8)}" fill="none" stroke-linecap="round" opacity="0.7"/>
      <!-- haunches -->
      <ellipse cx="{cx - S(52)}" cy="{feet - S(45)}" rx="{S(46)}" ry="{S(44)}" fill="url(#furDark)"/>
      <ellipse cx="{cx + S(52)}" cy="{feet - S(45)}" rx="{S(46)}" ry="{S(44)}" fill="url(#furDark)"/>
      <!-- body -->
      <ellipse cx="{cx}" cy="{feet - S(95)}" rx="{S(66)}" ry="{S(92)}" fill="url(#fur)"/>
      <!-- chest fluff -->
      <path d="M{cx - S(40)},{feet - S(150)} Q{cx},{feet - S(30)} {cx + S(40)},{feet - S(150)} Q{cx},{feet - S(120)} {cx - S(40)},{feet - S(150)} Z" fill="url(#furLight)"/>
      <!-- front legs -->
      <rect x="{cx - S(36)}" y="{feet - S(95)}" width="{S(26)}" height="{S(92)}" rx="{S(13)}" fill="url(#fur)"/>
      <rect x="{cx + S(10)}" y="{feet - S(95)}" width="{S(26)}" height="{S(92)}" rx="{S(13)}" fill="url(#fur)"/>
      <ellipse cx="{cx - S(24)}" cy="{feet - S(6)}" rx="{S(18)}" ry="{S(10)}" fill="#f2c07c"/>
      <ellipse cx="{cx + S(22)}" cy="{feet - S(6)}" rx="{S(18)}" ry="{S(10)}" fill="#f2c07c"/>
      <!-- collar -->
      <path d="M{cx - S(44)},{feet - S(176)} Q{cx},{feet - S(150)} {cx + S(44)},{feet - S(176)}" stroke="#3fa7f5" stroke-width="{S(12)}" fill="none" stroke-linecap="round"/>
      <circle cx="{cx}" cy="{feet - S(152)}" r="{S(9)}" fill="url(#brass)"/>
      {surf_head(cx, feet - S(225), s)}
    </g>'''

def surf_lying(cx, cy, s=1.0, sleeping=False):
    """Surf lying down, head on paws, facing left-ish. (cx, cy) = middle of his body."""
    def S(v): return v * s
    eyes = ""
    head = surf_head(cx - S(95), cy - S(40), s * 0.9, tongue=not sleeping, look=-4)
    if sleeping:
        head = head.replace('fill="#2b1a10"/>\n      <ellipse', 'fill="#2b1a10"/>\n      <ellipse', 1)
    return f'''
    <g filter="url(#shadow)">
      <path d="M{cx + S(120)},{cy + S(10)} C{cx + S(190)},{cy} {cx + S(210)},{cy - S(40)} {cx + S(200)},{cy - S(70)}" stroke="url(#fur)" stroke-width="{S(24)}" fill="none" stroke-linecap="round"/>
      <ellipse cx="{cx + S(20)}" cy="{cy}" rx="{S(130)}" ry="{S(55)}" fill="url(#fur)"/>
      <ellipse cx="{cx + S(85)}" cy="{cy + S(5)}" rx="{S(50)}" ry="{S(42)}" fill="url(#furDark)"/>
      <ellipse cx="{cx - S(40)}" cy="{cy + S(18)}" rx="{S(60)}" ry="{S(34)}" fill="url(#furLight)" opacity="0.7"/>
      <rect x="{cx - S(170)}" y="{cy + S(22)}" width="{S(110)}" height="{S(26)}" rx="{S(13)}" fill="url(#fur)"/>
      <rect x="{cx - S(160)}" y="{cy + S(36)}" width="{S(105)}" height="{S(24)}" rx="{S(12)}" fill="#f0b562"/>
      <path d="M{cx - S(140)},{cy - S(4)} Q{cx - S(100)},{cy + S(20)} {cx - S(60)},{cy - S(6)}" stroke="#3fa7f5" stroke-width="{S(11)}" fill="none" stroke-linecap="round"/>
      {head}
    </g>'''

def surf_portrait():
    """Square icon of Surf's face for the board and dialogue (viewBox 0 0 256 256)."""
    return svg(f'<g filter="url(#shadow)">{surf_head(128, 108, 1.15)}</g>'
               f'<path d="M58,214 Q128,250 198,214 L190,240 Q128,262 66,240 Z" fill="#3fa7f5" opacity="0"/>', 256, 256)

def mandy_portrait():
    return svg(f'{mandy_bust(128, 150, 0.9, wave=False)}', 256, 256)
