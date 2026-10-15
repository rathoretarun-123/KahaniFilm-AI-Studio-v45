#!/usr/bin/env python3
"""Build a desktop-renderable asset folder (cast.txt + PNG pictures) for one bundled film from its semantic
reference manifest, so tools/jvm MakeFilm can render the whole film with the user's real references.

Mirrors the role rules of tools/robotest ReferenceArtLoader: front references become the character's picture,
back/side references its views, the first panel of a prop sheet its insert, location references (or the first
panels of location sheets) the scene backgrounds. Contact sheets are split as 3x2 grids; every crop records its
source and box in crops.json. Nothing is invented: a character or place without a tagged reference is left out
and listed in missing.txt.

Usage: build_reference_film_assets.py <aakhiri_chai|babu_ke_liye> <out_dir>
"""
import json, sys
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
PLANS = {
    "aakhiri_chai": ["tea_stall", "rainy_lane", "tea_stall", "old_delhi_lane", "tea_stall"],
    "babu_ke_liye": ["old_post_office", "old_post_office_lane", "tree_shade", "village_lane", "old_post_office"],
}

def main(film, out):
    out = Path(out); out.mkdir(parents=True, exist_ok=True)
    manifest = json.load(open(ROOT / "tools" / "testdata" / film / "reference_manifest.json", encoding="utf-8"))
    stills = ROOT / "app" / "src" / "main" / "assets" / "film_library" / film / "stills"
    lines, crops, missing, used = [], [], [], set()
    chars, views, props, locations = set(), set(), set(), {}

    def source(row):
        name = Path(row["original_filename"]).name
        for p in stills.iterdir():
            if p.stem.split("__")[-1] == Path(name).stem:
                return p
        return None

    def copy(src, tag):
        dst = out / (tag + "_" + src.stem.split("__")[-1][:40] + ".png")
        if not dst.exists():
            Image.open(src).convert("RGBA").save(dst)
        return dst.name

    def crop(src, tag, panel):
        dst = out / (tag + "_" + src.stem.split("__")[-1][:40] + "_p%d.png" % panel)
        if not dst.exists():
            im = Image.open(src).convert("RGBA")
            cols, rows = 3, 2
            c, r = panel % cols, panel // cols
            box = (c * im.width // cols, r * im.height // rows, (c + 1) * im.width // cols, (r + 1) * im.height // rows)
            im.crop(box).save(dst)
            crops.append({"file": dst.name, "sheet": src.name, "panel": panel + 1, "grid": "3x2", "box": list(box)})
        return dst.name

    for row in manifest["uploads"]:
        if not row.get("auto_assign"):
            continue
        role, kind, view = row.get("role", ""), row.get("library_kind", ""), row.get("view", "")
        label, eid, sheet = row.get("entity_label", ""), row.get("entity_id", ""), bool(row.get("source_sheet"))
        src = source(row)
        if not label or not eid or src is None:
            continue
        if role == "character_reference" and kind == "person" and view == "front" and not sheet:
            canonical = manifest.get("scene_plan", {}).get("canonical_fronts", {}).get(eid)
            if canonical and Path(canonical).name != Path(row["original_filename"]).name:
                continue   # a reviewed canonical front: the other fronts stay library pictures
            if label not in chars:
                chars.add(label); lines.append("char|%s|%s" % (label, copy(src, "char_" + eid)))
        elif role == "character_reference" and kind == "view" and view in ("back", "side") and not sheet:
            key = label + "|" + view
            if key not in views:
                views.add(key)
                angle = 180 if view == "back" else 90
                lines.append("view|%s|%d|%s" % (label, angle, copy(src, "view_%s_%s" % (eid, view))))
        elif kind == "place" and role in ("location_reference", "location_sheet"):
            prev = locations.get(eid)
            if prev is None or (prev[1] and not sheet):
                locations[eid] = (src, sheet, label)
        elif kind == "object" and role.startswith("prop_sheet") and eid not in props:
            props.add(eid)
            img = crop(src, "prop_" + eid, 0) if sheet else copy(src, "prop_" + eid)
            lines.append("shot||%s|%s|object" % (label, img))

    plan = PLANS[film]
    screenplay = (ROOT / "app" / "src" / "main" / "assets" / (film + "_screenplay.txt")).read_text(encoding="utf-8")
    scene_count = sum(1 for l in screenplay.splitlines() if l.startswith("दृश्य "))
    panel_at = {}
    reviewed = {s["scene"]: s for s in manifest.get("scene_plan", {}).get("scenes", [])}
    for i in range(scene_count):
        if (i + 1) in reviewed:
            # a reviewed scene plan in the manifest beats the generic rotation
            rs = reviewed[i + 1]
            src = source({"original_filename": rs["original_filename"]})
            if src is None:
                missing.append("scene %d: reviewed reference not found" % (i + 1)); continue
            img = crop(src, "scene_" + rs["entity_id"], rs["panel"]) if rs.get("panel", -1) >= 0 else copy(src, "scene_" + rs["entity_id"])
            lines.append("scene|%d|%s" % (i + 1, img))
            continue
        eid = plan[i % len(plan)]
        loc = locations.get(eid) or (next(iter(locations.values())) if locations else None)
        if loc is None:
            missing.append("scene %d: no location reference" % (i + 1)); continue
        src, sheet, label = loc
        if sheet:
            p = panel_at.get(eid, 0); panel_at[eid] = (p + 1) % 6
            img = crop(src, "scene_" + eid, p)
        else:
            img = copy(src, "scene_" + eid)
        lines.append("scene|%d|%s" % (i + 1, img))
    for m in manifest.get("missing_assets", []):
        if m.get("review_required"):
            missing.append("MISSING_REFERENCE: %s — %s" % (m.get("entity_label"), m.get("reason")))
    (out / "cast.txt").write_text("\n".join(lines) + "\n", encoding="utf-8")
    (out / "crops.json").write_text(json.dumps(crops, ensure_ascii=False, indent=1), encoding="utf-8")
    (out / "missing.txt").write_text("\n".join(missing) + "\n", encoding="utf-8")
    print("%s: %d manifest lines (%d characters, %d views, %d props, %d scenes), %d crops, %d missing notes -> %s"
          % (film, len(lines), len(chars), len(views), len(props), scene_count, len(crops), len(missing), out))

if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
