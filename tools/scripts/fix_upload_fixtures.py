#!/usr/bin/env python3
"""Ensure phone-upload fixtures are single images rather than contact sheets."""
from pathlib import Path
from synthetic_art import image
R=Path(__file__).resolve().parents[2]
files=[("camera_photo.jpg",image(480,640,1,1,411)),("drawn_character_phone_saved.jpg",image(480,640,1,1,412)),("drawn_character_plain.png",image(480,640,1,1,413,open_mouth=True)),("drawn_place_phone_saved.jpg",image(640,420,1,1,414,"place")),("drawn_thing_phone_saved.jpg",image(480,360,1,1,415,"object","cup"))]
for name,data in files:
 p=R/"tools/testdata/upload"/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(data)
print("Replaced five upload fixtures with single-image inputs.")
