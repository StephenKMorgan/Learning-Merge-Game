"""Builds the launcher icon: an iced cinnamon roll with Surf peeking over it, on a warm gradient.
Run from repo root after prepare_art.py's cache exists: python3 tools/make_icon.py <cinnamon_png_dir> <emoji_png_dir>"""
import sys, os
from PIL import Image, ImageDraw, ImageFilter
cin, emo = sys.argv[1], sys.argv[2]
R = "android/app/src/main/res"
def fg(size):
    c = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    dog = Image.open(os.path.join(emo, "dog_face.png")).convert("RGBA").resize((int(size * .36),) * 2, Image.LANCZOS)
    c.alpha_composite(dog, (int(size * .44), int(size * .2)))
    roll = Image.open(os.path.join(cin, "iced_roll.png")).convert("RGBA").resize((int(size * .52),) * 2, Image.LANCZOS)
    c.alpha_composite(roll, (int(size * .2), int(size * .33)))
    return c
def bg(size):
    im = Image.new("RGBA", (size, size))
    d = ImageDraw.Draw(im)
    for y in range(size):
        t = y / size
        d.line([(0, y), (size, y)], fill=(int(255), int(214 - 40 * t), int(170 - 10 * t), 255))
    return im
dens = {"mdpi": 1, "hdpi": 1.5, "xhdpi": 2, "xxhdpi": 3, "xxxhdpi": 4}
for k, m in dens.items():
    d = os.path.join(R, "mipmap-" + k); os.makedirs(d, exist_ok=True)
    s = int(108 * m)
    fg(s).save(os.path.join(d, "ic_launcher_foreground.png"))
    L = int(48 * m)
    full = bg(s); full.alpha_composite(fg(s))
    mask = Image.new("L", (s, s), 0); ImageDraw.Draw(mask).rounded_rectangle((s*.09, s*.09, s*.91, s*.91), radius=int(s*.2), fill=255)
    out = Image.new("RGBA", (s, s), (0, 0, 0, 0)); out.paste(full, (0, 0), mask)
    out.crop((int(s*.09), int(s*.09), int(s*.91), int(s*.91))).resize((L, L), Image.LANCZOS).save(os.path.join(d, "ic_launcher.png"))
