"""Renders the renovation scenes and character portraits.

Each scene is a stack of layers (base room, decorations, characters). A layer is shown always, or only
once a task is done ("done:c1_bell"), or only until it's done ("undone:c1_sweep"). Layers are trimmed
to their visible pixels and listed in assets/scenes/<scene>.json with their position and z order.

Run from the repo root:  python3 tools/scenes/make_scenes.py [preview_dir]
"""
import json, os, sys, tempfile
from PIL import Image
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from common import W, H, svg, surf_portrait, mandy_portrait
from render import render
import inside, patio, kitchen, cottage

SCALE = 1.25
OUT = "android/app/src/main/assets/scenes"
ITEMS = "android/app/src/main/assets/items"

def build(name, layers, preview_dir=None):
    os.makedirs(OUT, exist_ok=True)
    w, h = int(W * SCALE), int(H * SCALE)
    with tempfile.TemporaryDirectory() as d:
        jobs = [(svg(body), os.path.join(d, f"{lid}.png"), w, h) for (lid, z, show, body) in layers]
        render(jobs)
        meta = []
        full = {}
        for (lid, z, show, _), (_, png, _, _) in zip(layers, jobs):
            im = Image.open(png).convert("RGBA")
            full[lid] = im
            box = im.getbbox() or (0, 0, 1, 1)
            pad = 2
            box = (max(0, box[0] - pad), max(0, box[1] - pad), min(w, box[2] + pad), min(h, box[3] + pad))
            crop = im.crop(box)
            fname = f"{name}_{lid}.webp"
            crop.save(os.path.join(OUT, fname), "WEBP", quality=90, method=6)
            meta.append({"id": lid, "file": fname, "x": box[0], "y": box[1], "w": crop.width, "h": crop.height, "z": z, "show": show})
        json.dump({"w": w, "h": h, "layers": meta}, open(os.path.join(OUT, f"{name}.json"), "w"), indent=1)
        if preview_dir:
            for label, done in [("before", False), ("after", True)]:
                canvas = Image.new("RGBA", (w, h))
                for (lid, z, show, _) in sorted(layers, key=lambda l: l[1]):
                    vis = show == "always" or (show.startswith("done:") and done) or (show.startswith("undone:") and not done)
                    if vis: canvas.alpha_composite(full[lid])
                canvas.convert("RGB").save(os.path.join(preview_dir, f"scene_{name}_{label}.png"))
    print(name, len(layers), "layers")

if __name__ == "__main__":
    prev = sys.argv[1] if len(sys.argv) > 1 else None
    build("inside", inside.layers(), prev)
    build("patio", patio.layers(), prev)
    build("kitchen", kitchen.layers(), prev)
    build("cottage", cottage.layers(), prev)
    with tempfile.TemporaryDirectory() as d:
        render([(surf_portrait(), os.path.join(d, "surf.png"), 256, 256), (mandy_portrait(), os.path.join(d, "mandy.png"), 256, 256)])
        for n in ("surf", "mandy"):
            Image.open(os.path.join(d, f"{n}.png")).resize((192, 192), Image.LANCZOS).save(os.path.join(ITEMS, f"{n}_face.webp"), "WEBP", quality=90)
