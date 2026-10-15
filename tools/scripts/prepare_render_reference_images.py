#!/usr/bin/env python3
"""Convert bundled WebP reference stills to PNG for the off-device AWT renderer tests.

The Android app uses Android's native WebP decoder. The desktop renderer uses ImageIO,
which does not consistently decode WebP on stock JDKs, so this test-only conversion keeps
the reference assets identical while making them readable to the JVM visual audit.
"""
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "app" / "src" / "main" / "assets" / "film_library"
OUT = ROOT / "tools" / "robotest" / "build" / "reference_images"
FILMS = ("aakhiri_chai", "babu_ke_liye")
EXTS = {".jpg", ".jpeg", ".png", ".webp"}

def main():
    total = 0
    for film in FILMS:
        source = ASSETS / film / "stills"
        dest = OUT / film
        if not source.is_dir():
            raise SystemExit(f"Missing bundled still directory: {source}")
        dest.mkdir(parents=True, exist_ok=True)
        count = 0
        for path in sorted(source.iterdir(), key=lambda p: p.name.lower()):
            if not path.is_file() or path.suffix.lower() not in EXTS:
                continue
            with Image.open(path) as im:
                im.convert("RGBA").save(dest / (path.stem + ".png"), format="PNG", optimize=True)
            count += 1
        expected = 77 if film == "aakhiri_chai" else 53
        if count != expected:
            raise SystemExit(f"{film}: expected {expected} original stills, converted {count}")
        total += count
        print(f"{film}: converted {count} reference stills to {dest}")
    print(f"Converted {total} reference stills for desktop render tests.")

if __name__ == "__main__":
    main()
