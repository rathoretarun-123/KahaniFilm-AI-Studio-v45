KahaniFilm for Windows (v48)
============================

1. Unzip KahaniFilm-Windows.zip anywhere (for example C:\KahaniFilm).
2. Double-click KahaniFilm\KahaniFilm.exe.
   (Windows may say "Windows protected your PC": click "More info", then "Run anyway". The program is not signed.)
3. Script: choose your story's .txt file (Hindi or English, saved as UTF-8).
4. Pictures folder: choose a folder laid out like this (folder names exactly as the script names them):

     my pictures\
       वृंदा\                         one folder per character
         front.png                    a file name with front / side / back / three-quarter (or left / right)
         side_left.png                says which view it is
         side_right.png
         back.png
         three_quarter_left.png
         sheet_expressions.png        any other picture is read as a sheet of poses (up to 3x2)
       वनुषा\
       स्थान\                         the places
         घर का छोटा लिविंग रूम\        one folder per place, named as in the script
         सोफे वाला कोना\
       वस्तुएं\                       the things
         तीन सीटर वाला सोफा\
         भूरा टेडी बियर\
       title.png                      the starting page (optional)
       end.png                        the ending page (optional)

   Anything you do not give is drawn by the studio. You can also leave the folder empty.
5. Press "Make the film". The film is saved next to the script as <script name>.mp4, and its working files
   (the cast, stills, the production document) in the folder <script name>_film.

What is inside
- The film engine of KahaniFilm (the same as the desktop films), its own Java runtime.
- Kokoro-82M (Apache-2.0) voices, spoken by sherpa-onnx (Apache-2.0). Its Hindi uses espeak-ng's data (GPL-3.0):
  see kokoro\NOTICE.txt.
- MoveNet, MediaPipe FaceMesh and YuNet (Apache-2.0) for the body's joints, the lips and the face.
- ffmpeg 7.1 (GPL build from gyan.dev): ffmpeg\LICENSE.txt.

These are 2D cut-out animations of your pictures, not Pixar 3D animation.
