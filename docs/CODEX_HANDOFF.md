TASK FOR CODEX: continue developing the KahaniFilm AI Studio Android app (the final, complete handoff)

AUTHORIZATION
I am Tarun, the owner of this project and of the GitHub repository below. I authorize you to work on my PC, in this
repository, on your own and without asking me anything. You may read and change any file, install the tools and SDKs
the build needs, run builds, tests and film renders, commit, and push to the working branch. Use my GitHub login on
this PC; run "gh auth login" if git asks. Keep working through the night until I come back. Do not stop to ask
questions: make the sensible choice, write each assumption in docs/testing-v47.md, and continue.

THE AIM
KahaniFilm turns a Hindi or English script plus my own pictures (characters, objects, places) into an animated family
film on an Android phone. The aim is Pixar-level quality: the film follows the script exactly, the staging is right,
the acting is natural, the physics is believable, and the pictures and lip-sync are steady in every frame.

0. MOST URGENT, IN THIS ORDER
a) git clone https://github.com/rathoretarun-123/KahaniFilm-AI-Studio-v45 (or git fetch), then
   git checkout ccr-a5fc4eef-d2p2nm and git pull.
b) Look at the GitHub Actions run of the newest commit on that branch (workflow "Build KahaniFilm APK"). If it failed,
   read its annotations and fix it first.
c) Within 30 minutes, send me the APK link and the source zip link of the newest commit whose CI run is fully green.
d) Then work through the open items in section 9, reporting after every verified round.

1. EVERY LOCATION
Repository and branches
- Repository: https://github.com/rathoretarun-123/KahaniFilm-AI-Studio-v45 (the only repository of this project).
- Working branch: ccr-a5fc4eef-d2p2nm. All the current work is here. Work and push only here.
- Branch "apk": CI writes the newest APK there. Direct download for my phone:
  https://github.com/rathoretarun-123/KahaniFilm-AI-Studio-v45/raw/apk/KahaniFilm.apk
  Its README.md names the commit the APK was built from.
- Source zip of any commit: https://github.com/rathoretarun-123/KahaniFilm-AI-Studio-v45/archive/<full commit sha>.zip
- Branch "kahanifilm-development" and about 20 feature branches (mesh-rigging-v46, shot-review-integration-v1,
  kahani-render-quality-gates and others) are older work. Everything useful from them is already in the working branch.
  Do not build from them.
- Last commit with every CI test green before the handoff: f4a709f. Then a213376 (CI failed one AppTest check, fixed
  in 9de6050), 9de6050 (its CI run was cancelled by the next push), c6f4772 (the handoff files and picture folders),
  and the commit that last updated this file. Check the newest commit's CI first (step 0b).

Build, CI and signing
- build.sh: builds the APK (release/KahaniFilm-v47.apk).
- .github/workflows/build-apk.yml: the CI.
- tools/debug.keystore: the signing key (alias kahani, password android). Committed on purpose.
- app/src/main/AndroidManifest.xml: package and versionCode 47 (build.sh adds the CI run number).

Source code
- app/src/main/java/com/tarun/kahani/core: the film engine, pure Java (see section 5).
- app/src/main/java/com/tarun/kahani/app: the Android screens and services.
- app/src/main/res: icons and resources.

Scripts and pictures
- app/src/main/assets/aakhiri_chai_screenplay.txt: आखिरी चाय (bundled example film).
- app/src/main/assets/babu_ke_liye_screenplay.txt: बाबू के लिए (bundled example film).
- app/src/main/assets/film_library/aakhiri_chai and .../babu_ke_liye: the bundled pictures and reference videos of
  those two films (tools/scripts/prepare_bundled_film_assets.sh checks and imports them).
- tools/testdata/aakhiri_jodi/aakhiri_jodi_screenplay.txt, plan.txt and uploads/: आखिरी जोड़ी and my 43 uploads.
- tools/testdata/aakhiri_chitthi/aakhiri_chitthi_screenplay.txt: आखिरी चिट्ठी, the new standard format.
- tools/testdata/user_format_en/the_last_letter_en.txt: an English script in the same format.
- tools/testdata/render_assets/<film>/: ready picture folders with cast.txt for desktop renders of all four films
  (आखिरी चिट्ठी uses stand-in pictures; I have not given its pictures yet). See its README.md.
- tools/testdata/*: about 2,090 older test stories in many formats, for regression checks.
- app/src/main/assets/sample_story.txt and sample/: the app's sample story and placeholder pictures.
- voices/: voice samples used by tests.

My references for the director
- docs/reference/daily_activities/: my activity pictures (pictures/01..48), vocabulary_hi.md (61 phrases),
  gif_descriptions_40_hi.txt (45 GIF descriptions), umbrella_open_close.gif and book_hold_open_close_put_down.gif with
  frame sheets.
- app/src/main/assets/vocab/daily_activities_hi.txt and daily_activities_40_hi.txt: the same vocabulary inside the app.
- app/src/main/assets/director_reference_emotion_activity_guide.md (section 20) and docs/directors-manual.md.
- I also uploaded two zips of GIFs (40 daily activities; 5 more: cycling, chess, cricket, football, newspaper). They
  arrived only as HTML download pages, so only their descriptions were saved (gif_descriptions_40_hi.txt and the app
  vocabulary). If I give the GIF files again, add them to docs/reference/daily_activities and learn from them.

Records and guides
- docs/testing-v47.md: the history of every decision, defect and test, newest at the end (sections 6.x).
- docs/script_format.md and app/src/main/assets/help/script_format.txt: the script format in Hindi and English.
- docs/HANDOFF_PROMPT_CHATGPT.md: the earlier brief. docs/CODEX_HANDOFF.md: this file.

Tools
- tools/jvm/com/tarun/kahani/jvm/MakeFilm.java: renders a film on a desktop with exactly the phone's code.
- tools/review/: scan_frames.py (every frame), sheets.py (contact sheets), FilmProbe, StageProbe, BatchProbe, CamProbe,
  IdProbe, P2, U, and cloudtest/ (a fake Gemini and OpenAI server). See tools/review/README.md.
- tools/robotest/: the tests. tools/scripts/: the fixture and asset preparation scripts CI runs.

2. SET UP THE PC
Linux, or WSL (Ubuntu) on a Windows PC, is easiest, because build.sh and the CI steps are bash. Install:
- JDK 17 (the build) and JDK 21 (the tests).
- Android SDK command-line tools, then: sdkmanager "platform-tools" "platforms;android-23" "platforms;android-34"
  "build-tools;34.0.0". Put $ANDROID_HOME/build-tools/34.0.0 on PATH (aapt2, d8, zipalign, apksigner). Set ANDROID_HOME.
- Gradle 8.14 or newer (for tools/robotest).
- ffmpeg and espeak-ng (MakeFilm encodes with ffmpeg and makes voices with espeak-ng).
- python3 with Pillow (python3-pil), zip, unzip, curl, git, gh.
- The Robolectric framework jar: download
  https://repo1.maven.org/maven2/org/robolectric/android-all/14-robolectric-10818077/android-all-14-robolectric-10818077.jar
  to ~/.cache/kahani/android-all-14.jar (build.sh and the tests use it).

3. BUILD, TEST AND CI
- APK: bash build.sh. It reads versionCode and versionName from the manifest; on CI it sets
  versionCode = 47*10000 + GITHUB_RUN_NUMBER and versionName 47.0.<run>, so every build installs as an update.
  Never change the keystore, the package name or the signing; keep the version code rising.
- CI (on every push to the working branch, or by manual dispatch): builds, publishes the APK to the "apk" branch about
  3 minutes after the push, then runs the tests (about 20 minutes). A new push cancels the run still going. Failing
  tests show as annotations on the check run.
- Tests, prepared exactly as CI does:
  * bash tools/scripts/prepare_bundled_film_assets.sh
  * python3 tools/scripts/prepare_legacy_test_fixtures.py
  * mkdir -p tools/robotest/voices and copy voices/man.wav, deep_slow.wav, woman.wav, child.wav into it
  * python3 tools/scripts/prepare_render_reference_images.py
  * gradle -p tools/robotest test --continue --tests com.tarun.kahani.ProductionPlanTest --tests com.tarun.kahani.AppTest
    --tests com.tarun.kahani.HundredPassSoakTest --tests com.tarun.kahani.AakhiriChaiAcceptanceTest
    --tests com.tarun.kahani.AakhiriChaiDesktopRenderTest --tests com.tarun.kahani.BabuKeLiyeDesktopRenderTest
    --tests com.tarun.kahani.NarratorContractTest --tests com.tarun.kahani.BundledFilmAssetsTest
    --tests com.tarun.kahani.ReviewProtocolTest --tests com.tarun.kahani.ScreenplayNotesTest
    --tests com.tarun.kahani.SeatContinuityTest --tests com.tarun.kahani.SheetPanelsTest
    --tests com.tarun.kahani.DashboardSoakTest --tests com.tarun.kahani.ThreeStoriesSoakTest
- Never skip, disable or weaken a test to get green. AppTest requires every screen's own text to be English; Hindi only
  inside content (the script, the format guide).
- Desktop render: compile app/src/main/java/com/tarun/kahani/core and tools/jvm into one classes directory with
  javac -encoding UTF-8 -source 8 -target 8 -cp ~/.cache/kahani/android-all-14.jar, then from the repo root:
  java -cp <classes> com.tarun.kahani.jvm.MakeFilm <script> tools/testdata/render_assets/<film> out.mp4 640 24 <stills dir>
  Options (environment): FROM / TO (seconds), QC=0 (skip the pre-check), ONLY=1 with TIMES=t1,t2 (single frames),
  STILL_EVERY, ASPECT, SUBS, NO_LOOK, SHOTS=<file> (the shot list), ACTS=all, FXS=1, KEYS=<name>, WIDE=1.
- Checking a film: ffmpeg -i out.mp4 -vf fps=1 sec/f%04d.jpg, python3 tools/review/sheets.py sec sheet 6 4,
  python3 tools/review/scan_frames.py out.mp4 scan. A big change at a planned cut is fine; look at the frame pair.
- Regression check for the reader and director: run tools/review/BatchProbe.java over a list of all scripts in
  tools/testdata (plus the four films) before and after a change, and diff the outputs. Older scripts must stage the
  same unless the change is meant for them.

4. HOW THE APP WORKS (the pages)
Home (four tiles: New film, My films, Library, Settings) > Story page (paste the script; "📝 Script format (Hindi and
English)" shows the format) > Studio (pictures: 1. Characters, 2. Objects, 3. Places and backgrounds, 4. reference
images and videos, optional; title and end pages are made by the studio) > Make the film (24 fps) > the film.
Library: my saved pictures, voices and sounds; the director matches them to a story. Settings: the Gemini key and the
OpenAI ChatGPT key.

5. CODE MAP (app/src/main/java/com/tarun/kahani)
core:
- ScriptParser: the reader. userFormat() reads my format in Hindi or English (headings, Scene/दृश्य N, END/समाप्त);
  clauseSentences() splits long comma-joined directions; Name: "line" (manner) becomes Name (manner): "line".
- Bible: listedPlaces(); intangible() (the head noun decides: "पानी की बाल्टी" is asked for, "बारिश का पानी" is not).
- Director: stages every sentence: posture, entrances, everyday actions (the EVERYDAY rows hold my 61 phrases and 45
  GIF descriptions), things in hands (thingsIn), umbrellas and books, weather, wind, and scriptCamera() for the
  script's camera words.
- ShotPlanner (shots), Renderer (every frame), Art (my pictures), Rigidity (rigid and soft, what opens), Nature (wind,
  leaves, rain, water), Cues (light and sound cues), Cloud (Gemini, then ChatGPT at once; models gemini-flash-latest,
  gemini-2.0-flash; gpt-4.1-mini, gpt-4o-mini), ScriptAI and PicSense (picture matching), Film, Pose, Look, Puppet,
  FilmLook.
app:
- MainActivity (all screens; identify() places library pictures above a 0.45 match), AutoLibrary (what to ask for),
  Library, Project (MAX_REF_FRAMES = 18, MAX_REF_VIDEOS = 2), Prefs (Settings keys, openaiKey), FilmJob (FPS = 24),
  SheetSaver.

6. THE SCRIPT FORMAT (every script from now on, Hindi or English; the app must read any script written this way)
शीर्षक: फ़िल्म का नाम  /  Title: name of the film
पात्र  /  Characters:  नाम (उम्र) (विवरण)  /  Name (age) (description), one per line
वस्तुएं  /  Objects:  name (description), one per line
स्थान और पृष्ठभूमि  /  Places and Backgrounds:  name (description), one per line
कहानी  /  Story
दृश्य 1  /  Scene 1
(scene description)
नाम: "संवाद" (how it is said)  /  Name: "Dialogue" (how it is said)
दृश्य 2  /  Scene 2 …
समाप्त  /  END
Every description in ( ), every dialogue in " ". No other marks such as * / #. The reader, director, technical
director and editor must understand this format fully.

7. ALL MY INSTRUCTIONS (keep every one true, for every script and every page of the app)
The studio and the app
1. Ask only for pictures of the characters, objects, places and backgrounds listed before दृश्य 1 / Scene 1. The studio
   asks in this order: characters, then objects, then places and backgrounds, then reference images and videos of the
   story (optional). Remove anything extra.
2. Never ask for pictures of intangible things: wind, water, sky, sunlight, rain, dust, fire, shade, sounds, smells,
   feelings, emotions. Never ask for shots, props or costumes the lists don't name.
3. Every upload takes up to 18 pictures, the same as for characters.
4. Library identification must be correct. Only confident matches are placed; the rest wait for "Change".
5. Settings has a Gemini API key and a ChatGPT (OpenAI) API key. If Gemini fails, switch to ChatGPT immediately.
6. Films are made at 24 frames per second, hard-coded.
7. Every page of the app follows these instructions without exception.
8. Each new APK installs over (updates) the version already on my phone.
The film
9. Check every frame like a human, not one frame a second. The director, technical director, reader and editor
   deliberate on every frame instead of every shot, think like humans, find errors and correct them. The director's
   call is final.
10. No new lips form while a character speaks. No sharpness changes from frame to frame.
11. Objects never appear from nowhere: they are taken out of a bag, picked up or handed over. An open umbrella is
    always held by someone, never open and folded at once. Worn things (glasses, ribbons, slippers) stay worn.
12. Opening makes rigid things soft: umbrellas, books, laptops and similar things open and close realistically, as in
    my reference GIFs (how a book is held, opened, closed and put down; how an umbrella opens and closes).
13. Natural forces: cloth, leaves and grass flutter in the wind's direction; kites and leaves fly in the wind; water
    flows, splashes and makes waves; rigid and soft objects react correctly to wind and every other force.
14. Posture and continuity: a seated person stays seated in the next scene in the same place unless the script stands
    them up. Nobody walks through anyone.
15. Camera: understand the shot words in descriptions (wide, medium, close-up, over the shoulder, from above, from
    below, pull back, push in, cut to) and keep refining the camera angles.
16. Learn from all my activity references (61 phrases, 45 GIF descriptions, the pictures) and keep them in the
    director's vocabulary and in the library.
17. Refine the meshing (the rig of my pictures) and the rendering in all four stories, and check each one frame by
    frame.
Delivery
18. Debug the APK many times before each delivery: build it, check the app's own flows (a new film, the story page,
    the studio uploads, the library, Settings, making a film) and the tests, and only then send the links.
19. Every delivery gives me two links: the direct APK download and the source code zip of the exact commit. The APK
    must be bug free, with all the tests done.
20. Keep refining, checking and debugging on your own, without waiting for my instructions, and send me the links
    again after every verified round.

8. WHAT IS ALREADY DONE (git log of the working branch; details in docs/testing-v47.md)
- One committed signing key, so updates install over each other (fdc3714); every APK published to the apk branch
  (be8b0e4); a rising version code per CI build (a213376).
- Things in hands, never floating inserts: taken out of a bag or box, held, handed over, put down (27144ae, f18b25f,
  98d17b7); the notebook in hand from the start (1a21c90); the letter stays in the girl's hand (aebf7e3).
- Umbrellas: one umbrella, held, put aside as the script says, its colour from the story (3bfaacb, e34d53d, 9d3fd6e,
  1a21c90). The book opens and closes (1a21c90).
- No painted face on a profile, smooth sharpness, wind-true leaves, flowing water and splash, things that open
  (f9bcac9); soft things flutter in the wind (65f7351); a wind that starts blows nobody away (7a10b73).
- 24 fps hard-coded (1a21c90).
- My activity vocabulary: 61 phrases and 28 pictures (8e5ee5e), 20 more pictures (48a976e), 45 GIF descriptions and
  12 everyday rows (a4744f7).
- The studio: uploads in sections (c858be5); intangibles never asked for (77136f3, a213376); only the listed items, in
  my order, 18 pictures per upload, confident library matches only (837ad48); the library searched on its own (27264c3).
- AI keys: a ChatGPT key in Settings; Gemini falls to ChatGPT at once; a Gemini model the key cannot reach falls to
  the next (9ae66c6).
- The reader: my format, first in Hindi (0764065), then Hindi and English with END (a213376); the story page shows the
  format guide (a213376, 9de6050).
- आखिरी चिट्ठी staged as written: seated on the bench, no false falls, vanishings, lights going out or face jumps,
  worn things stay worn, the letter from the bag and from the hand (335ca55); the over-the-shoulder layer per shot
  (e953e63); the camera the script writes (9de6050).
- Backdrop sheets keep one panel without its gutter (facbc52).

9. OPEN ITEMS, MOST IMPORTANT FIRST
1. Confirm CI is green on the newest commit; fix any failure.
2. Check the script's camera shots in renders of बाबू के लिए and आखिरी चिट्ठी (CamProbe lists them). Babu's scene 3
   opens on "कट टू मीडियम क्लोज़-अप": the rule that every part opens on a wide shot must not fight it. The close look at
   the letter must frame the hands holding it.
3. आखिरी चिट्ठी: in the last shot Pari rests her head on his shoulder; in scene 3 her hand rests on his hand, he holds
   the letter to his chest, tears run.
4. Render all four films (आखिरी चाय, बाबू के लिए, आखिरी जोड़ी, आखिरी चिट्ठी) at 24 fps from tools/testdata/render_assets,
   scan every frame, read the contact sheets, fix every defect a human would see, and repeat.
5. Offline library matching is only a colour guess; with a key, Gemini or ChatGPT vision decides. Improve the offline
   guess without placing wrong pictures (IdProbe shows the scores).
6. Rigid and soft behaviour under wind and other forces; the mesh quality of my pictures in all films.
7. English ground-cover names like "Dust and dry leaves on the ground" are still asked for; treat them like the Hindi
   ones (dust is intangible).

10. HOW TO WORK AND REPORT
- After each change: run the tests, compare staging with BatchProbe, render and scan the films the change touches,
  push to the working branch, and wait for CI to be green.
- Each time CI is green on a new commit, report to me in short plain English: the APK link, the source zip link with
  the full commit sha, what changed, what was tested, what is still open. The first report within 30 minutes, then
  after every verified round through the night. I write in English and Hindi; answer in simple English.
- Write each round's findings in docs/testing-v47.md.
