#!/usr/bin/env python3
"""Generates launcher-icon assets from the master Play Store icon.

Reads play_store_icon_final.png.png (512x512, full-bleed artwork on a solid
background) and produces, per density:

- mipmap-*/ic_launcher_foreground.png — adaptive-icon foreground: the artwork
  scaled so its content fits the 66/108 safe zone, on the sampled background
  color (seamless against the solid background layer)
- mipmap-*/ic_launcher_monochrome.png — Android 13+ themed-icon silhouette,
  built by alpha-masking pixels that differ from the background color
- drawable-nodpi/logo_rhythmwise.png — full-color artwork on transparency,
  used as the in-app logo (unlock screen)

Re-run after changing the master icon:

    python tools/generate_launcher_icons.py
"""

import os
from PIL import Image, ImageChops

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "play_store_icon_final.png.png")
RES = os.path.join(ROOT, "composeApp", "src", "androidMain", "res")

DENSITIES = {"mdpi": 1.0, "hdpi": 1.5, "xhdpi": 2.0, "xxhdpi": 3.0, "xxxhdpi": 4.0}
LAYER_DP = 108
# Launchers mask the outer edge of the 108dp layer; artwork must stay inside
# the central 66dp circle to survive every mask shape.
SAFE_FRACTION = 66 / 108
# Luma-difference-to-alpha slope for the silhouette mask: full opacity by ~85
# of luma difference, smooth ramp through antialiased edges below that.
ALPHA_SLOPE = 3


def content_geometry(img, bg):
    """Bounding box of everything that isn't the background color."""
    diff = ImageChops.difference(img, Image.new("RGB", img.size, bg)).convert("L")
    bbox = diff.point(lambda v: 255 if v > 30 else 0).getbbox()
    left, top, right, bottom = bbox
    extent = max(right - left, bottom - top)
    center = ((left + right) / 2, (top + bottom) / 2)
    return extent, center


def paste_scaled(canvas, art, art_center, scale):
    """Paste `art` scaled by `scale` so `art_center` lands at the canvas center."""
    resized = art.resize(
        (round(art.width * scale), round(art.height * scale)), Image.LANCZOS
    )
    x = round(canvas.width / 2 - art_center[0] * scale)
    y = round(canvas.height / 2 - art_center[1] * scale)
    canvas.paste(resized, (x, y), resized if resized.mode == "RGBA" else None)


def main():
    img = Image.open(SRC).convert("RGB")
    bg = img.getpixel((2, 2))
    extent, center = content_geometry(img, bg)
    print(f"background {'#%02X%02X%02X' % bg}, content extent {extent}px of {img.width}px")

    # Silhouette + transparent-background logo, both derived from the same mask.
    alpha = ImageChops.difference(img, Image.new("RGB", img.size, bg)) \
        .convert("L").point(lambda v: min(255, v * ALPHA_SLOPE))
    mono = Image.new("RGBA", img.size, (255, 255, 255, 0))
    mono.putalpha(alpha)
    logo = img.convert("RGBA")
    logo.putalpha(alpha)

    for density, mult in DENSITIES.items():
        canvas_px = round(LAYER_DP * mult)
        scale = (SAFE_FRACTION * canvas_px) / extent
        out_dir = os.path.join(RES, f"mipmap-{density}")
        os.makedirs(out_dir, exist_ok=True)

        fg = Image.new("RGB", (canvas_px, canvas_px), bg)
        paste_scaled(fg, img, center, scale)
        fg.save(os.path.join(out_dir, "ic_launcher_foreground.png"))

        mc = Image.new("RGBA", (canvas_px, canvas_px), (255, 255, 255, 0))
        paste_scaled(mc, mono, center, scale)
        mc.save(os.path.join(out_dir, "ic_launcher_monochrome.png"))
        print(f"mipmap-{density}: {canvas_px}px layers")

    logo_dir = os.path.join(RES, "drawable-nodpi")
    os.makedirs(logo_dir, exist_ok=True)
    logo.save(os.path.join(logo_dir, "logo_rhythmwise.png"))
    print("drawable-nodpi/logo_rhythmwise.png")
    print(f'Set ic_launcher_background to {"#%02X%02X%02X" % bg}')


if __name__ == "__main__":
    main()
