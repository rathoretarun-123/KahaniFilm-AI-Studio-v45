# Film review tools

Used to check every film frame by frame on a desktop JVM, exactly as the phone draws it.

- `scan_frames.py <film.mp4> <out dir>` — scans every frame: black or blown frames, flicker, large jumps inside a shot,
  frozen runs, dull stretches. A jump at a planned cut is expected; look at the frame pair before calling it a defect.
- `sheets.py <stills dir> <out prefix> <per row> <rows>` — contact sheets of stills (e.g. one frame a second from
  `ffmpeg -i film.mp4 -vf fps=1 sec/f%04d.jpg`).
- `FilmProbe.java <script>` — every actor's posture every 0.5 s (STAND / SIT / KNEEL / LIE, seat, anchor) and actions.
- `StageProbe.java <file of sentences>` — stages one direction sentence at a time with two stand-in characters.
- `BatchProbe.java <list of scripts>` — a digest of the staging of every script in the list; run it before and after a
  change over all of tools/testdata and diff the two outputs to see which stories a change touches.
- `CamProbe.java <scripts…>` — the shots the script's own camera words asked for.
- `IdProbe.java <script> <pictures…>` — the offline library scores of pictures against the script's characters.

Compile core (app/src/main/java/com/tarun/kahani/core) and tools/jvm into one classes directory with `javac -source 8
-target 8 -cp android-all-14.jar`, then run the probes with that directory on the class path. Render a film with
`java -cp <classes> com.tarun.kahani.jvm.MakeFilm <script> <assets dir> out.mp4 640 24 <stills dir>` from the repo root.
