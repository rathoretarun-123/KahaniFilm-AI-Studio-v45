#!/usr/bin/env python3
"""Restore original v42 test fixtures from optional source archives; synthesize only generic plumbing fixtures.
Never synthesize the user-family sheets38/sheets40/sheets40b: those encode real identity references.
Any generated test images/audio are explicitly marked synthetic and are not film assets.
"""
import json, math, struct, wave, zipfile, zlib
from pathlib import Path
R=Path(__file__).resolve().parents[2]
ARCHIVES=[R/"KahaniFilm-v42-source-part3-test-sheets.zip",R/"KahaniFilm-v42-source-part4-test-stories-and-pictures.zip"]
EXPECTED_SHA256={
 "KahaniFilm-v42-source-part3-test-sheets.zip":"c96867fc92e0d354b84fef7723fcc2d481361833e3439df7b9c9e639e6b5cc56",
 "KahaniFilm-v42-source-part4-test-stories-and-pictures.zip":"fe105a3263f0334fb48cb43a64819f40aaf23492c1ee671a27b926168a6f16f9",
}
restored=[]; synthetic=[]; missing=[]
def png(w,h,seed,cols=5,rows=2,scene=False,thing=False):
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
 bg=[(223,235,243),(241,228,216),(225,239,222),(220,200,230),(246,239,211),(218,235,232)]
 hair=[(56,42,35),(65,48,38),(42,39,36),(93,62,43),(51,45,61)]
 cw=w//cols; ch=h//rows
 for n in range(cols*rows):
  x0=(n%cols)*cw+3;x1=min(w,(n%cols+1)*cw-3);y0=(n//cols)*ch+3;y1=min(h,(n//cols+1)*ch-3)
  rect(x0,y0,x1,y1,bg[(seed+n)%len(bg)])
  cx=(x0+x1)//2;cy=y0+ch//4;rr=max(4,min(16,cw//5,ch//5))
  if scene:
   rect(x0,y0+ch*2//3,x1,y1,(119,163,113));circ(x0+cw*4//5,y0+ch//4,max(3,ch//10),(248,207,104))
  elif thing:
   rect(cx-cw//6,cy-ch//8,cx+cw//6,cy+ch//4,(70,82,92))
   rect(cx-cw//8,cy-ch//5,cx+cw//8,cy-ch//8,(105,75,48))
   rect(cx-cw//5,cy+ch//4,cx+cw//5,cy+ch//4+4,(45,48,52))
  else:
   circ(cx,cy-rr//3,rr+2,hair[(seed+n)%len(hair)]);circ(cx,cy,rr,(201,151,124))
   circ(cx-rr//3,cy,2,(30,30,30));circ(cx+rr//3,cy,2,(30,30,30))
   rect(cx-rr//4,cy+rr//2,cx+rr//4,cy+rr//2+2,(105,45,45))
   body_bottom=min(y1-12,cy+rr+ch//3)
   shirt=[(54,118,158),(177,86,72),(74,135,91),(132,96,155)][(seed+n)%4]
   rect(cx-cw//4,cy+rr+2,cx+cw//4,body_bottom,shirt)
   # Explicit arms, legs and feet keep synthetic full-body fixtures from masquerading as head-and-torso crops.
   rect(cx-cw//4-5,cy+rr+ch//10,cx-cw//4+3,body_bottom-10,(201,151,124))
   rect(cx+cw//4-3,cy+rr+ch//10,cx+cw//4+5,body_bottom-10,(201,151,124))
   rect(cx-cw//8,body_bottom,cx-2,y1-10,shirt)
   rect(cx+2,body_bottom,cx+cw//8,y1-10,shirt)
   rect(cx-cw//8,y1-10,cx-2,y1-5,(60,50,42))
   rect(cx+2,y1-10,cx+cw//8,y1-5,(60,50,42))
 def chunk(t,d):return struct.pack(">I",len(d))+t+d+struct.pack(">I",zlib.crc32(t+d)&0xffffffff)
 raw=b"".join(b"\0"+p[y*w*3:(y+1)*w*3] for y in range(h))
 return b"\x89PNG\r\n\x1a\n"+chunk(b"IHDR",struct.pack(">IIBBBBB",w,h,8,2,0,0,0))+chunk(b"IDAT",zlib.compress(raw,6))+chunk(b"IEND",b"")
def put(rel,data,category):
 q=R/rel
 if q.exists() and q.stat().st_size:return
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
 for n,sc in [("camera_photo.jpg",False),("drawn_character_phone_saved.jpg",False),("drawn_character_plain.png",False),("drawn_place_phone_saved.jpg",True),("drawn_thing_phone_saved.jpg",True)]:
  # These named files model one ordinary phone/gallery upload, not a 3x2 contact sheet.
  # Contact-sheet segmentation has its own multi-panel fixtures and tests.
  put("tools/testdata/upload/"+n,png(480,360,201+len(synthetic),1,1,sc),"synthetic")
 # Small synthetic sample assets only if missing; never use these as film references.
 # Generate every filename named by sample/cast.txt so the sample project has a complete, coherent manifest.
 sample_dir=R/"app/src/main/assets/sample"
 sample_refs=[]
 cast_file=sample_dir/"cast.txt"
 if cast_file.is_file():
  for raw in cast_file.read_text(encoding="utf-8").splitlines():
   f=raw.strip().split("|")
   if not f or len(f)<2: continue
   if f[0]=="char" and len(f)>=3: sample_refs.append((f[2],False,False))
   elif f[0]=="view" and len(f)>=4: sample_refs.append((f[3],False,False))
   elif f[0]=="scene" and len(f)>=3: sample_refs.append((f[2],True,False))
   elif f[0]=="shot" and len(f)>=4: sample_refs.append((f[3],True,False))
   elif f[0] in ("title","end"): sample_refs.append((f[1],True,False))
 sample_refs.extend([("char_kripa_pouch.jpg",False,True),("bg_garden.jpg",True,False),("bg_forest.jpg",True,False)])
 seen_sample=set()
 for n,sc,thing in sample_refs:
  if not n or n in seen_sample: continue
  seen_sample.add(n)
  put("app/src/main/assets/sample/"+n,png(480,360,301+len(synthetic),1,1,sc,thing),"synthetic")
 # Frequencies are calibrated against the app analyser: 230 Hz is read as ~260 Hz and overlaps the child range.
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
