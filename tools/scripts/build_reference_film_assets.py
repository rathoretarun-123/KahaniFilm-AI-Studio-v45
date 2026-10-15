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

def sheet_panels(im):
    """The panels of a contact sheet, as boxes in reading order, found by the gutters of the sheet's own colour
    (the same rule as the studio's Angles.sheetPanels): a row or column is a gutter when nine tenths of it is the
    border's colour; the stretches between gutters, each at least a tenth of the sheet, are the panels. None when
    the sheet has no inner gutter."""
    small = im.convert("RGB")
    w, h = small.size
    px = small.load()
    border = [px[x, y] for x in range(0, w, max(1, w // 64)) for y in (0, 1, h - 2, h - 1)] + \
             [px[x, y] for y in range(0, h, max(1, h // 64)) for x in (0, 1, w - 2, w - 1)]
    bg = tuple(sum(c[i] for c in border) // len(border) for i in range(3))
    def gutter(c):
        return (min(c) >= 215 and max(c) - min(c) <= 30) or (abs(c[0] - bg[0]) + abs(c[1] - bg[1]) + abs(c[2] - bg[2])) < 40
    gc = [sum(1 for y in range(h) if gutter(px[x, y])) >= h * 0.9 for x in range(w)]
    gr = [sum(1 for x in range(w) if gutter(px[x, y])) >= w * 0.9 for y in range(h)]
    def spans(g, n):
        out, inner, i, max_gut = [], 0, 0, max(3, round(n * 0.06))
        while i < n:
            if g[i]:
                j = i
                while j < n and g[j]:
                    j += 1
                if j - i > max_gut and i > 0 and j < n:
                    return None
                if i > 0 and j < n:
                    inner += 1
                i = j
                continue
            j = i
            while j < n and not g[j]:
                j += 1
            if j - i >= n * 0.1:
                out.append((i, j))
            i = j
        if inner == 0:
            return out if len(out) == 1 else None
        return out
    cs, rs = spans(gc, w), spans(gr, h)
    if cs is None or rs is None or len(cs) * len(rs) < 2:
        return None
    return [(c[0], r[0], c[1], r[1]) for r in rs for c in cs]

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
            panels = sheet_panels(im)
            if panels is not None and panel < len(panels):
                box, grid = panels[panel], "gutters"
            else:
                cols, rows = 3, 2
                c, r = panel % cols, panel // cols
                box, grid = (c * im.width // cols, r * im.height // rows, (c + 1) * im.width // cols, (r + 1) * im.height // rows), "3x2"
            im.crop(box).save(dst)
            crops.append({"file": dst.name, "sheet": src.name, "panel": panel + 1, "grid": grid, "box": list(box)})
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
