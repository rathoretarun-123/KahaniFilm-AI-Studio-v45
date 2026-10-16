import sys, glob, os
from PIL import Image, ImageDraw
src, out, per_row, rows = sys.argv[1], sys.argv[2], int(sys.argv[3]), int(sys.argv[4])
fs = sorted(glob.glob(os.path.join(src, "f*.jpg")))
per = per_row * rows
n = 0
for s in range(0, len(fs), per):
    chunk = fs[s:s+per]
    ims = [Image.open(f) for f in chunk]
    w, h = 480, 270
    sheet = Image.new("RGB", (per_row * w, rows * (h + 14)), (20, 20, 20))
    d = ImageDraw.Draw(sheet)
    for i, (f, im) in enumerate(zip(chunk, ims)):
        x, y = (i % per_row) * w, (i // per_row) * (h + 14)
        sheet.paste(im.resize((w, h)), (x, y + 14))
        d.text((x + 4, y + 1), os.path.basename(f).split("_")[1].replace(".jpg", ""), fill=(255, 220, 0))
    n += 1
    sheet.save(f"{out}_{n:02d}.jpg", quality=82)
print(n, "sheets from", len(fs), "stills")
