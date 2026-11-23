"""Renders the renovation scenes and character portraits.

Each scene is a stack of layers (base room, decorations, characters). A layer is shown always, or only
once a task is done ("done:c1_bell"), or only until it's done ("undone:c1_sweep"). Layers are trimmed
to their visible pixels and listed in assets/scenes/<scene>.json with their position and z order.

Run from the repo root:  python3 tools/scenes/make_scenes.py [preview_dir]
"""
import json, os, sys, tempfile
from PIL import Image
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from common import W, H, svg, surf_portrait, mandy_portrait_parts, DEFAULT_TINT
from render import render
import inside, patio, kitchen, cottage

SCALE = 1.25
OUT = "android/app/src/main/assets/scenes"
ITEMS = "android/app/src/main/assets/items"

def norm(layers):
    """Layers are (id, z, show, svg) or (id, z, show, svg, tint, variant)."""
    return [l if len(l) == 6 else (*l, "", "") for l in layers]

def tint(im, color):
    """Multiply an RGBA image by a hex colour (what the app does with the tintable parts)."""
    r, g, b = int(color[1:3], 16), int(color[3:5], 16), int(color[5:7], 16)
    ch = im.split()
    return Image.merge("RGBA", (ch[0].point(lambda v: v * r // 255), ch[1].point(lambda v: v * g // 255), ch[2].point(lambda v: v * b // 255), ch[3]))

def default_visible(var):
    return var in ("", "bob")

def build(name, layers, preview_dir=None):
    layers = norm(layers)
    os.makedirs(OUT, exist_ok=True)
    w, h = int(W * SCALE), int(H * SCALE)
    with tempfile.TemporaryDirectory() as d:
        jobs = [(svg(body), os.path.join(d, f"{lid}.png"), w, h) for (lid, z, show, body, _t, _v) in layers]
        render(jobs)
        meta = []
        full = {}
        for (lid, z, show, _, tnt, var), (_, png, _, _) in zip(layers, jobs):
            im = Image.open(png).convert("RGBA")
            full[lid] = im
            box = im.getbbox() or (0, 0, 1, 1)
            pad = 2
            box = (max(0, box[0] - pad), max(0, box[1] - pad), min(w, box[2] + pad), min(h, box[3] + pad))
            crop = im.crop(box)
            fname = f"{name}_{lid}.webp"
            crop.save(os.path.join(OUT, fname), "WEBP", quality=90, method=6)
            meta.append({"id": lid, "file": fname, "x": box[0], "y": box[1], "w": crop.width, "h": crop.height, "z": z, "show": show, "tint": tnt, "variant": var})
        json.dump({"w": w, "h": h, "layers": meta}, open(os.path.join(OUT, f"{name}.json"), "w"), indent=1)
        if preview_dir:
            for label, done in [("before", False), ("after", True)]:
                canvas = Image.new("RGBA", (w, h))
                for (lid, z, show, _, tnt, var) in sorted(layers, key=lambda l: l[1]):
                    vis = show == "always" or (show.startswith("done:") and done) or (show.startswith("undone:") and not done)
                    if vis and default_visible(var):
                        canvas.alpha_composite(tint(full[lid], DEFAULT_TINT[tnt]) if tnt else full[lid])
                canvas.convert("RGB").save(os.path.join(preview_dir, f"scene_{name}_{label}.png"))
    print(name, len(layers), "layers")

if __name__ == "__main__":
    prev = sys.argv[1] if len(sys.argv) > 1 else None
    build("inside", inside.layers(), prev)
    build("patio", patio.layers(), prev)
    build("kitchen", kitchen.layers(), prev)
    build("cottage", cottage.layers(), prev)
    with tempfile.TemporaryDirectory() as d:
        render([(surf_portrait(), os.path.join(d, "surf.png"), 256, 256)])
        Image.open(os.path.join(d, "surf.png")).resize((192, 192), Image.LANCZOS).save(os.path.join(ITEMS, "surf_face.webp"), "WEBP", quality=90)
        # Mandy's portrait, in parts (assets/items/mandy/parts.json) plus a default-look image for anywhere else
        parts = mandy_portrait_parts()
        render([(text, os.path.join(d, f"{lid}.png"), 256, 256) for (lid, text, _, _) in parts])
        os.makedirs(os.path.join(ITEMS, "mandy"), exist_ok=True)
        manifest = []
        composite = Image.new("RGBA", (256, 256))
        for (lid, _, tnt, var) in parts:
            im = Image.open(os.path.join(d, f"{lid}.png")).convert("RGBA")
            im.resize((192, 192), Image.LANCZOS).save(os.path.join(ITEMS, "mandy", f"{lid}.webp"), "WEBP", quality=90)
            manifest.append({"file": f"{lid}.webp", "tint": tnt, "variant": var})
            if default_visible(var): composite.alpha_composite(tint(im, DEFAULT_TINT[tnt]) if tnt else im)
        json.dump(manifest, open(os.path.join(ITEMS, "mandy", "parts.json"), "w"), indent=1)
        composite.resize((192, 192), Image.LANCZOS).save(os.path.join(ITEMS, "mandy_face.webp"), "WEBP", quality=90)
