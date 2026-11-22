"""Synthesizes the game's sound effects (original, no license needed) as small WAV files.
Run from the repo root: python3 tools/make_sfx.py"""
import numpy as np, wave, os
SR = 22050
OUT = "android/app/src/main/res/raw"

def env(n, a=0.005, d=0.3):
    t = np.arange(n) / SR
    e = np.minimum(1, t / a) * np.exp(-t / d)
    return e

def tone(f, dur, d=0.25, harm=(1, .5, .25), a=0.004, bend=0.0):
    n = int(SR * dur); t = np.arange(n) / SR
    freq = f * (1 + bend * t / dur)
    ph = 2 * np.pi * np.cumsum(freq) / SR
    s = sum(h * np.sin(ph * (i + 1)) for i, h in enumerate(harm))
    return s * env(n, a, d)

def bell(f, dur, d=0.35):
    n = int(SR * dur); t = np.arange(n) / SR
    s = np.sin(2*np.pi*f*t) + .6*np.sin(2*np.pi*f*2.76*t)*np.exp(-t/0.08) + .3*np.sin(2*np.pi*f*5.4*t)*np.exp(-t/0.04)
    return s * env(n, 0.002, d)

def seq(parts, gap):
    total = int(SR * (gap * (len(parts) - 1))) + max(len(p) for p in parts) + 10
    out = np.zeros(total)
    for i, p in enumerate(parts):
        o = int(SR * gap * i); out[o:o + len(p)] += p
    return out

def noise(dur, d=0.1, lp=0.2):
    n = int(SR * dur); x = np.random.default_rng(1).standard_normal(n)
    y = np.zeros(n); acc = 0
    for i in range(n):
        k = lp * (1 - i / n) + 0.02
        acc += k * (x[i] - acc); y[i] = acc
    return y * env(n, 0.01, d)

def save(name, s, vol=0.6):
    s = s / (np.max(np.abs(s)) + 1e-9) * vol
    tail = int(SR * 0.01); s[-tail:] *= np.linspace(1, 0, tail)
    with wave.open(os.path.join(OUT, name + ".wav"), "wb") as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR)
        w.writeframes((s * 32767).astype(np.int16).tobytes())

if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    save("sfx_pop", tone(520, 0.14, d=0.05, bend=0.9, harm=(1, .3)), 0.5)
    save("sfx_tap", tone(900, 0.05, d=0.012, harm=(1,)), 0.35)
    save("sfx_merge", seq([tone(660, .25, .12, bend=.3), bell(990, .4, .2)], 0.06), 0.6)
    save("sfx_coin", seq([bell(1976, .25, .1), bell(2637, .45, .2)], 0.07), 0.45)
    save("sfx_error", seq([tone(220, .14, .06, harm=(1, .4, .2)), tone(180, .2, .08, harm=(1, .4, .2))], 0.12), 0.4)
    save("sfx_levelup", seq([bell(f, .7, .35) for f in (523, 659, 784, 1047, 1319)], 0.09), 0.6)
    save("sfx_order", seq([bell(784, .4, .2), bell(988, .4, .2), bell(1175, .6, .3)], 0.08), 0.55)
    save("sfx_correct", seq([bell(880, .3, .15), bell(1319, .5, .25)], 0.08), 0.5)
    save("sfx_wrong", tone(330, .3, .12, bend=-.35, harm=(1, .5, .3)), 0.35)
    save("sfx_whoosh", noise(.35, .12), 0.35)
    save("sfx_sparkle", seq([bell(f, .35, .15) for f in (1568, 2093, 2637, 3136, 2637, 3520)], 0.045), 0.35)
    save("sfx_unlock", seq([noise(.15, .05, .5), bell(1047, .5, .25), bell(1568, .5, .25)], 0.07), 0.5)
    save("sfx_woof", seq([tone(300, .12, .05, bend=-.4, harm=(1, .8, .6, .4)), tone(280, .16, .06, bend=-.5, harm=(1, .8, .6, .4))], 0.17), 0.45)
    print(sorted(os.listdir(OUT)))
