#!/usr/bin/env python3
"""Create controlled synthetic fixtures by reading test filenames, not private photo archives."""
import json,math,re,struct,wave
from pathlib import Path
from synthetic_art import image
R=Path(__file__).resolve().parents[2]
made=[]
def put(rel,data):
 p=R/rel;p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(data);made.append(rel)
def main():
 tests=(R/"tools/robotest/src/test/java/com/tarun/kahani/AppTest.java").read_text(encoding="utf-8")
 bydir={"sheets":set(),"sheets38":set(),"sheets40":set(),"sheets40b":set()}
 active="sheets"
 for line in tests.splitlines():
  for d in ("sheets38","sheets40","sheets40b"):
   if "tools/testdata/"+d in line:active=d
  if "@Test" in line or "public void " in line:active="sheets"
  for n in re.findall(r'"([0-9]{2}_[A-Za-z0-9_]+\\.(?:jpg|png|jpeg))"',line):
   if active in bydir:bydir[active].add(n)
 for n in range(1,61):put(f"tools/testdata/sheets/sheet{n:02d}.jpg",image(1000,400,5,2,n,"place" if 34<=n<=43 else "person"))
 for d,names in bydir.items():
  for i,name in enumerate(sorted(names)):
   low=name.lower()
   kind="place" if any(k in low for k in ("place","scene","park","maidan")) else "object" if any(k in low for k in ("swing","slide","thing","kulhad","rod","props")) else "person"
   style="kitchen" if "kitchen" in low else "room" if "room" in low else "park" if kind=="place" else "swing-seat" if "swing" in low else "slide" if "slide" in low else "cup" if "kulhad" in low else "thing"
   put(f"tools/testdata/{d}/{name}",image(900,600,3,2,1000+i,kind,style,open_mouth="laugh" in low))
 uploads=[("camera_photo.jpg",image(480,640,1,1,411)),("drawn_character_phone_saved.jpg",image(480,640,1,1,412)),
 ("drawn_character_plain.png",image(480,640,1,1,413,open_mouth=True)),("drawn_place_phone_saved.jpg",image(640,420,1,1,414,"place")),
 ("drawn_thing_phone_saved.jpg",image(480,360,1,1,415,"object","cup"))]
 for n,d in uploads:put("tools/testdata/upload/"+n,d)
 old=(R/"tools/scripts/prepare_legacy_test_fixtures.py").read_text(encoding="utf-8")
 samples=re.findall(r'"((?:char|bg)_[A-Za-z0-9_]+\\.jpg)"',old)
 for i,n in enumerate(dict.fromkeys(samples)):
  back="_back" in n;kind="place" if n.startswith("bg_") else "object" if "pouch" in n else "person"
  put("app/src/main/assets/sample/"+n,image(480,1200,1,1,2000+i,kind,"park" if "garden" in n else "outdoor",back=back,open_mouth=(i==1)))
 for n,f0 in [("man.wav",132),("deep_slow.wav",105)]:
  p=R/"voices"/n;p.parent.mkdir(parents=True,exist_ok=True);frames=[]
  for k in range(32000):
   t=k/16000;u=t%0.28;gate=max(0.0,min(1.0,u/.025,(.25-u)/.03))
   v=.52*math.sin(2*math.pi*f0*t)+.24*math.sin(4*math.pi*f0*t)+.12*math.sin(6*math.pi*f0*t)
   frames.append(struct.pack("<h",int(4500*gate*v)))
  with wave.open(str(p),"wb") as w:w.setnchannels(1);w.setsampwidth(2);w.setframerate(16000);w.writeframes(b"".join(frames))
  made.append("voices/"+n)
 required=["tools/testdata/sheets/sheet01.jpg","tools/testdata/sheets/sheet60.jpg","tools/testdata/upload/drawn_character_phone_saved.jpg","voices/man.wav","voices/deep_slow.wav"]
 missing=[x for x in required if not (R/x).is_file() or not (R/x).stat().st_size]
 if missing:raise SystemExit("Missing synthetic fixtures: "+", ".join(missing))
 (R/"tools/testdata/fixture_inventory.json").write_text(json.dumps({"classification":"synthetic test fixtures only","privacy":"No personal photo archives are copied into CI.","created":made,"verified":required},indent=2)+"\n",encoding="utf-8")
 print(f"Created {len(made)} deterministic synthetic test fixtures; verified {len(required)} required files.")
if __name__=="__main__":main()
