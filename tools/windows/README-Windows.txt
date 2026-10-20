KahaniFilm for Windows (v48)
============================

1. Unzip KahaniFilm-Windows.zip anywhere (for example C:\KahaniFilm).
2. Double-click KahaniFilm\KahaniFilm.exe.
   (Windows may say "Windows protected your PC": click "More info", then "Run anyway". The program is not signed.)
3. The app has the phone app's screens and colours:
   - Stories: "New story" or "Open a script (.txt)" (Hindi or English, saved as UTF-8). Each story is kept in
     Documents\KahaniFilm\<story name>\.
   - Script: type or paste the script, Save, "Check the script" (what the Studio will ask for).
   - Studio: asks only for what the script names, with the phone's limits — each character (up to 18 pictures;
     say which view each picture is: front, side, back, three-quarter, or a sheet of poses), each object (18),
     each place (18), the starting and ending page, and additional references (100, kept with the story).
     Each character can be given one of Kokoro's voices (or left automatic). Anything not given is drawn.
   - Make the film: choose the size, Start (Stop stops it). The whole log is in script_film\log.txt.
   - Film: play the MP4, open its folder, save a copy, see its stills.
   - Library: Kokoro's voices (Listen) and the pictures of every story.
   Not on Windows: recording your own voice, the phone's speech, the phone's share sheet.
4. For a film without the window: KahaniFilm\runtime\bin\java.exe -cp "app\KahaniFilm.jar;app\onnxruntime.jar"
   com.tarun.kahani.jvm.KahaniDesktop <script.txt> <pictures folder or -> [width]
   (the pictures folder laid out as the Studio saves it: a folder per character with front.png, side_left.png …,
   स्थान\<place>\, वस्तुएं\<thing>\, title.png, end.png).

What is inside
- The film engine of KahaniFilm (the same as the desktop films), its own Java runtime.
- Kokoro-82M (Apache-2.0) voices, spoken by sherpa-onnx (Apache-2.0). Its Hindi uses espeak-ng's data (GPL-3.0):
  see kokoro\NOTICE.txt.
- MoveNet, MediaPipe FaceMesh and YuNet (Apache-2.0) for the body's joints, the lips and the face.
- ffmpeg 7.1 (GPL build from gyan.dev): ffmpeg\LICENSE.txt.

These are 2D cut-out animations of your pictures, not Pixar 3D animation.
