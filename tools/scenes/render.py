import json, os, subprocess, tempfile
HERE = os.path.dirname(os.path.abspath(__file__))

def render(items):
    """items: list of (svg_text, png_path, width, height)."""
    with tempfile.TemporaryDirectory() as d:
        jobs = []
        for i, (text, png, w, h) in enumerate(items):
            p = os.path.join(d, f"{i}.svg")
            open(p, "w").write(text)
            jobs.append({"svg": p, "png": png, "width": w, "height": h})
        jf = os.path.join(d, "jobs.json"); json.dump(jobs, open(jf, "w"))
        subprocess.check_call(["node", os.path.join(HERE, "render.js"), jf])
