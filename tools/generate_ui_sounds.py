#!/usr/bin/env python3
"""Generates the app's UI sound effects into composeApp/src/androidMain/res/raw.

Every sound is synthesized from scratch (sine partials + filtered noise), so the
whole set shares one tonal family and can be re-tuned by editing the parameters
below and re-running:

    python tools/generate_ui_sounds.py

Loudness contract: peaks are authored so the set sounds *subtle* at the app's
default 80% sound-effects volume (a perceptual x^2 curve, so 80% -> 0.64 linear
gain applied by SoundPoolSoundEffectPlayer). 100% gives audible headroom above
the default without clipping.
"""

import math
import os
import random
import struct
import wave

SAMPLE_RATE = 44100
OUT_DIR = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "composeApp", "src", "androidMain", "res", "raw",
)

# Tonal palette: D-major-ish pentatonic so overlapping sounds never clash.
D6, E6, FS6, A6, D7 = 1174.66, 1318.51, 1479.98, 1760.00, 2349.32


def silence(duration):
    return [0.0] * int(SAMPLE_RATE * duration)


def tone(freq_start, freq_end, duration, decay_tau, partial=0.0, attack=0.002):
    """Sine blip with exponential decay, optional pitch glide and 2x partial."""
    n = int(SAMPLE_RATE * duration)
    out = []
    phase = 0.0
    for i in range(n):
        t = i / SAMPLE_RATE
        freq = freq_start * (freq_end / freq_start) ** (t / duration)
        phase += 2.0 * math.pi * freq / SAMPLE_RATE
        env = math.exp(-t / decay_tau)
        if t < attack:
            env *= t / attack
        out.append(env * (math.sin(phase) + partial * math.sin(2.0 * phase)))
    return out


def whoosh(freq_start, freq_end, duration, q=1.8, seed=7, attack=0.35):
    """Band-passed noise with an exponential center-frequency sweep.

    `attack` is the raised-cosine ramp-up as a fraction of the duration; keep it
    short for sounds that must feel instant (the page-change swipe).
    """
    rng = random.Random(seed)
    n = int(SAMPLE_RATE * duration)
    low = band = 0.0
    out = []
    for i in range(n):
        t = i / SAMPLE_RATE
        freq = freq_start * (freq_end / freq_start) ** (t / duration)
        f = 2.0 * math.sin(math.pi * freq / SAMPLE_RATE)
        noise = rng.uniform(-1.0, 1.0)
        high = noise - low - band / q
        band += f * high
        low += f * band
        pos = i / n
        env = 0.5 - 0.5 * math.cos(math.pi * pos / attack) if pos < attack \
            else 0.5 + 0.5 * math.cos(math.pi * (pos - attack) / (1.0 - attack))
        out.append(band * env)
    return out


def click(duration=0.022, freq=D7, seed=3):
    """Very short detent click: damped sine + a pinch of noise transient."""
    rng = random.Random(seed)
    n = int(SAMPLE_RATE * duration)
    out = []
    for i in range(n):
        t = i / SAMPLE_RATE
        body = math.sin(2.0 * math.pi * freq * t) * math.exp(-t / 0.004)
        snap = rng.uniform(-1.0, 1.0) * math.exp(-t / 0.0015) * 0.5
        env_in = min(1.0, t / 0.0008)
        out.append((body + snap) * env_in)
    return out


def mix(*layers):
    """Sum layers of (offset_seconds, samples) into one buffer."""
    total = max(int(off * SAMPLE_RATE) + len(s) for off, s in layers)
    out = [0.0] * total
    for off, samples in layers:
        start = int(off * SAMPLE_RATE)
        for i, v in enumerate(samples):
            out[start + i] += v
    return out


def write_wav(name, samples, peak):
    """Normalize to `peak` (full scale = 1.0), fade tail, write 16-bit mono."""
    top = max(abs(v) for v in samples) or 1.0
    gain = peak / top
    fade = min(len(samples), int(SAMPLE_RATE * 0.004))
    for i in range(fade):
        samples[-1 - i] *= i / fade
    path = os.path.join(OUT_DIR, name)
    with wave.open(path, "wb") as f:
        f.setnchannels(1)
        f.setsampwidth(2)
        f.setframerate(SAMPLE_RATE)
        f.writeframes(b"".join(
            struct.pack("<h", int(max(-1.0, min(1.0, v * gain)) * 32767))
            for v in samples
        ))
    print(f"{name}: {len(samples) / SAMPLE_RATE * 1000:.0f} ms, peak {peak}")


def main():
    # Frequent, tiny feedback — quietest and shortest of the set.
    write_wav("snd_tick.wav", click(), peak=0.50)
    write_wav("snd_tap.wav", tone(D6, D6, 0.06, 0.012, partial=0.30), peak=0.48)
    write_wav("snd_tap_light.wav", tone(FS6, FS6, 0.045, 0.008), peak=0.36)

    # Stateful actions carry direction: on/select rises, off/deselect falls.
    write_wav("snd_toggle_on.wav", tone(620, 980, 0.11, 0.030), peak=0.44)
    write_wav("snd_toggle_off.wav", tone(980, 620, 0.11, 0.030), peak=0.44)
    write_wav("snd_select.wav", tone(E6, E6, 0.12, 0.025, partial=0.25), peak=0.44)
    write_wav("snd_deselect.wav", tone(880, 880, 0.10, 0.020, partial=0.10), peak=0.38)

    # Spatial transitions: pitchless noise so they read as motion, not melody.
    # The swipe leads with a fast attack — it plays mid-gesture and must feel instant.
    write_wav("snd_swipe.wav", whoosh(500, 1600, 0.14, attack=0.12), peak=0.30)
    write_wav("snd_open.wav", whoosh(350, 1200, 0.15, seed=11), peak=0.32)
    write_wav("snd_close.wav", whoosh(1200, 350, 0.15, seed=12), peak=0.32)

    # Outcomes: warm rising arpeggio vs. two soft low pulses.
    write_wav("snd_success.wav", mix(
        (0.00, tone(D6, D6, 0.26, 0.060, partial=0.20)),
        (0.08, tone(FS6, FS6, 0.26, 0.060, partial=0.20)),
        (0.16, tone(A6, A6, 0.30, 0.070, partial=0.20)),
    ), peak=0.42)
    write_wav("snd_error.wav", mix(
        (0.00, tone(196, 196, 0.10, 0.035, partial=0.15)),
        (0.00, tone(208, 208, 0.10, 0.035)),
        (0.11, tone(196, 196, 0.13, 0.040, partial=0.15)),
        (0.11, tone(208, 208, 0.13, 0.040)),
    ), peak=0.46)


if __name__ == "__main__":
    main()
