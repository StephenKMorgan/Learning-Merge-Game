"""Composes and renders the game's background music (original, no license needed).

Everything is synthesized here: electric piano, upright bass, brushed drums, celesta melody,
plucked guitar and marimba, plus a little room reverb. Each track is rendered so its end flows
straight back into its start, then encoded to OGG Vorbis (needs ffmpeg).

Run from the repo root:  python3 tools/make_music.py
"""
import os, subprocess, tempfile, wave
import numpy as np

SR = 32000
OUT = "android/app/src/main/res/raw"

# ------------------------------------------------------------------ helpers

def mtof(m): return 440.0 * 2 ** ((m - 69) / 12)

NOTE = {"C": 0, "Db": 1, "D": 2, "Eb": 3, "E": 4, "F": 5, "Gb": 6, "G": 7, "Ab": 8, "A": 9, "Bb": 10, "B": 11}
QUAL = {"maj7": [0, 4, 7, 11, 14], "m7": [0, 3, 7, 10, 14], "7": [0, 4, 7, 10, 14], "6": [0, 4, 7, 9, 14],
        "m6": [0, 3, 7, 9, 14], "maj9": [0, 4, 7, 11, 14], "7sus": [0, 5, 7, 10, 14]}

def chord(sym):
    for q in sorted(QUAL, key=len, reverse=True):
        if sym.endswith(q):
            return NOTE[sym[: -len(q)]], QUAL[q]
    return NOTE[sym], [0, 4, 7, 12]

class Mix:
    def __init__(self, seconds):
        self.n = int(seconds * SR) + SR * 4
        self.L = np.zeros(self.n); self.R = np.zeros(self.n)

    def add(self, sig, t, pan=0.0, gain=1.0):
        i = int(t * SR)
        if i >= self.n: return
        sig = sig[: self.n - i] * gain
        self.L[i:i + len(sig)] += sig * np.sqrt(0.5 * (1 - pan))
        self.R[i:i + len(sig)] += sig * np.sqrt(0.5 * (1 + pan))

def env(n, a=0.005, d=0.3, s=0.0, sd=None):
    t = np.arange(n) / SR
    e = np.minimum(1, t / a) * (s + (1 - s) * np.exp(-t / d))
    if sd: e *= np.exp(-t / sd)
    rel = min(n, int(0.04 * SR)); e[-rel:] *= np.linspace(1, 0, rel)
    return e

def onepole(x, cutoff):
    """Fast one-pole lowpass using FFT-free recursion in chunks."""
    k = np.exp(-2 * np.pi * cutoff / SR)
    y = np.zeros_like(x); prev = 0.0
    for s in range(0, len(x), 48):
        seg = x[s:s + 48]
        # closed form: y[n] = k^n*prev + (1-k)*sum k^(n-j) x[j]
        pw = k ** np.arange(len(seg))
        c = np.cumsum(seg / pw) * pw * (1 - k) + pw * prev * k
        y[s:s + len(seg)] = c
        prev = c[-1]
    return y

# ------------------------------------------------------------------ instruments

def rhodes(f, dur, vel=0.6):
    n = int((dur + 0.6) * SR); t = np.arange(n) / SR
    idx = 1.6 * np.exp(-t / 0.35) * vel
    mod = np.sin(2 * np.pi * f * t)
    car = np.sin(2 * np.pi * f * t + idx * mod)
    tine = 0.12 * np.sin(2 * np.pi * f * 7.1 * t) * np.exp(-t / 0.05)
    body = 0.3 * np.sin(2 * np.pi * f * 1.003 * t)
    e = env(n, 0.003, 1.2, 0.0, None)
    hold = np.ones(n); off = int(dur * SR)
    if off < n: hold[off:] = np.exp(-np.arange(n - off) / SR / 0.12)
    return (car + tine + body) * e * hold * vel

def bass(f, dur, vel=0.8):
    n = int((dur + 0.3) * SR); t = np.arange(n) / SR
    s = np.sin(2 * np.pi * f * t) + 0.35 * np.sin(4 * np.pi * f * t) + 0.12 * np.sin(6 * np.pi * f * t)
    thump = 0.5 * np.sin(2 * np.pi * f * t) * np.exp(-t / 0.03)
    e = env(n, 0.008, 0.9)
    hold = np.ones(n); off = int(dur * SR)
    if off < n: hold[off:] = np.exp(-np.arange(n - off) / SR / 0.05)
    return (s + thump) * e * hold * vel

def celesta(f, dur, vel=0.5):
    n = int((dur + 1.0) * SR); t = np.arange(n) / SR
    s = np.sin(2 * np.pi * f * t) + 0.4 * np.sin(2 * np.pi * f * 2 * t) * np.exp(-t / 0.3) \
        + 0.2 * np.sin(2 * np.pi * f * 3.98 * t) * np.exp(-t / 0.12)
    return s * env(n, 0.002, 0.7) * vel

def marimba(f, dur, vel=0.6):
    n = int((dur + 0.5) * SR); t = np.arange(n) / SR
    s = np.sin(2 * np.pi * f * t) + 0.25 * np.sin(2 * np.pi * f * 3.93 * t) * np.exp(-t / 0.05) \
        + 0.1 * np.sin(2 * np.pi * f * 9.2 * t) * np.exp(-t / 0.015)
    return s * env(n, 0.001, 0.32) * vel

def pluck(f, dur, vel=0.5, rnd=None):
    """Karplus-Strong nylon guitar."""
    n = int((dur + 0.8) * SR)
    p = max(2, int(SR / f))
    rnd = rnd or np.random.default_rng(int(f * 10))
    buf = rnd.uniform(-1, 1, p)
    buf = np.convolve(buf, [0.5, 0.5], mode="same")
    out = np.zeros(n)
    for i in range(0, n, p):
        seg = buf.copy()
        out[i:i + p] = seg[: min(p, n - i)]
        buf = 0.996 * 0.5 * (buf + np.roll(buf, 1))
    return out * env(n, 0.002, 1.5) * vel

def noise(n, seed):
    return np.random.default_rng(seed).standard_normal(n)

def kick(vel=0.5):
    n = int(0.3 * SR); t = np.arange(n) / SR
    f = 110 * np.exp(-t / 0.05) + 45
    return np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t / 0.12) * vel

def brush(seed, length=0.25, vel=0.15):
    n = int(length * SR); x = noise(n, seed)
    x = onepole(x - onepole(x, 1200), 5000)        # band-limited: soft swish, not hiss
    t = np.arange(n) / SR
    e = np.minimum(1, t / 0.03) * np.exp(-t / (length * 0.45))
    return x * e * vel

def tick(seed, vel=0.06):
    n = int(0.05 * SR); x = noise(n, seed)
    x = x - onepole(x, 6000)
    return x * np.exp(-np.arange(n) / SR / 0.012) * vel

def shaker(seed, vel=0.05):
    n = int(0.09 * SR); x = noise(n, seed)
    x = x - onepole(x, 5000)
    t = np.arange(n) / SR
    return x * np.minimum(1, t / 0.02) * np.exp(-t / 0.03) * vel

def sleigh(seed, vel=0.08):
    n = int(0.25 * SR); t = np.arange(n) / SR
    rng = np.random.default_rng(seed)
    s = sum(np.sin(2 * np.pi * rng.uniform(5000, 9000) * t + rng.uniform(0, 6)) for _ in range(6))
    return s * np.exp(-t / 0.06) * (1 + 0.5 * np.sin(2 * np.pi * 22 * t)) * vel

# ------------------------------------------------------------------ reverb & render

def reverb(L, R, wet=0.18, decay=1.6):
    n = int(decay * SR)
    t = np.arange(n) / SR
    rng = np.random.default_rng(9)
    irL = rng.standard_normal(n) * np.exp(-t / (decay / 5)); irR = rng.standard_normal(n) * np.exp(-t / (decay / 5))
    irL[: int(0.012 * SR)] = 0; irR[: int(0.017 * SR)] = 0
    irL /= np.sqrt(np.sum(irL ** 2)); irR /= np.sqrt(np.sum(irR ** 2))
    def conv(x, ir):
        m = len(x) + len(ir)
        size = 1 << (m - 1).bit_length()
        return np.fft.irfft(np.fft.rfft(x, size) * np.fft.rfft(ir, size), size)[: len(x)]
    return L + wet * conv(L, irL), R + wet * conv(R, irR)

def finish(mix, loop_seconds, name):
    L, R = reverb(mix.L, mix.R)
    n = int(loop_seconds * SR)
    # fold the tail past the loop point back onto the start, so the loop is seamless
    tail = L[n:], R[n:]
    L, R = L[:n].copy(), R[:n].copy()
    k = min(len(tail[0]), n)
    L[:k] += tail[0][:k]; R[:k] += tail[1][:k]
    # gentle warmth: roll off harsh highs
    L = 0.75 * L + 0.25 * onepole(L, 5000); R = 0.75 * R + 0.25 * onepole(R, 5000)
    peak = max(np.max(np.abs(L)), np.max(np.abs(R)))
    L, R = L / peak * 0.8, R / peak * 0.8
    stereo = np.stack([L, R], axis=1)
    with tempfile.TemporaryDirectory() as d:
        wav = os.path.join(d, "t.wav")
        with wave.open(wav, "wb") as w:
            w.setnchannels(2); w.setsampwidth(2); w.setframerate(SR)
            w.writeframes((stereo * 32767).astype(np.int16).tobytes())
        subprocess.check_call(["ffmpeg", "-y", "-loglevel", "error", "-i", wav, "-c:a", "libvorbis", "-q:a", "3",
                               os.path.join(OUT, name + ".ogg")])
    print(name, f"{loop_seconds:.1f}s", os.path.getsize(os.path.join(OUT, name + ".ogg")) // 1024, "KB")

# ------------------------------------------------------------------ composition helpers

def voicing(sym, low=53):
    root, iv = chord(sym)
    notes = [root + i for i in iv[1:]]        # rootless: 3, 5, 7, 9
    base = low + ((root - low) % 12)
    out = []
    for nn in notes:
        m = base + (nn - root)
        while m < low: m += 12
        while m > low + 17: m -= 12
        out.append(m)
    return sorted(out)

def scale_tones(key_root, minor=False):
    steps = [0, 2, 3, 5, 7, 8, 10] if minor else [0, 2, 4, 5, 7, 9, 11]
    return sorted({key_root + s + 12 * o for o in range(-1, 8) for s in steps})

def melody(chords, key, rng, phrase_bars=4, low=67, high=84):
    """Singable melody: phrase A, A', B, A'' over the chord list, chord tones on strong beats."""
    scale = [m for m in scale_tones(key) if low <= m <= high]
    rhythms = [
        [(0, 1.5), (1.5, 0.5), (2, 1), (3, 1)],
        [(0, 1), (1, 0.5), (1.5, 1.5), (3, 1)],
        [(0.5, 0.5), (1, 1), (2, 2)],
        [(0, 2), (2, 1), (3, 0.5), (3.5, 0.5)],
        [(0, 3)],
    ]
    def make_phrase(start_bar, seed_offset, ending):
        r = np.random.default_rng(rng + seed_offset)
        notes = []
        cur = scale[len(scale) // 2]
        for b in range(phrase_bars):
            sym = chords[(start_bar + b) % len(chords)]
            root, iv = chord(sym)
            tones = {(root + i) % 12 for i in iv[:4]}
            rhy = rhythms[4] if (b == phrase_bars - 1 and ending) else rhythms[r.integers(0, 4)]
            for (pos, d) in rhy:
                strong = pos in (0, 2)
                cands = [m for m in scale if abs(m - cur) <= 4]
                if strong: cands = [m for m in cands if m % 12 in tones] or [m for m in scale if m % 12 in tones]
                w = np.array([1.0 / (1 + abs(m - cur)) for m in cands])
                cur = cands[r.choice(len(cands), p=w / w.sum())]
                notes.append((start_bar + b, pos, d, cur))
        return notes
    out = []
    total = len(chords)
    form = ["A", "A2", "B", "A3"]
    for i, part in enumerate(form):
        sb = i * phrase_bars
        if sb >= total: break
        if part == "A": base = make_phrase(sb, 0, True); out += base
        elif part == "A2":
            base = make_phrase(0, 0, True)
            alt = make_phrase(sb, 7, True)
            out += [(bb + sb, p, d, m) for (bb, p, d, m) in base if bb < phrase_bars - 1] + [x for x in alt if x[0] == sb + phrase_bars - 1]
        elif part == "B": out += make_phrase(sb, 13, True)
        else:
            base = make_phrase(0, 0, True)
            out += [(bb + sb, p, d, m) for (bb, p, d, m) in base]
    return out

# ------------------------------------------------------------------ tracks

def cinnamon_morning(frosty=False):
    bpm = 82; beat = 60 / bpm; bar = 4 * beat
    prog = ["Fmaj7", "Em7", "Dm7", "C7", "Bbmaj7", "Am7", "Gm7", "C7",
            "Fmaj7", "Em7", "Dm7", "C7", "Bbmaj7", "C7", "Fmaj7", "Fmaj7"]
    prog = prog + ["Bbmaj7", "Bbmaj7", "Am7", "Dm7", "Gm7", "C7", "Fmaj7", "C7sus"] + prog[:8]
    bars = len(prog); L = bars * bar
    mix = Mix(L)
    sw = 0.62  # swing
    rng = np.random.default_rng(3)
    for b, sym in enumerate(prog):
        t0 = b * bar
        root, _ = chord(sym)
        # electric piano comping: on 1, the "and" of 2, sometimes 4
        v = voicing(sym)
        hits = [(0, 1.4), (1 + sw, 1.2)] + ([(3, 0.8)] if b % 2 else [])
        for pos, d in hits:
            for k, m in enumerate(v):
                mix.add(rhodes(mtof(m), d * beat, 0.32 + 0.05 * rng.random()), t0 + pos * beat + k * 0.008, pan=-0.25)
        # walking bass
        br = 36 + ((root - 36) % 12)
        nxt = chord(prog[(b + 1) % bars])[0]
        walk = [br, br + 7, br + 12 if rng.random() < 0.5 else br + 4, 36 + ((nxt - 36) % 12) + (1 if rng.random() < 0.5 else -1)]
        for i, m in enumerate(walk):
            mix.add(bass(mtof(m), beat * 0.9, 0.55), t0 + i * beat, pan=0.05)
        # drums
        for i in range(4):
            if i in (0, 2): mix.add(kick(0.35), t0 + i * beat)
            if i in (1, 3): mix.add(brush(b * 8 + i, 0.3, 0.06), t0 + i * beat, pan=0.2)
            mix.add(tick(b * 16 + i, 0.035), t0 + i * beat, pan=0.4)
            mix.add(tick(b * 16 + i + 100, 0.025), t0 + (i + sw) * beat, pan=0.4)
            if frosty: mix.add(sleigh(b * 4 + i), t0 + (i + sw) * beat, pan=-0.4)
    # melody (celesta), resting through the first 2 bars as an intro
    mel = melody(prog, 5, 11)
    for (bb, pos, d, m) in mel:
        if bb < 2: continue
        p = pos if pos == int(pos) else int(pos) + sw
        mix.add(celesta(mtof(m + (12 if frosty else 0)), d * beat, 0.30), bb * bar + p * beat, pan=0.3)
    finish(mix, L, "music_cinnamon_frosty" if frosty else "music_cinnamon")

def sunny_patio():
    bpm = 104; beat = 60 / bpm; bar = 4 * beat
    prog = ["Cmaj7", "Am7", "Dm7", "G7", "Em7", "A7", "Dm7", "G7",
            "Cmaj7", "C7", "Fmaj7", "Fm6", "Em7", "A7", "Dm7", "G7"]
    prog = prog * 2
    bars = len(prog); L = bars * bar
    mix = Mix(L)
    rng = np.random.default_rng(5)
    for b, sym in enumerate(prog):
        t0 = b * bar
        root, _ = chord(sym)
        v = voicing(sym, 55)
        # bossa guitar pattern (eighths): x . x . . x . x | . x . x x . x .
        pat = [0, 1, 2.5, 3.5] if b % 2 == 0 else [0.5, 1.5, 2, 3]
        for pos in pat:
            for k, m in enumerate(v):
                mix.add(pluck(mtof(m), beat * 0.8, 0.22, rng), t0 + pos * beat + k * 0.012, pan=-0.3)
        # bossa bass: root on 1, fifth on 2&, root on 3, fifth on 4&
        br = 36 + ((root - 36) % 12)
        for pos, m in [(0, br), (1.5, br + 7), (2, br + 7), (3.5, br)]:
            mix.add(bass(mtof(m), beat * 0.7, 0.5), t0 + pos * beat)
        for i in range(8):
            mix.add(shaker(b * 8 + i, 0.045 if i % 2 else 0.03), t0 + i * beat / 2, pan=0.5)
        mix.add(kick(0.25), t0); mix.add(kick(0.2), t0 + 2 * beat)
        for pos in ([0.5, 2, 3.5] if b % 2 == 0 else [1, 2.5]):
            mix.add(tick(b * 10 + int(pos * 2), 0.05), t0 + pos * beat, pan=-0.1)  # rim
    mel = melody(prog, 0, 21, low=72, high=88)
    for (bb, pos, d, m) in mel:
        if bb < 2: continue
        mix.add(marimba(mtof(m), d * beat, 0.32), bb * bar + pos * beat, pan=0.25)
        mix.add(marimba(mtof(m - 12), d * beat, 0.12), bb * bar + pos * beat + 0.01, pan=0.25)
    finish(mix, L, "music_patio")

if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    cinnamon_morning()
    cinnamon_morning(frosty=True)
    sunny_patio()
