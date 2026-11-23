"""Downloads the Fluent 3D emoji the game uses (MIT license, github.com/microsoft/fluentui-emoji),
draws the cinnamon-roll chain, and writes everything as 192px WebP into the app's assets.
Run from the repo root:  python3 tools/prepare_art.py [cache_dir]
To add art: add the emoji's Fluent folder name to NAMES and re-run."""
import os, sys, subprocess, urllib.request, urllib.parse
from PIL import Image

NAMES = """Sheaf of rice|Flatbread|Bread|Bagel|Pretzel|Baguette bread|Croissant|Sandwich|Candy|Lollipop|
Chocolate bar|Cookie|Doughnut|Cupcake|Shortcake|Birthday cake|Egg|Glass of milk|Butter|Cheese wedge|Honey pot|
Custard|Pancakes|Waffle|Beans|Hot beverage|Teacup without handle|Mate|Cup with straw|Bubble tea|Ice cream|
Cherries|Strawberry|Blueberries|Red apple|Peach|Mango|Pineapple|Watermelon|Seedling|Herb|Tulip|Rose|Hibiscus|
Sunflower|Cherry blossom|Bouquet|Bone|Softball|Flying disc|Teddy bear|Ribbon|Crown|Trophy|Dog face|Dog|
Bowl with spoon|Jar|Hut|Basket|Teapot|Potted plant|Wrapped gift|Package|Coin|Money bag|Gem stone|High voltage|
Star|Glowing star|Sparkles|Ticket|Brain|Puzzle piece|Calendar|Shopping bags|Gear|Bar chart|Locked|Unlocked|
Woman cook|Old woman|Old man|Person surfing|Woman farmer|Detective|Police officer|Woman artist|Man cook|Girl|Boy|
Woman teacher|Firefighter|Construction worker|Woman office worker|Farmer|Couch and lamp|Chair|Framed picture|
Window|Door|Candle|Bellhop bell|Bed|Light bulb|Broom|Hammer and wrench|Paintbrush|Umbrella on ground|
Mantelpiece clock|Fork and knife with plate|Fallen leaf|Maple leaf|Chestnut|Mushroom|Pie|Snowflake|Snowman|
Christmas tree|Red heart|Love letter|Heart with ribbon|Four leaf clover|Dove|Rainbow|Water wave|Sun|Ferris wheel|
Crystal ball|Magic wand|Party popper|Confetti ball|Balloon|Hourglass done|Abacus|Input latin letters|
Input numbers|Game die|Thought balloon|Musical note|Speaker high volume|Muted speaker|Vibration mode|Key|
Moon cake|Woman in tuxedo|Shaved ice|Soft ice cream|Lemon|Kiwi fruit|Hot springs|Mobile phone|Admission tickets|House|Salt"""
NAMES = [n.strip() for n in NAMES.replace("\n", "").split("|") if n.strip()]
BASE = "https://raw.githubusercontent.com/microsoft/fluentui-emoji/main/assets/"
OUT = "android/app/src/main/assets/items"

def slug(n): return n.lower().replace(" ", "_").replace("-", "_")

def fetch(name, cache):
    path = os.path.join(cache, slug(name) + ".png")
    if os.path.exists(path): return path
    s = slug(name)
    for rel in (f"{name}/3D/{s}_3d.png", f"{name}/Default/3D/{s}_3d_default.png"):
        try:
            urllib.request.urlretrieve(BASE + urllib.parse.quote(rel), path); return path
        except Exception: pass
    raise SystemExit("missing emoji: " + name)

def webp(src, dst, size=192):
    Image.open(src).convert("RGBA").resize((size, size), Image.LANCZOS).save(dst, "WEBP", quality=88, method=6)

if __name__ == "__main__":
    cache = sys.argv[1] if len(sys.argv) > 1 else "/tmp/fluent-cache"
    os.makedirs(cache, exist_ok=True); os.makedirs(OUT, exist_ok=True)
    for n in NAMES: webp(fetch(n, cache), os.path.join(OUT, slug(n) + ".webp"))
    cin = os.path.join(cache, "cinnamon"); os.makedirs(cin, exist_ok=True)
    subprocess.check_call([sys.executable, "-I", os.path.join(os.path.dirname(os.path.abspath(__file__)), "make_cinnamon_art.py"), cin])
    for f in os.listdir(cin): webp(os.path.join(cin, f), os.path.join(OUT, f[:-4] + ".webp"))
    print(len(os.listdir(OUT)), "images in", OUT)
