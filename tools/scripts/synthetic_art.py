"""Privacy-safe synthetic art fixtures for deterministic image-processing tests."""
import struct,zlib

def image(w,h,cols=1,rows=1,seed=0,kind="person",style="outdoor",back=False,open_mouth=False):
 p=bytearray([248,248,246]*(w*h))
 def rect(x0,y0,x1,y1,c):
  for y in range(max(0,int(y0)),min(h,int(y1))):
   for x in range(max(0,int(x0)),min(w,int(x1))):
    i=(y*w+x)*3;p[i:i+3]=bytes(c)
 def oval(cx,cy,rx,ry,c):
  rx=max(1,int(rx));ry=max(1,int(ry))
  for y in range(max(0,int(cy)-ry),min(h,int(cy)+ry+1)):
   for x in range(max(0,int(cx)-rx),min(w,int(cx)+rx+1)):
    if ((x-cx)/float(rx))**2+((y-cy)/float(ry))**2<=1:
     i=(y*w+x)*3;p[i:i+3]=bytes(c)
 def line(x0,y0,x1,y1,c,t=4):
  n=max(abs(int(x1)-int(x0)),abs(int(y1)-int(y0)),1);r=max(1,int(t)//2)
  for k in range(n+1):oval(round(x0+(x1-x0)*k/n),round(y0+(y1-y0)*k/n),r,r,c)
 def person(box,n):
  x0,y0,x1,y1=box;cw=x1-x0;ch=y1-y0;cx=(x0+x1)//2
  hr=max(4,min(int(cw*.17),int(ch*.13)));hy=y0+int(ch*.21)
  hair=[(50,37,29),(78,51,35),(42,41,44),(99,65,41)][seed%4]
  skin=[(211,158,124),(189,129,94),(225,180,145)][seed%3]
  cloth=[(48,108,160),(176,76,66),(69,131,85),(128,91,153)][seed%4]
  v=("back" if back else ["front","three-quarter","side","back","walk","arms_up"][n%6])
  side=v=="side";q=v=="three-quarter";rx=int(hr*(.64 if side else .82 if q else 1))
  oval(cx,hy-2,rx+3,hr+3,hair)
  if v!="back":
   oval(cx+(int(rx*.12) if q else 0),hy,rx,hr,skin)
   oval(cx,hy-int(hr*.42),rx+2,max(3,int(hr*.52)),hair)
   rect(cx-rx-1,hy-int(hr*.1),cx-rx//2,hy+int(hr*.5),hair)
   if side:
    oval(cx+int(rx*.3),hy,max(1,rx//10),max(1,hr//10),(25,25,25))
    line(cx+rx-1,hy+int(hr*.16),cx+rx+max(2,hr//4),hy+int(hr*.2),skin,max(2,hr//8))
   else:
    sep=max(2,int(rx*(.42 if not q else .34)));ey=hy-int(hr*.05)
    oval(cx-sep//2,ey,max(1,rx//11),max(1,hr//10),(25,25,25))
    oval(cx+sep//2+(int(rx*.12) if q else 0),ey,max(1,rx//12),max(1,hr//10),(25,25,25))
   my=hy+int(hr*.48)
   if open_mouth and n==0:
    oval(cx,my,max(2,rx//5),max(3,hr//4),(75,24,30));rect(cx-rx//8,my-2,cx+rx//8,my+2,(246,235,215))
   else:line(cx-rx//5,my,cx+rx//5,my,(100,40,39),max(2,rx//18))
  neck=max(2,hr//4);top=hy+hr;bottom=y0+int(ch*.70);should=max(5,int(hr*1.2))
  rect(cx-neck,top-2,cx+neck,top+max(3,hr//2),skin);top+=max(2,hr//3)
  rect(cx-should,top,cx+should,bottom,cloth);hand=y0+int(ch*.68);armY=top+max(2,int((bottom-top)*.12));limb=max(3,int(hr*.22))
  if v=="arms_up":
   line(cx-should,armY,cx-should-limb*2,top-hr,skin,limb);line(cx+should,armY,cx+should+limb*2,top-hr,skin,limb)
  else:
   line(cx-should,armY,cx-should-limb,hand,skin,limb);line(cx+should,armY,cx+should+limb,hand,skin,limb)
  gap=max(2,int(hr*.16));leg=max(3,int(hr*.24));foot=y0+int(ch*.93);knee=y0+int(ch*.84)
  if v=="walk":
   line(cx-gap,bottom,cx-gap-leg,knee,cloth,leg);line(cx-gap-leg,knee,cx-gap-leg*2,foot-2,cloth,leg)
   line(cx+gap,bottom,cx+gap+leg,knee-4,cloth,leg);line(cx+gap+leg,knee-4,cx+gap+leg*2,foot-2,cloth,leg)
  else:
   line(cx-gap,bottom,cx-gap,foot-3,cloth,leg);line(cx+gap,bottom,cx+gap,foot-3,cloth,leg)
  rect(cx-gap-leg,foot-4,cx-gap+leg,foot+2,(52,42,36));rect(cx+gap-leg,foot-4,cx+gap+leg,foot+2,(52,42,36))
 def place(box,n):
  x0,y0,x1,y1=box;cw=x1-x0;ch=y1-y0
  sky=[(155,204,235),(190,218,238),(241,201,158),(126,159,198)][n%4]
  rect(x0,y0,x1,y0+int(ch*.68),sky);rect(x0,y0+int(ch*.68),x1,y1,(93,150,83))
  oval(x0+int(cw*.82),y0+int(ch*.18),max(3,int(cw*.06)),max(3,int(ch*.09)),(255,224,134))
  for f in (.13,.88):
   tx=x0+int(cw*f);ty=y0+int(ch*.75);rect(tx-2,ty-int(ch*.2),tx+3,ty,(94,64,41));oval(tx,ty-int(ch*.22),int(cw*.09),int(ch*.14),(48,115,56))
  if style in ("room","kitchen"):
   rect(x0+int(cw*.30),y0+int(ch*.25),x0+int(cw*.70),y0+int(ch*.72),(230,211,178))
   rect(x0+int(cw*.36),y0+int(ch*.34),x0+int(cw*.48),y0+int(ch*.48),(107,166,198))
   rect(x0+int(cw*.53),y0+int(ch*.34),x0+int(cw*.65),y0+int(ch*.48),(107,166,198))
   rect(x0+int(cw*.43),y0+int(ch*.54),x0+int(cw*.57),y0+int(ch*.72),(109,72,47))
  else:
   lx=x0+int(cw*.38);rx=x0+int(cw*.61);top=y0+int(ch*.24);base=y0+int(ch*.73)
   line(lx,base,lx+int(cw*.04),top,(70,62,53),max(2,cw//90));line(rx,base,rx-int(cw*.04),top,(70,62,53),max(2,cw//90))
   line(lx-int(cw*.04),top,rx+int(cw*.04),top,(70,62,53),max(2,cw//90))
   sy=y0+int(ch*.53);line(lx+int(cw*.02),top,lx+int(cw*.02),sy,(55,55,55),max(2,cw//150))
   rect(lx-int(cw*.025),sy,rx-int(cw*.035),sy+max(2,ch//30),(176,68,54))
 def obj(box,n):
  x0,y0,x1,y1=box;cw=x1-x0;ch=y1-y0;cx=(x0+x1)//2;base=y0+int(ch*.78)
  if style in ("swing","swing-seat"):
   lx=x0+int(cw*.30);rx=x0+int(cw*.70);top=y0+int(ch*.16)
   line(lx,base,lx+int(cw*.08),top,(57,111,166),max(2,cw//30));line(rx,base,rx-int(cw*.08),top,(57,111,166),max(2,cw//30))
   line(lx-int(cw*.08),top,rx+int(cw*.08),top,(57,111,166),max(2,cw//30))
   line(cx-int(cw*.1),top+2,cx-int(cw*.1),base-int(ch*.28),(58,61,65),max(2,cw//55))
   line(cx+int(cw*.1),top+2,cx+int(cw*.1),base-int(ch*.28),(58,61,65),max(2,cw//55))
   rect(cx-int(cw*.18),base-int(ch*.28),cx+int(cw*.18),base-int(ch*.22),(183,62,50))
  elif style in ("slide","rod","thing"):
   line(x0+int(cw*.28),base,x0+int(cw*.40),y0+int(ch*.22),(57,111,166),max(2,cw//30))
   line(x0+int(cw*.40),y0+int(ch*.22),x0+int(cw*.54),y0+int(ch*.22),(57,111,166),max(2,cw//30))
   line(x0+int(cw*.54),y0+int(ch*.22),x0+int(cw*.72),base,(183,62,50),max(2,cw//25))
  else:rect(cx-int(cw*.18),y0+int(ch*.30),cx+int(cw*.18),base,(146,98,54))
 for n in range(cols*rows):
  cw=w//cols;ch=h//rows;x0=(n%cols)*cw+4;x1=min(w,(n%cols+1)*cw-4)
  y0=(n//cols)*ch+4;y1=min(h,(n//cols+1)*ch-4);box=(x0+4,y0+4,x1-4,y1-4)
  if kind=="place":place(box,n)
  elif kind=="object":obj(box,n)
  else:person(box,n)
 def chunk(t,d):return struct.pack(">I",len(d))+t+d+struct.pack(">I",zlib.crc32(t+d)&0xffffffff)
 raw=b"".join(b"\0"+p[y*w*3:(y+1)*w*3] for y in range(h))
 return b"\x89PNG\r\n\x1a\n"+chunk(b"IHDR",struct.pack(">IIBBBBB",w,h,8,2,0,0,0))+chunk(b"IDAT",zlib.compress(raw,6))+chunk(b"IEND",b"")
