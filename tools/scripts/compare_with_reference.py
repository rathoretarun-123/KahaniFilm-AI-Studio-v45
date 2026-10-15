#!/usr/bin/env python3
"""Measure a rendered film against its reference clip and write a JSON + Markdown report.

Measures (ffmpeg/ffprobe + Pillow, no model downloads): container metadata, black and frozen segments, duplicate
frame share (mpdecimate), scene-cut count, mean luma, colour-histogram similarity between the two clips' sampled
frames, and a tiled contact sheet of both for human inspection. These are technical properties; cinematic quality
is judged from the frames, never from one number.

Usage: compare_with_reference.py <rendered.mp4> <reference.mp4> <out_dir> <label>
"""
import json, subprocess, sys, math
from pathlib import Path
from PIL import Image

def run(cmd):
    return subprocess.run(cmd, capture_output=True, text=True)

def probe(path):
    r = run(["ffprobe", "-v", "error", "-show_entries", "format=duration,size:stream=codec_type,codec_name,width,height,r_frame_rate,nb_frames", "-of", "json", str(path)])
    d = json.loads(r.stdout or "{}")
    out = {"duration": float(d.get("format", {}).get("duration", 0)), "size": int(d.get("format", {}).get("size", 0))}
    for s in d.get("streams", []):
        if s.get("codec_type") == "video":
            num, den = s.get("r_frame_rate", "0/1").split("/")
            out.update({"codec": s.get("codec_name"), "width": s.get("width"), "height": s.get("height"), "fps": round(int(num) / max(1, int(den)), 3), "frames": int(s.get("nb_frames") or 0)})
        if s.get("codec_type") == "audio":
            out["audio"] = s.get("codec_name")
    return out

def count(path, vf, needle):
    r = run(["ffmpeg", "-v", "info", "-i", str(path), "-vf", vf, "-an", "-f", "null", "-"])
    return (r.stderr or "").count(needle)

def frames_kept(path):
    r = run(["ffmpeg", "-v", "info", "-i", str(path), "-vf", "mpdecimate,showinfo", "-an", "-f", "null", "-"])
    return (r.stderr or "").count(" n:")

def mean_luma(path):
    r = run(["ffmpeg", "-v", "info", "-i", str(path), "-vf", "signalstats,metadata=print:key=lavfi.signalstats.YAVG:file=-", "-an", "-f", "null", "-"])
    vals = [float(l.split("=")[1]) for l in (r.stdout or "").splitlines() if "YAVG" in l]
    return round(sum(vals) / len(vals), 1) if vals else None

def sample(path, out_dir, label, n=12):
    out_dir.mkdir(parents=True, exist_ok=True)
    dur = probe(path)["duration"] or 1
    files = []
    for i in range(n):
        t = dur * (i + 0.5) / n
        f = out_dir / ("%s_%02d.png" % (label, i))
        run(["ffmpeg", "-v", "error", "-y", "-ss", "%.3f" % t, "-i", str(path), "-frames:v", "1", "-vf", "scale=320:-2", str(f)])
        if f.exists():
            files.append(f)
    return files

def hist(f):
    im = Image.open(f).convert("RGB").resize((64, 36))
    h = im.histogram()
    s = float(sum(h)) or 1
    return [x / s for x in h]

def hist_similarity(a, b):
    # Bhattacharyya coefficient over the RGB histograms, 0..1 (1 = identical colour distribution)
    return sum(math.sqrt(x * y) for x, y in zip(a, b))

def tile(files, out, cols=6):
    ims = [Image.open(f) for f in files]
    if not ims:
        return
    w, h = ims[0].size
    rows = (len(ims) + cols - 1) // cols
    sheet = Image.new("RGB", (cols * w, rows * h), (20, 20, 20))
    for i, im in enumerate(ims):
        sheet.paste(im.resize((w, h)), ((i % cols) * w, (i // cols) * h))
    sheet.save(out)

def main(rendered, reference, out_dir, label):
    rendered, reference, out_dir = Path(rendered), Path(reference), Path(out_dir)
    out_dir.mkdir(parents=True, exist_ok=True)
    rep = {"label": label, "rendered": str(rendered), "reference": str(reference)}
    for key, p in (("rendered", rendered), ("reference", reference)):
        m = probe(p)
        m["black_segments"] = count(p, "blackdetect=d=0.1:pix_th=0.10", "black_start")
        m["frozen_segments"] = count(p, "freezedetect=n=0.003:d=0.5", "freeze_start")
        kept = frames_kept(p)
        m["frames_after_mpdecimate"] = kept
        m["duplicate_frame_share"] = round(1 - kept / m["frames"], 3) if m.get("frames") else None
        m["scene_cuts"] = count(p, "select='gt(scene,0.3)',showinfo", " n:")
        m["mean_luma"] = mean_luma(p)
        rep[key + "_metrics"] = m
    rs = sample(rendered, out_dir / "frames", label + "_rendered")
    fs = sample(reference, out_dir / "frames", label + "_reference")
    tile(rs, out_dir / (label + "_rendered_tiles.jpg"))
    tile(fs, out_dir / (label + "_reference_tiles.jpg"))
    if rs and fs:
        hr = [hist(f) for f in rs]; hf = [hist(f) for f in fs]
        best = [max(hist_similarity(a, b) for b in hf) for a in hr]
        rep["colour_similarity"] = {"mean_best_match": round(sum(best) / len(best), 3), "min": round(min(best), 3), "note": "Bhattacharyya coefficient of RGB histograms, sampled frames; a palette measure, not a quality score"}
    (out_dir / (label + "_comparison.json")).write_text(json.dumps(rep, indent=1, ensure_ascii=False), encoding="utf-8")
    md = ["# %s — rendered vs reference" % label, "", "| property | rendered | reference |", "|---|---|---|"]
    keys = ["width", "height", "fps", "duration", "frames", "codec", "audio", "size", "black_segments", "frozen_segments", "duplicate_frame_share", "scene_cuts", "mean_luma"]
    for k in keys:
        md.append("| %s | %s | %s |" % (k, rep["rendered_metrics"].get(k), rep["reference_metrics"].get(k)))
    if "colour_similarity" in rep:
        md.append(""); md.append("Colour-histogram similarity (sampled frames, best match): mean %s, min %s." % (rep["colour_similarity"]["mean_best_match"], rep["colour_similarity"]["min"]))
    (out_dir / (label + "_comparison.md")).write_text("\n".join(md) + "\n", encoding="utf-8")
    print("\n".join(md))

if __name__ == "__main__":
    main(*sys.argv[1:5])
