# Handoff prompt: KahaniFilm AI Studio

Paste everything below the line into ChatGPT, together with the source zip of the branch.

---

You are taking over the development of **KahaniFilm AI Studio**, an Android app that turns a Hindi or English script
plus the owner's own pictures into an animated family film. The owner, Tarun, writes in English and Hindi. He wants
Pixar-level films and a debugged APK after every round of work. Read this whole brief before you change anything.

## 1. Where everything is

- Repository: https://github.com/rathoretarun-123/KahaniFilm-AI-Studio-v45
- Working branch: `ccr-a5fc4eef-d2p2nm`. All work and pushes go to this branch.
- Latest source: https://github.com/rathoretarun-123/KahaniFilm-AI-Studio-v45/archive/refs/heads/ccr-a5fc4eef-d2p2nm.zip
- Latest APK, always the newest CI build of the branch: https://github.com/rathoretarun-123/KahaniFilm-AI-Studio-v45/raw/apk/KahaniFilm.apk
  The README on the `apk` branch names the commit each APK was built from.
- The history of every decision and test: `docs/testing-v47.md` (sections 6.x, newest at the end).
- The script format: `docs/script_format.md` and `app/src/main/assets/help/script_format.txt` (Hindi and English).

## 2. How it is built and tested

- Plain Java 8 for the app, no Gradle: `bash build.sh` (aapt2, javac, d8, zipalign, apksigner; Android platforms 23 and
  34, build-tools 34). Output: `release/KahaniFilm-v47.apk`.
- Signing: `tools/debug.keystore` (alias `kahani`, password `android`) is committed on purpose, so every new APK
  installs as an update over the app already on the phone. Never regenerate or replace it. `build.sh` sets the
  version code to 47 × 10000 + the CI run number, so each CI build is newer than the last.
- CI: `.github/workflows/build-apk.yml`. On every push to the branch it builds the APK, publishes it to the `apk`
  branch (2 to 3 minutes after the push), then runs the test suite (15 to 20 minutes). A new push cancels the run still
  going. Failing tests appear as annotations on the check run.
- Tests: `tools/robotest` (JUnit 4). CI runs ProductionPlanTest, AppTest (Robolectric: the app's screens),
  HundredPassSoakTest, AakhiriChaiAcceptanceTest, AakhiriChaiDesktopRenderTest, BabuKeLiyeDesktopRenderTest,
  NarratorContractTest, BundledFilmAssetsTest, ReviewProtocolTest, ScreenplayNotesTest, SeatContinuityTest,
  SheetPanelsTest, DashboardSoakTest and ThreeStoriesSoakTest. The tests other than AppTest also run on a plain JVM.

## 3. Code map (`app/src/main/java/com/tarun/kahani`)

`core/` is pure Java and also runs on a desktop JVM:
- **ScriptParser**, the reader. `userFormat()` turns the owner's format into the lists the reader knows, in Hindi or
  English. `clauseSentences()` splits long comma-joined directions into sentences. `Name: "line" (manner)` becomes
  `Name (manner): "line"`, so the manner is never read aloud.
- **Bible**: `listedPlaces()`, and `intangible()`, which looks at the head noun. "पानी की बाल्टी" is a bucket and is
  asked for; "बारिश का पानी", "दोपहर की धूप", "आसमान और हवा" and feelings are not.
- **Director** stages every sentence: posture (sit, stand, kneel, lie), entrances, everyday actions (the EVERYDAY rows,
  including the owner's 61 activity phrases and 45 GIF descriptions), things in hands (`thingsIn`: taken out of a bag,
  held, given, put down, worn), umbrellas and books opening and closing, weather and wind. `scriptCamera()` turns the
  script's camera words into shots: wide, medium, close-up, pull back, push in, from above or below; "कट टू" is a cut.
- **ShotPlanner** plans the shots. **Renderer** draws every frame: sprites, the mesh rig, held things, the
  over-the-shoulder layer. **Art** loads the owner's pictures (cutouts, panels, views). **Rigidity** knows rigid and
  soft things and what opens. **Nature** draws wind, leaves, rain and water. **Cues** handles light and sound cues.
- **Cloud** calls Gemini first and, when Gemini fails, OpenAI ChatGPT at once, then a free fallback. **ScriptAI** and
  **PicSense** do the picture matching for the library, PicSense offline.
- **Film**, **Pose**, **Look**, **Puppet** and **FilmLook** hold the film, the poses, the looks and the colour finish.

`app/` is Android: **MainActivity** (all screens), **AutoLibrary** (what to ask for, library matching), **Library**,
**Project**, **Prefs** (Settings keys, including `openaiKey`), **FilmJob** (FPS = 24, hard-coded), **SheetSaver**.

## 4. Desktop tools for checking films (`tools/jvm`, `tools/review`)

- Render a film exactly as the phone does. Compile `core` and `tools/jvm` into one classes directory against the
  android-all-14 jar, then run from the repo root:
  `java -cp <classes> com.tarun.kahani.jvm.MakeFilm <script.txt> <assets dir> out.mp4 640 24 <stills dir>`.
  The assets dir holds the pictures and a `cast.txt` with lines such as `char|<name>|<file>`,
  `view|<name>|<angle>|<file>`, `shot||<thing>|<file>|object` and `scene|<n>|<file>`.
  Environment: `FROM` / `TO` in seconds, `QC=0` skips the pre-check.
- `tools/review`: `scan_frames.py` scans every frame of a film; `sheets.py` makes contact sheets; `FilmProbe`,
  `StageProbe`, `BatchProbe`, `CamProbe` and `IdProbe` show the staging, compare it across all stories, list the
  script's shots and the offline library scores. See `tools/review/README.md`.
- The four reference scripts:
  - `app/src/main/assets/aakhiri_chai_screenplay.txt`
  - `app/src/main/assets/babu_ke_liye_screenplay.txt`
  - `tools/testdata/aakhiri_jodi/aakhiri_jodi_screenplay.txt`, with the owner's pictures in `uploads/`
  - `tools/testdata/aakhiri_chitthi/aakhiri_chitthi_screenplay.txt`, in the new standard format
  - an English script in the same format: `tools/testdata/user_format_en/the_last_letter_en.txt`
- `tools/testdata` holds about 2,090 older test stories. Use BatchProbe before and after every change to prove older
  script formats still stage the same.

## 5. The owner's rules, for every script and every page of the app

The script format, from now on:

```
शीर्षक: फ़िल्म का नाम                Title: Name of the film

पात्र                                Characters
नाम (उम्र) (विवरण)                   Name (age) (description)

वस्तुएं                               Objects
वस्तु का नाम (विवरण)                 Name (description)

स्थान और पृष्ठभूमि                    Places and Backgrounds
जगह का नाम (विवरण)                   Name (description)

कहानी                                Story

दृश्य 1                               Scene 1
(दृश्य का विवरण)                     (description of the scene)
नाम: "संवाद" (बोलने का भाव)           Name: "Dialogue" (how it is said)

समाप्त                               END
```

Every description is in ( ), every dialogue in " ". No other marks such as * / #.

App rules:
1. Ask only for pictures of the characters, objects, places and backgrounds listed before दृश्य 1 / Scene 1. The studio
   page asks in this order: characters, objects, places and backgrounds, then optional reference images and videos.
2. Never ask for intangible things (wind, water, sky, sunlight, rain, dust, fire, shade, sounds, feelings), nor for
   shots, props or costumes the lists don't name.
3. Every upload takes up to 18 pictures.
4. Library pictures are matched correctly. Only confident matches are placed on their own; the rest wait for "Change".
5. Settings holds a Gemini key and a ChatGPT (OpenAI) key. When Gemini fails, ChatGPT is used at once.
6. Films are 24 fps, hard-coded.
7. The camera understands the shot words in descriptions (wide, medium, close-up, over the shoulder, from above or
   below, pull back, push in, cut to), and camera angles keep being refined.
8. Every page of the app follows these rules without exception.
9. Each new APK installs over the version already on the phone.

Film rules:
10. Check every frame like a human, not just one a second. The director, technical director, reader and editor weigh
    every frame; the director's call is final.
11. No new lips appear while speaking. No sharpness changes between frames.
12. Objects never appear from nowhere: they are taken out of a bag, picked up or handed over. Worn things stay worn.
13. Opening makes rigid things soft: umbrellas, books and laptops open and close realistically. The owner's reference
    GIFs and pictures are in `docs/reference/daily_activities`.
14. Cloth, leaves and grass flutter in the wind's direction. Kites and leaves fly. Water flows, splashes and makes waves.
15. Posture holds: someone seated stays seated in the next scene in the same place. Nobody walks through anyone.
16. The aim is a Pixar-level animated film.

## 6. State at the handoff

- The newest commit adds the script's camera directions. The local suite passed (40 tests). Its CI run starts with
  the push; check its result first.
- Before it, the script format in Hindi and English, END, the head-noun intangible check, the format guide button on
  the story page and the rising version code (commit `a213376`). Its CI run failed one AppTest check: every screen's own
  text must be English, and the story screen had a Hindi label. The newest commit makes that label English; the guide
  the button opens keeps both languages.
- The last commit with every CI test green is `f4a709f`.
- Recent fixes are described in `docs/testing-v47.md` sections 6.17 to 6.19: "आखिरी चिट्ठी" staged as written
  (seated on the bench, no false falls, vanishings, lights going out or face jumps), worn things, the letter taken out
  of the bag and from the hand, the seated profile while someone walks past, the over-the-shoulder layer per shot.

## 7. Open items, most important first

1. Check the script's camera shots in renders of बाबू के लिए and आखिरी चिट्ठी. बाबू's scene 3 now opens on
   "कट टू मीडियम क्लोज़-अप": make sure the rule that every part opens on a wide shot does not fight it, and that
   the close look at the letter frames the hands holding it.
2. आखिरी चिट्ठी: in the last shot Pari should rest her head on his shoulder. In scene 3 her hand should rest on his
   hand, he should hold the letter to his chest, and tears should run.
3. Render all four films at 24 fps, scan every frame, read the contact sheets, and fix what a human would notice.
4. Offline library matching is only a colour guess (on flat placeholder pictures it matched a little girl to a
   witch). With a key, Gemini or ChatGPT vision decides. Improve the offline guess without placing wrong pictures.
5. Rigid and soft behaviour under wind and other forces, and the mesh of the owner's pictures in the older films.
6. English ground-cover names such as "Dust and dry leaves on the ground" are still asked for; ask the owner.

## 8. How to work and report

- After each change: run the plain-JVM tests, compare the staging of all stories with BatchProbe so older scripts
  stay the same unless intended, render and scan the films it touches, then push to the branch and watch CI until
  it is green. Never skip or disable a test. Never regenerate the keystore.
- Report to the owner in short, plain English: the APK link, the source zip link for the exact commit
  (`https://github.com/rathoretarun-123/KahaniFilm-AI-Studio-v45/archive/<commit>.zip`), what changed, what was
  tested, and what is still open.
