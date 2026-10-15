#!/usr/bin/env python3
"""Restore original v42 test fixtures from optional source archives; synthesize only generic plumbing fixtures.
Never synthesize the user-family sheets38/sheets40/sheets40b: those encode real identity references.
Any generated test images/audio are explicitly marked synthetic and are not film assets.
"""
import io, json, math, struct, wave, zipfile, zlib
from pathlib import Path
from PIL import Image, ImageDraw, TiffImagePlugin
R=Path(__file__).resolve().parents[2]
ARCHIVES=[R/"KahaniFilm-v42-source-part3-test-sheets.zip",R/"KahaniFilm-v42-source-part4-test-stories-and-pictures.zip"]
EXPECTED_SHA256={
 "KahaniFilm-v42-source-part3-test-sheets.zip":"c96867fc92e0d354b84fef7723fcc2d481361833e3439df7b9c9e639e6b5cc56",
 "KahaniFilm-v42-source-part4-test-stories-and-pictures.zip":"fe105a3263f0334fb48cb43a64819f40aaf23492c1ee671a27b926168a6f16f9",
}
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
 bg=[(223,235,243),(241,228,216),(225,239,222),(239,226,239),(246,239,211),(218,235,232)]
 hair=[(56,42,35),(65,48,38),(42,39,36),(93,62,43),(51,45,61)]
 cw=w//cols; ch=h//rows
 for n in range(cols*rows):
  x0=(n%cols)*cw+3;x1=min(w,(n%cols+1)*cw-3);y0=(n//cols)*ch+3;y1=min(h,(n//cols+1)*ch-3)
  rect(x0,y0,x1,y1,bg[(seed+n)%len(bg)])
  cx=(x0+x1)//2;cy=(y0+y1)//2;rr=max(4,min(16,cw//5,ch//5))
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
def jpeg_fixture(seed, scene=False, phone=False, camera=False, back=False, open_mouth=False, turban=False):
 # Deterministic, clearly synthetic drawings for generic regression tests only.
 # The three historical family identity directories remain intentionally untouched.
 im=Image.new("RGB",(480,360),(238,240,236) if not scene else (205,224,237))
 d=ImageDraw.Draw(im)
 if scene:
  d.rectangle((0,245,480,360),fill=(105,151,98))
  d.ellipse((340,28,430,118),fill=(247,211,115))
  for x,y in [(70,170),(125,195),(390,190),(430,220)]:
   d.rectangle((x,y,x+9,285),fill=(93,125,75))
   d.ellipse((x-23,y-30,x+30,y+24),fill=(62,139,77))
 else:
  # Full-body silhouette gives Cutout/PoseSense meaningful pixels.
  skin=(204,151,122); hair=(48+(seed%3)*7,38,32); cloth=[(54,118,158),(177,86,72),(74,135,91),(132,96,155)][seed%4]
  # legs and feet
  d.line((212,265,202,333),fill=(73,66,60),width=23)
  d.line((267,265,280,333),fill=(73,66,60),width=23)
  d.ellipse((180,321,218,345),fill=(57,48,43)); d.ellipse((265,321,304,345),fill=(57,48,43))
  # arms behind torso
  d.line((185,175,148,238),fill=skin,width=24); d.line((295,175,330,235),fill=skin,width=24)
  # torso and neck
  d.polygon([(202,151),(278,151),(310,270),(170,270)],fill=cloth,outline=(58,54,51))
  d.rectangle((225,137,255,168),fill=skin)
  # hair/head silhouette
  d.ellipse((180,38,300,160),fill=hair)
  if back:
   # Back view: hair covers the head; no eyes, nose or mouth are drawn.
   d.ellipse((192,47,288,151),fill=hair)
   d.line((240,137,240,164),fill=hair,width=12)
  else:
   d.ellipse((190,48,290,151),fill=skin)
   # ears
   d.ellipse((180,83,199,112),fill=skin); d.ellipse((281,83,300,112),fill=skin)
   # fringe/hair line
   d.arc((187,37,293,104),180,355,fill=hair,width=16)
   # eyes and brows
   d.line((205,78,226,73),fill=(45,35,29),width=5); d.line((254,73,275,78),fill=(45,35,29),width=5)
   d.ellipse((208,82,222,96),fill=(25,22,20)); d.ellipse((258,82,272,96),fill=(25,22,20))
   d.ellipse((212,85,217,90),fill=(250,250,245)); d.ellipse((262,85,267,90),fill=(250,250,245))
   d.line((240,92,232,116,244,118),fill=(144,92,69),width=4)
   if open_mouth:
    d.ellipse((221,119,260,145),fill=(75,20,25),outline=(118,43,43),width=3)
    d.rectangle((228,121,253,128),fill=(255,251,232))
   else:
    d.arc((220,119,260,140),5,170,fill=(130,44,45),width=5)
   if turban:
    d.ellipse((176,22,304,76),fill=(28,63,118),outline=(218,174,74),width=4)
    d.arc((185,27,295,70),190,350,fill=(202,54,54),width=12)
    d.ellipse((232,22,248,39),fill=(229,186,74))
    d.arc((208,103,272,137),5,175,fill=(38,30,27),width=10) # moustache
   else:
    d.arc((208,105,272,139),10,170,fill=(72,42,32),width=5)
  # identity-neutral clothing accents; no resemblance to any real person.
  d.line((202,174,278,174),fill=(235,202,121),width=5)
 exif=Image.Exif()
 if phone:
  exif[271]="Apple"; exif[272]="iPhone 15"
 if camera:
  exif[33434]=TiffImagePlugin.IFDRational(1,120)
  exif[33437]=TiffImagePlugin.IFDRational(18,10)
  exif[34855]=100
  exif[37386]=TiffImagePlugin.IFDRational(4,1)
 out=io.BytesIO()
 im.save(out,format="JPEG",quality=92,exif=exif)
 return out.getvalue()
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
  seed=201+len(synthetic)
  if n.endswith(".png"):
   data=png(480,360,seed,1,1,sc)
  else:
   data=jpeg_fixture(seed,sc,phone=n!="camera_photo.jpg",camera=n=="camera_photo.jpg")
  put("tools/testdata/upload/"+n,data,"synthetic")
 # Small synthetic sample/audio files only if missing; test-only placeholders.
 for n,sc,back,mouth,hat in [
  ("char_vrinda.jpg",False,False,False,False),("char_vrinda_back.jpg",False,True,False,False),
  ("char_vanusha.jpg",False,False,True,False),("char_vanusha_back.jpg",False,True,False,False),
  ("char_raju.jpg",False,False,False,False),("char_raju_back.jpg",False,True,False,False),
  ("char_kripa.jpg",False,False,False,False),("char_kripa_back.jpg",False,True,False,False),
  ("char_kripa_pouch.jpg",False,False,False,False),("char_ratanlal.jpg",False,False,False,True),
  ("bg_garden.jpg",True,False,False,False),("bg_forest.jpg",True,False,False,False)]:
  put("app/src/main/assets/sample/"+n,jpeg_fixture(301+len(synthetic),sc,back=back,open_mouth=mouth,turban=hat),"synthetic")
 for n,f in [("man.wav",170),("deep_slow.wav",125),("woman.wav",260),("child.wav",400)]:
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
