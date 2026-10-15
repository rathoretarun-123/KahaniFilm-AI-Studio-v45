#!/usr/bin/env python3
"""Restore original v42 test fixtures from optional source archives; synthesize only generic plumbing fixtures.
Never synthesize the user-family sheets38/sheets40/sheets40b: those encode real identity references.
Any generated test images/audio are explicitly marked synthetic and are not film assets.
"""
import io, json, math, re, struct, wave, zipfile, zlib
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
def sheet_fixture(seed):
 # Deterministic ten-panel contact sheet for crop/pose regression only.
 # Never used as a film reference or as a substitute for missing identity packs.
 im=Image.new("RGB",(1000,500),(250,250,248))
 d=ImageDraw.Draw(im)
 backgrounds=[(236,242,247),(243,237,228),(231,243,232),(243,232,241),(246,242,224),
              (225,240,240),(241,235,225),(232,239,225),(238,232,246),(244,238,231)]
 for i in range(10):
  col=i%5; row=i//5; x=col*200; y=row*250
  d.rectangle((x+2,y+2,x+197,y+247),fill=backgrounds[(seed+i)%len(backgrounds)])
  # Different neutral clothing colours; no resemblance to any real person.
  skin=(196,143,112); hair=(45+(seed+i)%4*7,37,32)
  cloth=[(52,111,155),(173,79,68),(72,128,86),(123,88,151),(177,133,54)][(seed+i)%5]
  pose="stand"; side=False; animal=(seed==1)
  if seed==2 and i==1: pose="stand"; side=True
  if seed==4 and i==8: pose="sit"
  if seed==9 and i==2: pose="lie"
  if seed==1 and i==5: pose="run"; side=True
  if seed==15 and i==9: pose="sit"
  if seed==15 and i==4: pose="wave"
  if seed==12 and i==9: pose="jump"
  cx=x+100; cy=y+125
  if pose=="lie":
   # horizontal sleeping animal/person, long connected silhouette
   d.ellipse((cx-65,cy-19,cx-28,cy+18),fill=hair)
   d.polygon([(cx-36,cy-15),(cx+34,cy-18),(cx+60,cy+8),(cx-25,cy+15)],fill=cloth)
   d.line((cx+20,cy+4,cx+62,cy+28),fill=skin,width=11)
   d.line((cx-8,cy+4,cx+28,cy+27),fill=skin,width=11)
   continue
  if pose=="sit":
   d.ellipse((cx-19,cy-88,cx+19,cy-50),fill=hair)
   d.ellipse((cx-16,cy-84,cx+16,cy-54),fill=skin)
   d.line((cx,cy-52,cx,cy-5),fill=skin,width=12)
   d.polygon([(cx-20,cy-52),(cx+20,cy-52),(cx+28,cy-5),(cx-28,cy-5)],fill=cloth)
   d.line((cx-16,cy-6,cx-43,cy+21,cx-6,cy+21),fill=cloth,width=16)
   d.line((cx+14,cy-6,cx+43,cy+20,cx+61,cy+20),fill=cloth,width=16)
   d.line((cx-13,cy-42,cx-37,cy-17),fill=skin,width=9)
   d.line((cx+13,cy-42,cx+35,cy-17),fill=skin,width=9)
   continue
  if pose=="jump":
   # full-body leaping figure; sword and foot deliberately cross nominal panel boundaries.
   d.ellipse((cx-18,cy-93,cx+18,cy-57),fill=hair)
   d.ellipse((cx-15,cy-89,cx+15,cy-59),fill=skin)
   d.polygon([(cx-18,cy-55),(cx+18,cy-55),(cx+29,cy-4),(cx-24,cy-4)],fill=cloth)
   d.line((cx-12,cy-45,cx-35,cy-18),fill=skin,width=10)
   d.line((cx+12,cy-45,cx+5,cy-5),fill=skin,width=10)
   d.line((cx-10,cy-4,cx-31,cy+45),fill=cloth,width=13)
   d.line((cx+12,cy-4,cx+42,cy+48),fill=cloth,width=13)
   d.ellipse((cx+26,cy+39,cx+64,cy+57),fill=(57,48,43))
   d.line((cx-30,cy-18,cx+40,cy-228),fill=(83,60,37),width=7)
   d.line((cx-35,cy-14,cx+45,cy-238),fill=(210,215,222),width=5)
   d.line((cx-36,cy-14,cx-47,cy-1),fill=(124,77,44),width=9)
   continue
  # standing, side, waving, or running: head/torso/arms/legs share connected joints
  headx=cx+(-12 if side else 0)
  d.ellipse((headx-20,cy-99,headx+20,cy-59),fill=hair)
  d.ellipse((headx-16,cy-94,headx+16,cy-62),fill=skin)
  if not side:
   d.ellipse((headx-10,cy-82,headx-5,cy-77),fill=(35,28,24))
   d.ellipse((headx+5,cy-82,headx+10,cy-77),fill=(35,28,24))
   d.arc((headx-9,cy-77,headx+9,cy-67),5,170,fill=(120,42,42),width=2)
  else:
   d.ellipse((headx+7,cy-82,headx+12,cy-77),fill=(35,28,24))
   d.polygon([(headx+13,cy-79),(headx+24,cy-74),(headx+13,cy-70)],fill=skin)
  d.rectangle((cx-8,cy-60,cx+8,cy-45),fill=skin)
  d.polygon([(cx-22,cy-48),(cx+22,cy-48),(cx+27,cy+10),(cx-27,cy+10)],fill=cloth)
  if pose=="wave":
   d.line((cx-15,cy-39,cx-35,cy-15),fill=skin,width=9)
   d.line((cx+15,cy-39,cx+42,cy-72),fill=skin,width=9)
   d.ellipse((cx+36,cy-79,cx+49,cy-65),fill=skin)
  elif pose=="run":
   d.line((cx-15,cy-37,cx-43,cy-55),fill=skin,width=9)
   d.line((cx+15,cy-37,cx+40,cy-13),fill=skin,width=9)
  else:
   d.line((cx-15,cy-38,cx-34,cy-4),fill=skin,width=9)
   d.line((cx+15,cy-38,cx+34,cy-4),fill=skin,width=9)
  if pose=="run":
   d.line((cx-10,cy+8,cx-37,cy+39),fill=cloth,width=12)
   d.line((cx+10,cy+8,cx+42,cy+26),fill=cloth,width=12)
  else:
   d.line((cx-10,cy+8,cx-15,cy+68),fill=cloth,width=13)
   d.line((cx+10,cy+8,cx+15,cy+68),fill=cloth,width=13)
  d.ellipse((cx-30,cy+60,cx-5,cy+73),fill=(57,48,43))
  d.ellipse((cx+5,cy+60,cx+30,cy+73),fill=(57,48,43))
 out=io.BytesIO(); im.save(out,format="JPEG",quality=92)
 return out.getvalue()
def encode_fixture2(im, path, camera=False, phone=False):
 exif=Image.Exif()
 if phone: exif[271]="Apple"; exif[272]="iPhone 15"; exif[270]="Synthetic phone-saved drawing; not a camera original"
 if camera:
  exif[271]="Synthetic Camera"; exif[272]="KahaniFilm Test Camera"; exif[306]="2026:10:15 10:00:00"
  exif[34665]={33434:(1,120),33437:(18,10),34855:100,37386:(4,1),36867:"2026:10:15 10:00:00",36868:"2026:10:15 10:00:00"}
 out=io.BytesIO()
 if str(path).lower().endswith(".png"): im.save(out,format="PNG")
 else: im.save(out,format="JPEG",quality=92,exif=exif)
 return out.getvalue()
def portrait_fixture2(seed,back=False,animal=False,open_mouth=False,turban=False):
 w,h=1024,2048; im=Image.new("RGB",(w,h),(248,248,246)); d=ImageDraw.Draw(im); cx=w//2
 cloth=[(54,118,158),(177,86,72),(74,135,91),(132,96,155),(177,133,54)][seed%5]; skin=(198,145,116); hair=(47+(seed%4)*7,37,31)
 if animal:
  d.ellipse((w*.18,h*.38,w*.72,h*.62),fill=cloth,outline=(55,48,42),width=10); d.ellipse((w*.62,h*.32,w*.84,h*.51),fill=hair); d.ellipse((w*.76,h*.37,w*.79,h*.40),fill=(20,20,20))
  d.polygon([(w*.64,h*.34),(w*.67,h*.24),(w*.71,h*.34)],fill=hair)
  for x in (w*.28,w*.52,w*.67,w*.74): d.line((x,h*.58,x,h*.82),fill=cloth,width=25)
  d.arc((w*.12,h*.36,w*.35,h*.60),190,350,fill=hair,width=12)
 else:
  d.ellipse((cx-w*.14,h*.075,cx+w*.14,h*.235),fill=hair)
  if turban: d.ellipse((cx-w*.16,h*.065,cx+w*.16,h*.135),fill=(28,63,118),outline=(218,174,74),width=6)
  if back: d.ellipse((cx-w*.12,h*.09,cx+w*.12,h*.225),fill=hair); d.polygon([(cx-w*.12,h*.19),(cx+w*.12,h*.19),(cx+w*.17,h*.31),(cx-w*.17,h*.31)],fill=hair)
  else:
   d.ellipse((cx-w*.12,h*.09,cx+w*.12,h*.225),fill=skin); d.ellipse((cx-w*.15,h*.14,cx-w*.115,h*.17),fill=skin); d.ellipse((cx+w*.115,h*.14,cx+w*.15,h*.17),fill=skin)
   d.ellipse((cx-w*.065,h*.153,cx-w*.035,h*.168),fill=(20,20,20)); d.ellipse((cx+w*.035,h*.153,cx+w*.065,h*.168),fill=(20,20,20))
   if open_mouth: d.ellipse((cx-w*.055,h*.197,cx+w*.055,h*.222),fill=(75,20,25),outline=(118,43,43),width=4)
   else: d.arc((cx-w*.05,h*.195,cx+w*.05,h*.218),5,170,fill=(130,44,45),width=5)
  d.rectangle((cx-w*.035,h*.215,cx+w*.035,h*.265),fill=skin); d.polygon([(cx-w*.12,h*.25),(cx+w*.12,h*.25),(cx+w*.20,h*.62),(cx-w*.20,h*.62)],fill=cloth,outline=(58,54,51))
  d.line((cx-w*.12,h*.285,cx-w*.25,h*.52),fill=skin,width=28); d.line((cx+w*.12,h*.285,cx+w*.25,h*.52),fill=skin,width=28)
  d.line((cx-w*.08,h*.60,cx-w*.12,h*.86),fill=(67,62,57),width=30); d.line((cx+w*.08,h*.60,cx+w*.12,h*.86),fill=(67,62,57),width=30)
  d.ellipse((cx-w*.18,h*.84,cx-w*.04,h*.89),fill=(52,45,40)); d.ellipse((cx+w*.04,h*.84,cx+w*.18,h*.89),fill=(52,45,40))
 return encode_fixture2(im,"fixture.jpg")
def background_fixture2(seed,name):
 w,h=1280,720; im=Image.new("RGB",(w,h),(183,213,229)); d=ImageDraw.Draw(im); low=name.lower()
 if "cave" in low:
  d.rectangle((0,0,w,h),fill=(35,39,48)); d.polygon([(0,0),(w*.25,0),(w*.42,h*.6),(w*.3,h),(0,h)],fill=(66,66,72)); d.polygon([(w,0),(w*.75,0),(w*.58,h*.6),(w*.7,h),(w,h)],fill=(58,57,66)); d.ellipse((w*.34,h*.14,w*.68,h*.78),fill=(19,24,31)); d.rectangle((0,h*.82,w,h),fill=(73,67,60))
 elif "forest" in low:
  d.rectangle((0,h*.65,w,h),fill=(55,102,62))
  for x in range(30,w,150): d.rectangle((x,h*.28,x+20,h*.8),fill=(87,72,52)); d.ellipse((x-65,h*.10,x+90,h*.48),fill=(39+(x%30),111,61))
 elif "palace" in low or "gate" in low:
  d.rectangle((0,h*.68,w,h),fill=(156,145,112)); d.rectangle((w*.18,h*.22,w*.82,h*.7),fill=(207,185,147),outline=(112,89,64),width=8); d.rectangle((w*.42,h*.38,w*.58,h*.7),fill=(104,71,49)); d.polygon([(w*.14,h*.22),(w*.5,h*.04),(w*.86,h*.22)],fill=(157,57,51))
 else:
  d.rectangle((0,h*.66,w,h),fill=(104,151,92)); d.ellipse((w*.74,h*.06,w*.87,h*.24),fill=(248,211,119)); d.polygon([(0,h*.62),(w*.22,h*.45),(w*.4,h*.62),(w*.65,h*.42),(w,h*.61),(w,h*.75),(0,h*.75)],fill=(117,166,108))
  for x,y in [(w*.12,h*.35),(w*.82,h*.39),(w*.94,h*.45)]: d.rectangle((x,y,x+22,h*.72),fill=(99,82,55)); d.ellipse((x-70,y-80,x+100,y+100),fill=(56,128,69))
 return encode_fixture2(im,"background.jpg")
def prop_fixture2(seed,name):
 im=Image.new("RGB",(800,800),(248,248,246)); d=ImageDraw.Draw(im); low=name.lower(); w=h=800
 if any(k in low for k in ("pouch","bag")): d.polygon([(240,190),(560,190),(620,560),(180,560)],fill=(124,79,47),outline=(58,44,31)); d.arc((290,80,510,320),180,360,fill=(76,51,35),width=22)
 elif any(k in low for k in ("letter","paper")): d.polygon([(160,160),(640,145),(625,625),(175,640)],fill=(245,233,197),outline=(100,80,56))
 elif "swing" in low: d.line((200,100,200,650),fill=(90,66,43),width=18); d.line((600,100,600,650),fill=(90,66,43),width=18); d.line((160,150,640,150),fill=(90,66,43),width=18); d.line((330,150,330,500),fill=(65,65,65),width=5); d.line((470,150,470,500),fill=(65,65,65),width=5); d.rectangle((260,480,540,535),fill=(177,85,54))
 elif "slide" in low: d.line((200,160,200,650),fill=(95,91,82),width=16); d.line((600,160,600,650),fill=(95,91,82),width=16); d.polygon([(200,180),(570,560),(630,600),(270,230)],fill=(210,83,55))
 else: d.ellipse((200,200,600,600),fill=(150,99,56),outline=(65,46,30),width=10)
 return encode_fixture2(im,"prop.jpg")
def sheet_fixture2(seed,panels=10,scene=False,kind="character"):
 w,h=1000,500; cols=5 if panels==10 else 3; rows=(panels+cols-1)//cols; cw=w//cols; ch=h//rows; im=Image.new("RGB",(w,h),(248,248,246)); d=ImageDraw.Draw(im)
 for i in range(panels):
  x=(i%cols)*cw; y=(i//cols)*ch; x0=x+5; y0=y+5; x1=x+cw-5; y1=y+ch-5
  if scene or kind=="scene":
   d.rectangle((x0,y0,x1,y1),fill=(180+(i%3)*8,212,231)); horizon=y0+int(ch*.63); d.rectangle((x0,horizon,x1,y1),fill=(105+(i%3)*12,151,98)); d.ellipse((x0+cw*.68,y0+ch*.08,x0+cw*.83,y0+ch*.28),fill=(248,210,116))
   hx=x0+int(cw*.22); hy=y0+int(ch*.34); d.rectangle((hx,hy,hx+int(cw*.22),horizon),fill=(202,180,142),outline=(103,85,61),width=2); d.polygon([(hx-5,hy),(hx+int(cw*.11),hy-int(ch*.16)),(hx+int(cw*.23),hy)],fill=(146,67,56))
  elif kind=="prop":
   cx=x0+cw//2; cy=y0+ch//2; d.ellipse((cx-cw*.2,cy-ch*.18,cx+cw*.2,cy+ch*.18),fill=(140,93,55),outline=(60,45,32),width=3); d.rectangle((cx-cw*.16,cy-ch*.12,cx+cw*.16,cy+ch*.12),fill=(176,127,70))
  else:
   cx=x0+cw//2; cy=y0+ch//2; cloth=[(52,111,155),(173,79,68),(72,128,86),(123,88,151),(177,133,54)][(seed+i)%5]; skin=(198,145,116); hair=(45+(seed+i)%4*7,37,31)
   if seed==1:
    d.ellipse((cx-30,cy-5,cx+28,cy+25),fill=cloth); d.ellipse((cx+12,cy-28,cx+45,cy+4),fill=hair); d.arc((cx-65,cy-5,cx-10,cy+48),180,330,fill=hair,width=7)
    for dx in (-18,10): d.line((cx+dx,cy+15,cx+dx-4,cy+55),fill=cloth,width=9)
    continue
   pose="lie" if seed==9 and i==2 else "sit" if (seed==4 and i==8) or (seed==15 and i==9) else "wave" if seed==15 and i==4 else "jump" if seed==12 and i==9 else "stand"
   if pose=="lie":
    d.ellipse((cx-cw*.30,cy-ch*.07,cx+cw*.22,cy+ch*.07),fill=cloth); d.ellipse((cx-cw*.39,cy-ch*.10,cx-cw*.20,cy+ch*.08),fill=cloth); d.line((cx+cw*.12,cy,cx+cw*.35,cy+ch*.1),fill=cloth,width=10); d.line((cx-cw*.03,cy,cx+cw*.16,cy+ch*.1),fill=cloth,width=10); continue
   if pose=="sit":
    d.ellipse((cx-20,cy-90,cx+20,cy-50),fill=hair); d.ellipse((cx-16,cy-84,cx+16,cy-54),fill=skin); d.polygon([(cx-22,cy-50),(cx+22,cy-50),(cx+28,cy-2),(cx-28,cy-2)],fill=cloth); d.line((cx-16,cy-4,cx-42,cy+20,cx-6,cy+20),fill=cloth,width=15); d.line((cx+14,cy-4,cx+43,cy+20,cx+61,cy+20),fill=cloth,width=15); continue
   d.ellipse((cx-20,cy-100,cx+20,cy-60),fill=hair); d.ellipse((cx-16,cy-94,cx+16,cy-62),fill=skin); d.ellipse((cx-10,cy-82,cx-5,cy-77),fill=(35,28,24)); d.ellipse((cx+5,cy-82,cx+10,cy-77),fill=(35,28,24))
   d.rectangle((cx-8,cy-60,cx+8,cy-45),fill=skin); d.polygon([(cx-22,cy-48),(cx+22,cy-48),(cx+27,cy+10),(cx-27,cy+10)],fill=cloth); d.line((cx-15,cy-38,cx-34,cy-4),fill=skin,width=9); d.line((cx+15,cy-38,cx+34,cy-4),fill=skin,width=9); d.line((cx-10,cy+8,cx-15,cy+68),fill=cloth,width=13); d.line((cx+10,cy+8,cx+15,cy+68),fill=cloth,width=13); d.ellipse((cx-30,cy+60,cx-5,cy+73),fill=(57,48,43)); d.ellipse((cx+5,cy+60,cx+30,cy+73),fill=(57,48,43))
   if pose=="jump": d.polygon([(cx+15,cy-17),(cx+38,cy-20),(cx+65,y-ch*.41),(cx+48,y-ch*.41)],fill=(210,215,222)); d.line((cx+15,cy-18,cx+1,cy-7),fill=(124,77,44),width=8)
 if scene or kind=="scene":
  for col in range(1,cols): d.rectangle((col*cw-3,0,col*cw+3,h),fill=(250,250,248))
  for row in range(1,rows): d.rectangle((0,row*ch-3,w,row*ch+3),fill=(250,250,248))
 return encode_fixture2(im,"sheet.jpg")
def rebuild_synthetic_fixtures():
 syn=set(synthetic)
 def save(rel,data):
  q=R/rel
  if rel in syn or not q.exists() or not q.stat().st_size:
   q.parent.mkdir(parents=True,exist_ok=True); q.write_bytes(data)
   if rel not in syn and rel not in restored: synthetic.append(rel); syn.add(rel)
 for n in range(1,61):
  rel=f"tools/testdata/sheets/sheet{n:02d}.jpg"
  if rel in syn: save(rel,sheet_fixture2(n,10,34<=n<=43,"scene" if 34<=n<=43 else "character"))
 for n,scene in [("camera_photo.jpg",False),("drawn_character_phone_saved.jpg",False),("drawn_character_plain.png",False),("drawn_place_phone_saved.jpg",True),("drawn_thing_phone_saved.jpg",False)]:
  rel="tools/testdata/upload/"+n
  if rel in syn or not (R/rel).exists():
   im=Image.new("RGB",(480,360),(205,224,237) if scene else (238,240,236)); d=ImageDraw.Draw(im)
   if scene: d.rectangle((0,245,480,360),fill=(105,151,98)); d.ellipse((340,28,430,118),fill=(247,211,115))
   else: d.ellipse((180,38,300,160),fill=(198,145,116)); d.polygon([(202,151),(278,151),(310,270),(170,270)],fill=(54,118,158)); d.line((212,265,202,333),fill=(73,66,60),width=23); d.line((267,265,280,333),fill=(73,66,60),width=23)
   save(rel,encode_fixture2(im,rel,camera=n=="camera_photo.jpg",phone="phone" in n))
 test_java=R/"tools/robotest/src/test/java/com/tarun/kahani/AppTest.java"
 names=set(re.findall(r'"(\d{2}_[A-Za-z0-9_-]+\.(?:png|jpg|jpeg|webp))"',test_java.read_text(encoding="utf-8"))) if test_java.is_file() else set()
 for n in sorted(names):
  low=n.lower(); kind="scene" if any(k in low for k in ("place","park","livingroom","room","wall","garden","background","gate")) else "prop" if any(k in low for k in ("thing","swing","slide","letter","kulhad","pouch","beard","rod")) else "character"
  panels=6 if any(k in low for k in ("6views","6_views","turnaround","sheet","beard","poses")) else 1; seed=1000+len(synthetic)
  data=sheet_fixture2(seed,panels,kind=="scene",kind) if panels>1 else background_fixture2(seed,n) if kind=="scene" else prop_fixture2(seed,n) if kind=="prop" else portrait_fixture2(seed,back="back" in low,open_mouth="mouth" in low)
  save("tools/testdata/upload/"+n,data)
 for n in ["01_park_swing_slide_6views.jpg","02_park_wooden_swing_6views.jpg","03_slide_6views.jpg","04_swing_seat_6views.jpg","05_boy_turnaround_a.jpg","06_boy_turnaround_b.jpg"]:
  kind="scene" if "park" in n else "prop" if ("slide" in n or "swing" in n) else "character"
  save("tools/testdata/sheets40/"+n,sheet_fixture2(1200+len(synthetic),6,kind=="scene",kind))
 for n in ["01_arjun_shirt_rod_6views.jpg","02_kabir_jacket_6views.jpg","03_sameer_sweater_kulhad_6views.jpg"]:
  save("tools/testdata/sheets40b/"+n,sheet_fixture2(1300+len(synthetic),6,False,"character"))
 sample=R/"app/src/main/assets/sample"; cast=sample/"cast.txt"; refs=[]
 if cast.is_file():
  for line in cast.read_text(encoding="utf-8").splitlines():
   f=line.split("|")
   if f[0]=="char" and len(f)>2: refs.append(f[2])
   elif f[0]=="view" and len(f)>3: refs.append(f[3])
   elif f[0]=="scene" and len(f)>2: refs.append(f[2])
   elif f[0]=="shot" and len(f)>3: refs.append(f[3])
   elif f[0] in ("title","end") and len(f)>1: refs.append(f[1])
 refs.extend(["char_ratanlal.jpg","char_tarun.jpg","char_priya.jpg","char_arjun.jpg","char_rahul.jpg","char_kripa_pouch.jpg"])
 for n in sorted(set(refs)):
  rel="app/src/main/assets/sample/"+n; low=n.lower()
  if (R/rel).exists() and (R/rel).stat().st_size and rel not in syn: continue
  seed=1500+len(synthetic); data=prop_fixture2(seed,n) if ("pouch" in low or "bag" in low) else portrait_fixture2(seed,back="_back" in low,animal=any(k in low for k in ("simba","singham","rohu","khan")),open_mouth="vanusha" in low,turban=any(k in low for k in ("ratanlal","tarun"))) if n.startswith("char_") else background_fixture2(seed,n)
  save(rel,data)
 for n,freq in [("man.wav",125),("deep_slow.wav",78),("woman.wav",210),("child.wav",320)]:
  rel="voices/"+n; q=R/rel
  if rel in syn or not q.exists() or not q.stat().st_size:
   q.parent.mkdir(parents=True,exist_ok=True)
   with wave.open(str(q),"wb") as a:
    a.setnchannels(1); a.setsampwidth(2); a.setframerate(16000); a.writeframes(b"".join(struct.pack("<h",int(700*math.sin(2*math.pi*freq*k/16000))) for k in range(16000)))
   if rel not in syn and rel not in restored: synthetic.append(rel); syn.add(rel)
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
  put(f"tools/testdata/sheets/sheet{n:02d}.jpg",sheet_fixture(n),"synthetic")
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
  ("char_tarun.jpg",False,False,False,True),("char_priya.jpg",False,False,False,False),
  ("char_arjun.jpg",False,False,False,False),("char_rahul.jpg",False,False,False,False),
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
 rebuild_synthetic_fixtures()
 absent=[p for p in ["tools/testdata/sheets38"] if not (R/p).is_dir() or not any((R/p).iterdir())]
 manifest={"classification":"TEST FIXTURE INVENTORY — synthetic files are not film references",
  "original_archives_found":[p.name for p in ARCHIVES if p.is_file()],
  "original_files_restored":restored,"synthetic_test_only_files":synthetic,
  "identity_fixture_directories_missing":absent,
  "warning":"Synthetic sample/upload/audio/sheets40/sheets40b assets are test-only. The original family-reference directory sheets38 is intentionally not synthesized and requires its original source archive."}
 (R/"tools/testdata/fixture_inventory.json").write_text(json.dumps(manifest,indent=2)+"\n",encoding="utf-8")
 print(f"Restored {len(restored)} original files; created {len(synthetic)} explicitly synthetic test files.")
 if absent: print("BLOCKED: original identity fixtures missing: "+", ".join(absent))
if __name__=="__main__":main()
