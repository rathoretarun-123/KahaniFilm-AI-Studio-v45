#!/usr/bin/env python3
"""Restore original v42 test fixtures from optional source archives; synthesize only generic plumbing fixtures.
Never synthesize the user-family sheets38/sheets40/sheets40b: those encode real identity references.
Any generated test images/audio are explicitly marked synthetic and are not film assets.
"""
import io, json, math, struct, wave, zipfile, zlib
from pathlib import Path
R=Path(__file__).resolve().parents[2]
ARCHIVES=[R/"KahaniFilm-v42-source-part3-test-sheets.zip",R/"KahaniFilm-v42-source-part4-test-stories-and-pictures.zip"]
EXPECTED_SHA256={
 "KahaniFilm-v42-source-part3-test-sheets.zip":"c96867fc92e0d354b84fef7723fcc2d481361833e3439df7b9c9e639e6b5cc56",
 "KahaniFilm-v42-source-part4-test-stories-and-pictures.zip":"fe105a3263f0334fb48cb43a64819f40aaf23492c1ee671a27b926168a6f16f9",
}

def jpeg_bytes(data, camera=False):
 """Convert generated PNG pixels to genuine JPEG; add camera EXIF only to camera_photo.jpg."""
 from PIL import Image
 image = Image.open(io.BytesIO(data)).convert("RGB")
 out = io.BytesIO()
 image.save(out, format="JPEG", quality=95, subsampling=0, optimize=False)
 jpg = out.getvalue()
 if not camera:
  return jpg
 # Minimal valid EXIF APP1/TIFF: camera make plus ExposureTime, FNumber and ISO in the Exif IFD.
 make = b"OpenAI Test Camera\0"
 make_offset = 38
 ifd1_offset = (make_offset + len(make) + 1) & ~1
 ifd1_size = 2 + 12 * 3 + 4
 exposure_offset = ifd1_offset + ifd1_size
 fnumber_offset = exposure_offset + 8
 tiff = bytearray(fnumber_offset + 8)
 tiff[0:8] = b"II*\x00" + struct.pack("<I", 8)
 struct.pack_into("<H", tiff, 8, 2)
 struct.pack_into("<HHII", tiff, 10, 0x010F, 2, len(make), make_offset)
 struct.pack_into("<HHII", tiff, 22, 0x8769, 4, 1, ifd1_offset)
 struct.pack_into("<I", tiff, 34, 0)
 tiff[make_offset:make_offset + len(make)] = make
 struct.pack_into("<H", tiff, ifd1_offset, 3)
 entry = ifd1_offset + 2
 struct.pack_into("<HHII", tiff, entry, 0x829A, 5, 1, exposure_offset)
 struct.pack_into("<HHII", tiff, entry + 12, 0x829D, 5, 1, fnumber_offset)
 struct.pack_into("<HHIHH", tiff, entry + 24, 0x8827, 3, 1, 100, 0)
 struct.pack_into("<I", tiff, entry + 36, 0)
 struct.pack_into("<II", tiff, exposure_offset, 1, 125)
 struct.pack_into("<II", tiff, fnumber_offset, 28, 10)
 payload = b"Exif\x00\x00" + bytes(tiff)
 segment = b"\xff\xe1" + struct.pack(">H", len(payload) + 2) + payload
 if not jpg.startswith(b"\xff\xd8"):
  raise ValueError("Pillow did not encode a JPEG")
 return jpg[:2] + segment + jpg[2:]
restored=[]; synthetic=[]; missing=[]
def png(w,h,seed,cols=5,rows=2,scene=False):
 p=bytearray([248,248,246]*(w*h))
 def rect(x0,y0,x1,y1,c):
  for y in range(max(0,y0),min(h,y1)):
   for x in range(max(0,x0),min(w,x1)):
    i=(y*w+x)*3;p[i:i+3]=bytes(c)
 def circ(cx,cy,r,c):
  for y in range(max(0,cy-r),min(h,cy+r+1)):
   for x in range(max(0,cx-r),min(w,cx+r+1)):
    if (x-cx)**2+(y-cy)**2<=r*r:
     i=(y*w+x)*3;p[i:i+3]=bytes(c)
 # Keep every cell background safely beyond the splitter's 40-point background-distance threshold.
 # Character contact sheets share a clean page background; location panels have distinct scene colours.
 bg=[(180,210,230),(230,190,170),(180,220,180),(200,170,220),(230,220,160),(160,210,210)]
 hair=[(56,42,35),(65,48,38),(42,39,36),(93,62,43),(51,45,61)]
 cw=w//cols; ch=h//rows
 for n in range(cols*rows):
  x0=(n%cols)*cw+3;x1=min(w,(n%cols+1)*cw-3);y0=(n//cols)*ch+3;y1=min(h,(n//cols+1)*ch-3)
  rect(x0,y0,x1,y1,bg[(seed+n)%len(bg)])
  cx=(x0+x1)//2;cy=(y0+y1)//2;rr=max(4,min(45,cw//5,ch//5))
  if scene:
   rect(x0,y0+ch*2//3,x1,y1,(119,163,113));circ(x0+cw*4//5,y0+ch//4,max(3,ch//10),(248,207,104))
  else:
   circ(cx,cy-rr//3,rr+2,hair[(seed+n)%len(hair)]);circ(cx,cy,rr,(201,151,124))
   circ(cx-rr//3,cy,2,(30,30,30));circ(cx+rr//3,cy,2,(30,30,30))
   rect(cx-rr//4,cy+rr//2,cx+rr//4,cy+rr//2+2,(105,45,45))
   rect(cx-cw//4,cy+rr+2,cx+cw//4,min(y1,cy+rr+ch//3),[(54,118,158),(177,86,72),(74,135,91),(132,96,155)][(seed+n)%4])
 def chunk(t,d):return struct.pack(">I",len(d))+t+d+struct.pack(">I",zlib.crc32(t+d)&0xffffffff)
 raw=b"".join(b"\0"+p[y*w*3:(y+1)*w*3] for y in range(h))
 return b"\x89PNG\r\n\x1a\n"+chunk(b"IHDR",struct.pack(">IIBBBBB",w,h,8,2,0,0,0))+chunk(b"IDAT",zlib.compress(raw,6))+chunk(b"IEND",b"")
def put(rel,data,category):
 q=R/rel
 if q.exists() and q.stat().st_size:return
 if q.suffix.lower() in {".jpg", ".jpeg"} and data.startswith(b"\x89PNG\r\n\x1a\n"):
  data = jpeg_bytes(data, camera=(q.name == "camera_photo.jpg"))
 q.parent.mkdir(parents=True,exist_ok=True);q.write_bytes(data)
 (restored if category=="original" else synthetic).append(rel)
def main():
 # Prefer the original source archives; preserve existing files and paths.
 for zpath in ARCHIVES:
  if not zpath.is_file():continue
  import hashlib
  actual=hashlib.sha256(zpath.read_bytes()).hexdigest()
  expected=EXPECTED_SHA256.get(zpath.name)
  if expected and actual != expected:
   raise SystemExit(f"Source fixture archive checksum mismatch for {zpath.name}: {actual}")
  with zipfile.ZipFile(zpath) as z:
   for n in z.namelist():
    marker="/tools/testdata/"
    if marker not in n:continue
    rel=n.split(marker,1)[1]
    # Keep only test fixtures, never copy the archive's app source or overwrite.
    if rel.endswith("/") or ".." in Path(rel).parts:continue
    q=R/"tools/testdata"/rel
    if q.exists() and q.stat().st_size:continue
    q.parent.mkdir(parents=True,exist_ok=True);q.write_bytes(z.read(n));restored.append(str(q.relative_to(R)))
 # Generic 60-sheet crop/decode tests can use synthetic ten-panel sheets.
 for n in range(1,61):
  put(f"tools/testdata/sheets/sheet{n:02d}.jpg",png(1000,400,n,5,2,scene=34<=n<=43),"synthetic")
 # Generic upload-path fixtures only. Do NOT synthesize sheets38/sheets40/sheets40b identity sources.
 # Character imports are individual pictures; place and prop imports are explicit 3x2 contact sheets.
 uploads=[
  ("camera_photo.jpg",False,1,1),
  ("drawn_character_phone_saved.jpg",False,1,1),
  ("drawn_character_plain.png",False,1,1),
  ("drawn_place_phone_saved.jpg",True,3,2),
  ("drawn_thing_phone_saved.jpg",False,3,2),
 ]
 for n,sc,cols,rows in uploads:
  put("tools/testdata/upload/"+n,png(480,360,201+len(synthetic),cols,rows,sc),"synthetic")
 # Build every missing image named by the built-in sample manifest for JVM tests only.
 # These generic test fixtures are never substitutes for either film's approved references.
 sample_dir=R/"app/src/main/assets/sample"
 refs=[]
 for raw in (sample_dir/"cast.txt").read_text(encoding="utf-8").splitlines():
  f=raw.strip().split("|")
  if not f or f[0].startswith("#"): continue
  name=None; w=480; h=1200; scene=False
  if f[0]=="char" and len(f)>=3: name=f[2]
  elif f[0]=="view" and len(f)>=4: name=f[3]
  elif f[0]=="pose" and len(f)>=3: name=f[2]
  elif f[0]=="scene" and len(f)>=3: name=f[2]; w=640; h=360; scene=True
  elif f[0]=="shot" and len(f)>=4: name=f[3]; w=640; h=360; scene=True
  elif f[0] in ("title","end") and len(f)>=2: name=f[1]; w=640; h=360; scene=True
  if name and Path(name).suffix.lower() in (".jpg",".jpeg",".png"): refs.append((name,w,h,scene))
 refs.extend([("char_kripa_pouch.jpg",480,1200,False),("bg_forest.jpg",640,360,True)])
 for name,w,h,scene in refs:
  put("app/src/main/assets/sample/"+name,png(w,h,301+len(synthetic),1,1,scene),"synthetic")
 # Low-amplitude sine fixtures are test-only; they are measured after staging.
 for n,f in [("man.wav",170),("deep_slow.wav",125),("woman.wav",180),("child.wav",310)]:
  q=R/"voices"/n
  if not q.exists() or not q.stat().st_size:
   q.parent.mkdir(parents=True,exist_ok=True)
   with wave.open(str(q),"wb") as a:
    a.setnchannels(1);a.setsampwidth(2);a.setframerate(16000)
    a.writeframes(b"".join(struct.pack("<h",int(700*math.sin(2*math.pi*f*k/16000))) for k in range(16000)))
   synthetic.append("voices/"+n)
 absent=[p for p in ["tools/testdata/sheets38","tools/testdata/sheets40","tools/testdata/sheets40b"] if not (R/p).is_dir() or not any((R/p).iterdir())]
 manifest={"classification":"TEST FIXTURE INVENTORY — synthetic files are not film references",
  "original_archives_found":[p.name for p in ARCHIVES if p.is_file()],
  "original_files_restored":restored,"synthetic_test_only_files":synthetic,
  "identity_fixture_directories_missing":absent,
  "warning":"Synthetic sample/upload/audio assets must not be treated as film references. The v38/v40 family identity tests require the original source archives."}
 (R/"tools/testdata/fixture_inventory.json").write_text(json.dumps(manifest,indent=2)+"\n",encoding="utf-8")
 print(f"Restored {len(restored)} original files; created {len(synthetic)} explicitly synthetic test files.")
 if absent: print("BLOCKED: original identity fixtures missing: "+", ".join(absent))
if __name__=="__main__":main()
