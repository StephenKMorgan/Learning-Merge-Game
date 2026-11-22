"""Draws the cinnamon-roll item chain (there is no cinnamon roll emoji) in a glossy style that sits
next to the Fluent 3D emoji. Output: 256px PNGs, converted to WebP by prepare_art.py.
Run: python3 tools/make_cinnamon_art.py <out_dir>"""
import math, sys, os, random
from PIL import Image, ImageDraw, ImageFilter, ImageChops

S = 1024  # supersampled canvas

def radial(size, inner, outer, center=(0.4, 0.35)):
    w, h = size
    im = Image.new("RGB", size)
    px = im.load()
    cx, cy = center[0] * w, center[1] * h
    maxd = math.hypot(max(cx, w - cx), max(cy, h - cy))
    for y in range(h):
        for x in range(w):
            t = min(1, math.hypot(x - cx, y - cy) / maxd)
            px[x, y] = tuple(int(inner[i] + (outer[i] - inner[i]) * t) for i in range(3))
    return im

def shadow(canvas, box, alpha=70):
    sh = Image.new("L", (S, S), 0)
    ImageDraw.Draw(sh).ellipse(box, fill=alpha)
    sh = sh.filter(ImageFilter.GaussianBlur(28))
    dark = Image.new("RGBA", (S, S), (60, 30, 10, 255))
    dark.putalpha(sh)
    canvas.alpha_composite(dark)

def gloss(canvas, box, alpha=110):
    g = Image.new("L", (S, S), 0)
    ImageDraw.Draw(g).ellipse(box, fill=alpha)
    g = g.filter(ImageFilter.GaussianBlur(22))
    white = Image.new("RGBA", (S, S), (255, 255, 255, 255))
    white.putalpha(g)
    canvas.alpha_composite(white)

def disc(cx, cy, r, inner, outer):
    """A shaded disc as an RGBA layer."""
    layer = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    grad = radial((int(2 * r), int(2 * r)), inner, outer).convert("RGBA")
    mask = Image.new("L", grad.size, 0)
    ImageDraw.Draw(mask).ellipse((0, 0, grad.size[0] - 1, grad.size[1] - 1), fill=255)
    layer.paste(grad, (int(cx - r), int(cy - r)), mask)
    return layer

def spiral(draw, cx, cy, r, turns, width, color, phase=0.0, squash=1.0):
    pts = []
    n = 400
    for i in range(n + 1):
        t = i / n
        a = phase + t * turns * 2 * math.pi
        rr = r * t
        pts.append((cx + rr * math.cos(a), cy + rr * math.sin(a) * squash))
    draw.line(pts, fill=color, width=width, joint="curve")

def roll(canvas, cx, cy, r, baked=True, iced=False, squash=1.0, golden=False):
    if golden:
        inner, outer, line, hi = (255, 236, 140), (214, 150, 20), (170, 105, 10), (255, 248, 200)
    elif baked:
        inner, outer, line, hi = (240, 178, 105), (176, 98, 40), (112, 52, 18), (250, 205, 140)
    else:
        inner, outer, line, hi = (252, 232, 200), (226, 190, 140), (170, 110, 60), (255, 245, 225)
    shadow(canvas, (cx - r * 0.95, cy + r * 0.55 * squash, cx + r * 0.95, cy + r * 1.15 * squash))
    # side (thickness) of the roll
    side = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    ImageDraw.Draw(side).ellipse((cx - r, cy - r * squash + r * 0.18, cx + r, cy + r * squash + r * 0.18),
                                 fill=tuple(int(c * 0.78) for c in outer) + (255,))
    canvas.alpha_composite(side)
    top = disc(cx, cy, r, inner, outer)
    if squash != 1.0:
        top = top.resize((S, S))
    canvas.alpha_composite(top)
    d = ImageDraw.Draw(canvas)
    spiral(d, cx, cy, r * 0.88, 3.2, int(r * 0.09), line + (255,))
    spiral(d, cx + r * 0.02, cy - r * 0.03, r * 0.86, 3.2, int(r * 0.03), hi + (200,), phase=0.25)
    if iced:
        ic = Image.new("RGBA", (S, S), (0, 0, 0, 0))
        di = ImageDraw.Draw(ic)
        rnd = random.Random(int(cx * 7 + cy))
        for k in range(5):
            y0 = cy - r * 0.7 + k * r * 0.33
            pts = []
            for i in range(41):
                x = cx - r * 0.85 + i * (1.7 * r / 40)
                if math.hypot(x - cx, y0 - cy) > r * 0.92:
                    continue
                pts.append((x, y0 + math.sin(i * 0.55 + k) * r * 0.07))
            if len(pts) > 1:
                di.line(pts, fill=(255, 252, 245, 255), width=int(r * 0.09), joint="curve")
        sh2 = ic.split()[3].filter(ImageFilter.GaussianBlur(4))
        drop = Image.new("RGBA", (S, S), (150, 90, 50, 0)); drop.putalpha(sh2.point(lambda v: v * 0.5))
        canvas.alpha_composite(drop, (0, int(r * 0.04)))
        canvas.alpha_composite(ic)
    gloss(canvas, (cx - r * 0.6, cy - r * 0.8, cx + r * 0.1, cy - r * 0.35), 120)

def sparkle(canvas, cx, cy, s, color=(255, 255, 255, 255)):
    d = ImageDraw.Draw(canvas)
    d.polygon([(cx, cy - s), (cx + s * 0.22, cy - s * 0.22), (cx + s, cy), (cx + s * 0.22, cy + s * 0.22),
               (cx, cy + s), (cx - s * 0.22, cy + s * 0.22), (cx - s, cy), (cx - s * 0.22, cy - s * 0.22)], fill=color)

def plate(canvas, cx, cy, rx, ry, color=(250, 250, 255)):
    shadow(canvas, (cx - rx, cy - ry * 0.6, cx + rx, cy + ry * 1.5))
    d = ImageDraw.Draw(canvas)
    d.ellipse((cx - rx, cy - ry + 14, cx + rx, cy + ry + 14), fill=(190, 200, 220, 255))
    d.ellipse((cx - rx, cy - ry, cx + rx, cy + ry), fill=color + (255,))
    d.ellipse((cx - rx * 0.78, cy - ry * 0.74, cx + rx * 0.78, cy + ry * 0.74), fill=(232, 236, 248, 255))

def finish(canvas, path):
    canvas.resize((256, 256), Image.LANCZOS).save(path)

def sticks(path):
    c = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    shadow(c, (200, 700, 860, 860))
    for i, (dx, ang) in enumerate([(-90, -28), (60, -18), (0, -38)]):
        layer = Image.new("RGBA", (S, S), (0, 0, 0, 0))
        d = ImageDraw.Draw(layer)
        x0, y0, x1, y1 = 330 + dx, 170, 470 + dx, 830
        d.rounded_rectangle((x0, y0, x1, y1), radius=60, fill=(150, 75, 30, 255))
        d.rounded_rectangle((x0 + 18, y0 + 10, x0 + 58, y1 - 10), radius=20, fill=(200, 115, 55, 255))
        d.line([(x0 + 95, y0 + 20), (x0 + 95, y1 - 20)], fill=(110, 50, 18, 255), width=10)
        d.ellipse((x0, y0 - 25, x1, y0 + 50), fill=(120, 58, 22, 255))
        spiral(d, (x0 + x1) / 2, y0 + 12, 55, 2.2, 9, (190, 110, 55, 255), squash=0.45)
        layer = layer.rotate(ang, center=(512, 512), resample=Image.BICUBIC)
        c.alpha_composite(layer)
    finish(c, path)

def dough(path):
    c = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    shadow(c, (230, 720, 800, 860))
    c.alpha_composite(disc(512, 560, 300, (255, 248, 230), (228, 196, 150)).resize((S, S)))
    d = ImageDraw.Draw(c)
    # little cinnamon dusting
    rnd = random.Random(3)
    for _ in range(70):
        a = rnd.random() * 6.28; rr = rnd.random() ** 0.5 * 230
        x, y = 512 + math.cos(a) * rr, 520 + math.sin(a) * rr * 0.8
        d.ellipse((x - 7, y - 7, x + 7, y + 7), fill=(150, 80, 35, 220))
    gloss(c, (330, 330, 560, 470), 150)
    finish(c, path)

def single(path, baked, iced, golden=False):
    c = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    roll(c, 512, 500, 330, baked=baked, iced=iced, golden=golden)
    if golden:
        for (x, y, s) in [(180, 220, 70), (830, 300, 55), (800, 780, 80), (230, 760, 45)]:
            sparkle(c, x, y, s, (255, 250, 210, 255))
    finish(c, path)

def plated(path, n):
    c = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    if n == 3:
        plate(c, 512, 640, 440, 220)
        for (x, y, r) in [(330, 560, 175), (690, 560, 175), (510, 690, 185)]:
            roll(c, x, y, r, iced=True)
    else:
        d = ImageDraw.Draw(c)
        shadow(c, (90, 640, 934, 920))
        d.rounded_rectangle((80, 230, 944, 860), radius=60, fill=(150, 160, 175, 255))
        d.rounded_rectangle((110, 250, 914, 830), radius=46, fill=(205, 212, 225, 255))
        for row in range(2):
            for col in range(3):
                roll(c, 250 + col * 262, 420 + row * 250, 125, iced=True)
    finish(c, path)

if __name__ == "__main__":
    out = sys.argv[1]
    os.makedirs(out, exist_ok=True)
    sticks(os.path.join(out, "cinnamon_sticks.png"))
    dough(os.path.join(out, "dough_ball.png"))
    single(os.path.join(out, "cinnamon_swirl.png"), baked=False, iced=False)
    single(os.path.join(out, "cinnamon_roll.png"), baked=True, iced=False)
    single(os.path.join(out, "iced_roll.png"), baked=True, iced=True)
    plated(os.path.join(out, "roll_plate.png"), 3)
    plated(os.path.join(out, "roll_tray.png"), 6)
    single(os.path.join(out, "golden_roll.png"), baked=True, iced=True, golden=True)
