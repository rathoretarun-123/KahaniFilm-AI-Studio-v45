#!/usr/bin/env python3
"""Frame-by-frame defect scan of a rendered film (and the same stats for a reference clip).

Reads every frame through ffmpeg (no files on disk), measures luma / contrast / saturation / edge energy per frame,
and flags: black or blown frames, flicker (a big luma jump that is not at a shot cut), frozen runs, sudden
large-content jumps mid-shot (sprite pop), and a dull stretch (contrast or saturation under the reference floor).
Writes a text report plus a contact sheet of the flagged frames.
Usage: scan_frames.py film.mp4 out_dir [shots.txt] [reference.mp4]
"""
import sys, os, subprocess, json, math
from PIL import Image, ImageDraw

def probe(path):
    out = subprocess.check_output(["ffprobe", "-v", "error", "-select_streams", "v:0", "-show_entries",
                                   "stream=width,height,r_frame_rate,nb_frames,duration", "-of", "json", path])
    s = json.loads(out)["streams"][0]
    num, den = s["r_frame_rate"].split("/")
    return int(s["width"]), int(s["height"]), float(num) / float(den), float(s.get("duration", 0) or 0)

def frames(path, w, h):
    p = subprocess.Popen(["ffmpeg", "-v", "error", "-i", path, "-f", "rawvideo", "-pix_fmt", "rgb24", "-"], stdout=subprocess.PIPE, bufsize=10 ** 7)
    n = w * h * 3
    while True:
        b = p.stdout.read(n)
        if len(b) < n:
            break
        yield b
    p.wait()

def stats(b, w, h):
    im = Image.frombytes("RGB", (w, h), b)
    small = im.resize((w // 4, h // 4))
    L = small.convert("L")
    hist = L.histogram()
    tot = sum(hist)
    mean = sum(i * c for i, c in enumerate(hist)) / tot
    var = sum(c * (i - mean) ** 2 for i, c in enumerate(hist)) / tot
    hsv = small.convert("HSV")
    sat = sum(i * c for i, c in enumerate(hsv.split()[1].histogram())) / tot
    # edge energy: mean abs horizontal gradient of the luma (sharpness proxy)
    px = list(L.getdata())
    sw = w // 4
    e = 0
    for y in range(0, h // 4, 2):
        row = px[y * sw:(y + 1) * sw]
        e += sum(abs(row[x] - row[x - 1]) for x in range(1, sw, 2))
    e /= (h // 8) * (sw // 2)
    p5 = p95 = None
    acc = 0
    for i, c in enumerate(hist):
        acc += c
        if p5 is None and acc >= tot * 0.05: p5 = i
        if p95 is None and acc >= tot * 0.95: p95 = i; break
    return dict(mean=mean, std=math.sqrt(var), sat=sat, edge=e, p5=p5, p95=p95, thumb=small)

def diff(a, b):
    ha = a.convert("L").histogram(); hb = b.convert("L").histogram()
    return sum(abs(x - y) for x, y in zip(ha, hb)) / float(sum(ha)) * 100  # % of pixels that moved bins

def cut_times(shots):
    ts = []
    if not shots or not os.path.exists(shots): return ts
    for line in open(shots, encoding="utf-8", errors="replace"):
        line = line.strip()
        # lines like "12.34 ..." or "@12.3" — take the first number on the line
        num = ""
        for ch in line:
            if ch.isdigit() or ch == ".": num += ch
            elif num: break
        try:
            if num and 0 <= float(num) < 100000: ts.append(float(num))
        except ValueError: pass
    return ts

def main():
    film, out = sys.argv[1], sys.argv[2]
    shots = sys.argv[3] if len(sys.argv) > 3 else None
    ref = sys.argv[4] if len(sys.argv) > 4 else None
    os.makedirs(out, exist_ok=True)
    w, h, fps, dur = probe(film)
    cuts = cut_times(shots)
    rows, flags = [], []
    prev = None; frozen = 0; frozen_runs = 0; dup = 0; n = 0
    for b in frames(film, w, h):
        s = stats(b, w, h); t = n / fps
        kind = []
        if s["mean"] < 10: kind.append("black")
        if s["mean"] > 245: kind.append("blown")
        if prev is not None:
            d = diff(prev["thumb"], s["thumb"])
            near_cut = any(abs(t - c) < 0.35 for c in cuts)
            if d < 0.05: frozen += 1; dup += 1
            else:
                if frozen >= fps * 0.5: frozen_runs += 1
                frozen = 0
            if abs(s["mean"] - prev["mean"]) > 25 and not near_cut and "black" not in kind: kind.append("flicker%+.0f" % (s["mean"] - prev["mean"]))
            if d > 60 and not near_cut and abs(s["mean"] - prev["mean"]) < 25: kind.append("jump%.0f%%" % d)
        else:
            d = 0
        rows.append((t, s["mean"], s["std"], s["sat"], s["edge"], d))
        if kind:
            flags.append((n, t, ",".join(kind), s["thumb"].copy()))
        prev = s; n += 1
    if frozen >= fps * 0.5: frozen_runs += 1
    means = [r[1] for r in rows]; stds = [r[2] for r in rows]; sats = [r[3] for r in rows]; edges = [r[4] for r in rows]
    def avg(a): return sum(a) / max(1, len(a))
    rep = ["film %s: %dx%d %.1f fps, %d frames, %.1f s" % (os.path.basename(film), w, h, fps, n, n / fps),
           "mean luma %.1f  contrast(std) %.1f  saturation %.1f  edge %.2f  p5-p95 %.0f-%.0f" % (
               avg(means), avg(stds), avg(sats), avg(edges), avg([r[1] for r in rows]) and min(x for x in means), max(means)),
           "black frames %d, blown %d, flicker %d, mid-shot jumps %d, duplicate-frame share %.2f, frozen runs>=0.5s %d" % (
               sum(1 for f in flags if "black" in f[2]), sum(1 for f in flags if "blown" in f[2]),
               sum(1 for f in flags if "flicker" in f[2]), sum(1 for f in flags if "jump" in f[2]), dup / max(1, n), frozen_runs)]
    # a dull stretch: 2 s windows whose contrast or saturation is under 60% of the film's own median
    ms = sorted(stds)[len(stds) // 2]; mss = sorted(sats)[len(sats) // 2]
    win = int(fps * 2); dull = 0
    for i in range(0, n - win, win):
        if avg(stds[i:i + win]) < ms * 0.6 or avg(sats[i:i + win]) < mss * 0.6: dull += 1
    rep.append("dull 2 s windows (contrast or saturation < 60%% of median): %d" % dull)
    if ref and os.path.exists(ref):
        rw, rh, rfps, rdur = probe(ref)
        rm = rs = rsat = re_ = 0; rn = 0
        for b in frames(ref, rw, rh):
            s = stats(b, rw, rh); rm += s["mean"]; rs += s["std"]; rsat += s["sat"]; re_ += s["edge"]; rn += 1
        rep.append("reference %s: %d frames, mean luma %.1f  contrast(std) %.1f  saturation %.1f  edge %.2f" % (
            os.path.basename(ref), rn, rm / rn, rs / rn, rsat / rn, re_ / rn))
        rep.append("film vs reference: luma %+.0f%%  contrast %+.0f%%  saturation %+.0f%%  edge(sharpness) %+.0f%%" % (
            (avg(means) / (rm / rn) - 1) * 100, (avg(stds) / (rs / rn) - 1) * 100, (avg(sats) / (rsat / rn) - 1) * 100, (avg(edges) / (re_ / rn) - 1) * 100))
    rep.append("flagged frames: %d" % len(flags))
    for f in flags[:80]: rep.append("  f%05d %7.2fs %s" % (f[0], f[1], f[2]))
    open(os.path.join(out, "scan.txt"), "w").write("\n".join(rep) + "\n")
    with open(os.path.join(out, "frames.csv"), "w") as fh:
        fh.write("t,luma,std,sat,edge,diff\n")
        for r in rows: fh.write("%.3f,%.1f,%.1f,%.1f,%.2f,%.1f\n" % r)
    if flags:
        cols = 6; tw, th = w // 4, h // 4
        sheet = Image.new("RGB", (cols * tw, ((min(len(flags), 48) + cols - 1) // cols) * (th + 14)), (20, 20, 20))
        d = ImageDraw.Draw(sheet)
        for i, f in enumerate(flags[:48]):
            x, y = (i % cols) * tw, (i // cols) * (th + 14)
            sheet.paste(f[3], (x, y + 14)); d.text((x + 2, y + 1), "%.1fs %s" % (f[1], f[2][:28]), fill=(255, 220, 80))
        sheet.save(os.path.join(out, "flagged.jpg"), quality=85)
    print("\n".join(rep))

if __name__ == "__main__":
    main()
