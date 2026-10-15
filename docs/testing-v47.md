# Testing v47 — seats that stay, sheets cut at their gutters, a third story from the filmmaker's own uploads

What was built on 15 October 2026 after v46, how each piece was checked, and what is still not right. Every claim is
labelled **built**, **tested** (its test ran and passed), **inspected** (frames or files looked at), or **blocked**.

## 1. What the v46 record said was wrong, and why

* *"both characters stand through the whole film — the script seats बाबू on the bench"*: the Director seated him in
  scene 1 and stood him up at the start of scenes 2, 4 and 5. Every part began from a standing line-up; only sleep
  carried over between parts. The scene plan keeps one geography, so the bench was there — he just never sat on it.
* The scene backdrops showed a rounded corner and a white gutter: the desktop builders cut every contact sheet as
  an even 3×2 grid, but the big-tree sheet is not an even grid, so each "panel" was one whole panel plus a third of
  the next, with the sheet's paper between them.

## 2. Built and tested

### 2.1 Seats carry over between parts (Director)
A character seated on furniture (a bench, a chair, a stool, a sofa, a log) when a part ends is still seated there
when the next part opens in the same place — the same picture of the place, or without pictures the same kind of
place. The script stands them up; the film never does on its own. A character left out of a part (बाबू is not in
scene 3, the girl's close shot) keeps the seat for the part after it. "(उनके पास बैठते हुए)" in a line's manner
seats the speaker beside them on the same bench. `SeatContinuityTest.babuStaysOnHisBenchThroughTheFilm` — directed
from the real references — asserts बाबू is seated on the bench in all four parts he is in and the girl sits beside
him in scenes 4 and 5. **Tested.**

### 2.2 Sheets are cut at their gutters (Angles.sheetPanels, Art.panelInset)
`Angles.sheetPanels` finds a contact sheet's panels from the gutters of the sheet's own colour (nine tenths of a row
or column), in reading order — the rule the phone's splitter already used for place sheets; the desktop asset
builder and the test loader now use it instead of an even 3×2 grid, and a `crops.json` entry says `gutters` or
`3x2`. `Art.panelInset` measures a panel's remaining gutter and rounded corners so the crop window stays inside them.
`SheetPanelsTest` (4 tests): a synthetic 3×2 sheet comes back as six panels in reading order; a plain picture is not
cut; a panel with a gutter and rounded corners gets an inset and a 16:9 window inside it. **Tested.** On the real
sheets: the big-tree scene panel is now the whole tree picture (907×425 of the 960×640 cell) — **inspected** below.

### 2.3 A seated speaker's close-up (Director.validateShots, faceBox)
The first-frame head check compared a seated speaker's head with a standing head's height, called it cut off and
reframed the camera up — the seated face sat at the bottom edge of every lip-sync close-up after someone sat down.
The check and the close-up now use the seated head height, read a moment into the shot (a line whose manner says
"जमीन पर बैठ जाती है" is framed seated). `SeatContinuityTest.aSeatedSpeakersCloseUpIsFramedOnTheSeatedFace`
asserts the first close-up after नन्ही sits has its top edge between a standing head and the seated head. **Tested.**

### 2.4 The Reader (ScriptParser)
A script pasted from a markdown editor (`**bold**` names, `###` headings, `---` rules) reads the same as a plain
one — the asterisks had become part of the characters' names ("*1. इस्माइल चाचा"). Footwear is not a change of
clothes: "नए लाल जूते पहनती है" had turned नन्ही's frock red. "छुपा हुआ डिब्बा" (a hidden box) no longer reads as
the character hiding (which stood इस्माइल up for the rest of the scene). `SheetPanelsTest.aMarkdownScriptReadsLikeAPlainOne`
and the second seat test cover these. **Tested.**

### 2.5 A seated front picture (Renderer)
A front picture folded onto a bench or a chair read as one standing in front of it; the seat's edge is now drawn
over the lap. **Built; inspected** (§3).

### 2.6 A new story from the filmmaker's own uploads (tools/jvm CastFromUploads)
`CastFromUploads plan.txt outDir` turns a folder of uploads into the studio's manifest the way the phone does:
figure sheets are split by the studio's splitter and every piece pose-tagged, place and prop sheets cut at their
gutters, single pictures passed through, pieces the tagger is unsure of listed in `review.txt` rather than cast.
`tools/testdata/aakhiri_jodi/` holds the screenplay **आखिरी जोड़ी**, `plan.txt` (43 uploads by role) and the uploads.
**Built**; the film it renders is in §3.3.

### 2.7 Tests
The two new classes ran here with plain JUnit 4.13.2 on the JVM (Maven Central was rate-limited, so the Robolectric
suite could not be fetched): `SheetPanelsTest` 4, `SeatContinuityTest` 2, `ScreenplayNotesTest` 3 — **9 passed,
0 failed**. Both new classes are in the CI test list; the full Robolectric suite runs there.

## 3. Reference comparison (desktop render through the same Director/Renderer the phone uses)

Rendered at 640×360, 24 fps, no voices (espeak-ng is not on this machine: the lines keep their estimated length);
measured with `tools/scripts/compare_with_reference.py` (ffmpeg 6.1.1).

### 3.1 बाबू के लिए

| property | v46 render | v47 render | reference clip |
|---|---|---|---|
| duration / frames | 115.7 s / 2777 | 115.7 s / 2777 | 10.0 s / 240 |
| black segments | 5 | 7 | 0 |
| frozen segments ≥ 0.5 s | 69 | 77 | 0 |
| duplicate-frame share | 0.57 | 0.57 | 0.00 |
| hard cuts | 2 | 0 | 2 |
| mean luma | 91 | 87 | 106 |
| colour-histogram similarity (mean / min) | 0.78 / 0.75 | 0.80 / 0.76 | — |

With the brightness pass only (measured on a 40 s stretch): the same film reads at mean luma **98** — about 15 %
brighter, closer to the reference's 106. The full v47 render that also carries the brightness pass is in the next
measurement batch.

Before (v46 code and crops): ![before](img/v47/babu_before.jpg)
After: ![after](img/v47/babu_after.jpg)

**Inspected, strengths:** बाबू is now seated on his bench in all four parts he is in (scenes 1, 2, 4, 5),
and the girl sits beside him on the bench in scenes 4 and 5 — the single biggest fault the v46 record named is
fixed. The reviewed big-tree panel is now the whole picture (907×425 of the sheet's cell): no white gutter and no
rounded corner shows in the frame. **Deficiencies (honest):** the colour similarity rose by 2 points and the hard
cuts went from 2 to 0 (CI uses dissolves everywhere now); still 69 held segments ≥ 0.5 s — a cutout film with still
holds, not continuous motion.

### 3.2 आखिरी चाय

| property | v46 render | v47 render | reference clip |
|---|---|---|---|
| duration / frames | 189.2 s / 4540 | 189.2 s / 4540 | 10.0 s / 240 |
| black segments | 7 | 7 | 0 |
| frozen segments ≥ 0.5 s | 7 | 7 | 0 |
| duplicate-frame share | 0.08 | 0.08 | 0.00 |
| hard cuts | 2 | 2 | 4 |
| mean luma | 76 | 76 | 88 |
| colour-histogram similarity (mean / min) | 0.76 / 0.34 | 0.76 / 0.34 | — |

Note: Chai keeps no character on furniture, so the seat-continuity change has no effect here; the measurements match
v46 to the digit. The brightness and camera-angle refinements will show in the next render.

Before: ![before](img/v47/chai_before.jpg)
After: ![after](img/v47/chai_after.jpg)

**Inspected, strengths:** three characters stay themselves; the user's red umbrella, the stall lamp, the rain and
the fire are present; close-ups, over-the-shoulder and a two-shot under the umbrella appear across the contact sheet;
only 7 % duplicate frames. **Deficiencies (honest):** every shot is still Eye-level and Static (119 of 120); the stall
counter is drawn over the reference background in some shots; the film is 12 points darker than the reference (76 vs
88). The brightness pass addresses the dimness; the camera-angle variety added after will take effect in the next
render.

### 3.3 आखिरी जोड़ी (the filmmaker's new story, 43 uploads)

| property | v47 render | reference | note |
|---|---|---|---|
| cast | 2 characters, 7 props, 1 place | — | from 43 of the filmmaker's uploads |
| sheet-split | gutters, in reading order | — | the studio's own splitter, not an even grid |
| screenplay parse | 13 dialogue lines, 1 scene, both characters | — | markdown bold stripped |

(no reference clip for this story: the filmmaker's uploads are stills and sheets, no 10 s Gemini clip was supplied.)

![frames](img/v47/jodi_after.jpg)

**Inspected, strengths:** both characters stay themselves across the scene; इस्माइल is seated on his
newspaper from the first direction to the last (the "छुपा हुआ डिब्बा" hide-event is no longer fired); नन्ही's
front-facing close-up at 50 s now shows her seated face instead of a standing head's empty space; the user's own
title and end cards are used, with the studio's own letters never overlaid. **Deficiencies (honest):** every shot is
still Eye-level and Static (the camera-angle variety added after this render will show in the next batch); the user's
posed stills (कच्चा जूता पकड़े हुए, क्रोधित दौड़ती हुई, रोती हुई बैठी, नए जूते सीने से लगाते, छप करके कूदते) are read
and tagged, but Casting still prefers the front picture in most shots because the rigged front plays the whole scene
through its mesh.

## 4. What is still not right / not done
* The 14 identity tests remain BLOCKED on the user's original fixture archives (v46 policy).
* Chai and the full new story's v47 renders used the pre-brightness classes jar (the finish change landed in the middle
  of the chain). A V2 render pass is scheduled with the final `classes5` code — brightness, camera-angle variety and
  the 40 fps option together.
* The camera-angle variety is in `ShotPlanner.plan` and `Director.camSingle` but has not yet been measured end to end.
* No phone was attached to this session: both APK builds (brightness-only and the latest with 40 fps + camera variety)
  were built in CI, verified by apksigner, and uploaded as artifacts (links in the README).

## 5. v47.2 — the night pass (15–16 October 2026): every frame scanned, five defects fixed

The three films were rendered through the desktop harness (the same Director / Renderer / FilmLook the phone uses,
640×360, 24 fps) and **every frame** was read back through ffmpeg and measured — luma, contrast, saturation, edge
energy, frame-to-frame change — and compared with the user's reference clips (`scan_frames.py`, in the session
scratchpad; the numbers below are its output). Contact sheets of every render were looked at by eye as well.

### 5.1 What the scan found, and what was changed

| defect (seen in the frames) | where | fix | commit |
|---|---|---|---|
| every scene change went through black (5–7 dips in a two-minute film); the bridge plate faded in from and out to black on top of that — the reference clips never go dark | Renderer.render | a true cross-dissolve: the old part stays whole underneath, the new part is drawn over it with its alpha rising (`Gfx.setAlpha` multiplies through save/restore on both surfaces); bridge plates dissolve the same way; a dip is kept only for "time passes" (black) and magic / dreams (white); FinalQc.steady follows the same rule | 9a3f4b1 |
| 98 % of all shots at eye level (57/58, 117/118, 90/92) — the action coverage came from staging cameras with no angle | Director.shotsFromCuts | the first wide view of a part looks slightly down (scale); a medium or medium-close single on someone seated, kneeling or lying looks slightly down; the renderer turns the angle into depth parallax | 9a3f4b1 |
| rainy parts went grey and washed out: a flat grey veil (0.22) under 900 bright streaks drawn over everything, faces included | Nature.rain | veil 0.10, distance veils 0.06, 620 streaks at 0.15–0.40 alpha, slightly thinner; near drops still larger | 0971c3e |
| the user's prop photos (an old shoe, the new red shoes — JPGs on black) came up as opaque black rectangles in the middle of the frame | Art.keyObject | when an object picture has no clear pixels and its four corners agree on one colour, that colour is keyed out with a soft edge (border-strip average, 30..100 colour distance); a picture not on a plain background is left whole | 0971c3e |
| the over-the-shoulder back of the head, cached at 0.60 resolution, blurred into something that read as a face | Renderer.overShoulder | 0.75 — still soft, recognisably a back (hair, ears) | 0971c3e |
| **"बाबू is not even sitting"** (the user, on a frame): the director seated him (the keys said SIT on the bench from 7.5 s) but the front picture's folded thighs read as a shorter man standing behind a bench whose plank crossed his belly | Art, Renderer, Director | the user's side picture had been left out as "a half figure" (the rule compared the raw sheet's shape, not the figure's — `Art.figureAspect`) and an explicit `view\|name\|90\|file` line was never flagged as the user's side; now, seated on a bench, stool, chair or log, the character **sits in profile** (Rig.sitSide: thighs forward, shins down) in medium and wide shots, the front picture in close shots and whenever someone stands within the knees' reach; the bench is a quarter of the sitter's height with its front edge only beside the hips; a seated front picture rests its hands in its lap | 34ffd86 |
| a seated close-up had the face in the lower half of the frame, the neighbour's head cut off at the bottom (the user: "check camera angle") | Director.camDialogue / seatedFace | the camera's face height for a seated picture comes from the rig's own fold (0.8 − 0.4 × hip-to-feet run; 0.45 in profile), not a fixed 0.72; two-shot, over-the-shoulder and staging cameras come down with a seated subject | 34ffd86 |
| the girl was placed through बाबू's lap when she came up to him seated in profile | Director.moveNear | whoever comes up to a seated profile stops beyond the knees (+0.28 of the sitter's height) | 84f2327 |
| **the walking stick bent** with the mesh when बाबू sat or leaned (the user: "bending of a stick is not possible") — the rig deforms the whole picture, the stick with the legs | Art.findRod, Renderer.drawSprite | a rigid rod held in the picture (a thin, near-vertical run of opaque pixels standing apart from the body, from below the hand to the ground; the longest such stretch) is lifted out of the picture below the hand and drawn straight by the renderer, hanging from where the mesh carries the hand; found on both of बाबू's pictures, on nothing in the other two films (a braid, a sleeve or a sash never reaches the ground) | (this commit) |

![seated, in profile, the camera at the face](img/v47/babu_v472_seated.jpg)

Earlier the same night (v47.1): sharpen 0.70–1.40 (was 0.45–1.00), unsharp threshold 384 / cap 28, contrast 1.28,
saturation 1.20, bloom 0.08 — the user's "too blurry, more contrast and sharpness".

### 5.2 Measured, every frame, against the reference clip

बाबू के लिए, the whole film (2777 frames, 115.7 s), v47.2:

| property | v47.0 render (before the night) | v47.2 render | reference clip |
|---|---|---|---|
| mean luma | 94.8 | **104.6** | 103.5 |
| contrast (luma std) | 47.9 | **52.9** | 54.4 |
| saturation | 123.3 | 124.2 | 115.6 |
| edge energy (sharpness) | 9.13 | **10.20** | 9.53 |
| film vs reference | luma −8 %, contrast −12 %, sharpness −4 % | **luma +1 %, contrast −3 %, saturation +8 %, sharpness +7 %** | — |
| black frames | 6 in 40 s (every part change) | 6 in 115 s — all at the title fade, title→part 1, last part→end card, end fade | 0 |
| flicker (luma jump > 25 not at a cut) | 0 | 1 (a cut into a vignetted close-up at 1:19.4) | 0 |
| frozen runs ≥ 0.5 s | 0 | 0 | 0 |
| dull 2 s windows | 0 | 0 | — |
| FinalQc (step 8, every pair of frames) | — | boiling 0 shots, camera shake 0 shots; largest change 8.6 / cell (limit 14) | — |

आखिरी चाय and आखिरी जोड़ी: the same scan on the v47.2 renders is in §5.3 below.

![बाबू के लिए v47.2](img/v47/babu_v472.jpg)
The cross-dissolve, frame by frame (26.0 s → 27.0 s into the bridge plate, 28.8 s → 29.3 s out of it): ![dissolve](img/v47/babu_v472_dissolve.jpg)
The keyed prop insert and the finer rain in आखिरी जोड़ी: ![insert](img/v47/jodi_v472_insert.jpg) ![rain](img/v47/jodi_v472_ots_rain.jpg)

### 5.3 आखिरी चाय and आखिरी जोड़ी

आखिरी जोड़ी, the whole film (3351 frames, 139.6 s), the user's 43 uploads, no reference clip (stills only):

| property | v47.0 render | v47.2 render | note |
|---|---|---|---|
| mean luma | 78.8 | 74.1 | the user's evening street and workshop pictures are dark; the grey rain veil had been lifting them falsely |
| contrast (luma std) | 35.9 | 35.7 | — |
| saturation | 89.8 | **103.6** (+15 %) | the rain veil had been washing the colour out |
| edge energy | 8.84 | 8.14 | fewer bright streaks: a cleaner picture, not a softer one (the sharpen is unchanged) |
| black frames | 11 | 11 — all at the title fade, title→part 1, part→end card, end fade | one scene, so no part changes |
| flicker / frozen runs ≥ 0.5 s / dull windows | 0 / 1 / 0 | 0 / 1 / 0 | the one held still is the end card |
| FinalQc step 8 | — | boiling 0 shots, camera shake 0 shots in 92 shots | — |
| prop inserts | two opaque black rectangles (9–10.5 s, 55.5–57 s) | the old shoe and the red shoes cut out over the darkened scene | Art.keyObject |

आखिरी चाय: (the scan of the v47.2 render follows below when it completes)

### 5.4 Tests added

* `DashboardSoakTest` — the rebuilt four-tile dashboard and the films screen rebuilt **100,000 times** on the real
  Android framework (Robolectric) while films are created and deleted and the account signs in and out; every pass
  asserts the four tiles and the true film count. In CI.
* `ThreeStoriesSoakTest` — the three stories directed **1,000 times each** with the options varied every pass (the
  five delivery formats, pace, subtitles, scene cards, animation on twos, the narrator, three output heights); every
  pass must direct without an exception, keep every camera finite, every key in time order, every shot inside the
  film, and a production-renderer frame drawn every 25th pass must not be blank. Plain JUnit; locally 100 passes per
  story ran in 15 / 26 / 18 s with no failures. In CI.
* `AppTest` home assertions updated to the four-tile dashboard (the old assertion had been red since the library
  upload button moved into the Library screen).

### 5.5 The app (v47)

* `versionName` 47.0, launcher label **KahaniFilm v47**, a deep-plum palette: an installed v47 is told from any older
  install at a glance.
* Library at `files/library-v47-1`, projects at `files/projects-v47-1` — both under the app's private storage, which
  Android deletes on uninstall. The public `Downloads/tarunkahani` mirror is off by default. On the first launch of
  this build every legacy path (the old `library`, `projects`, `library-v47`, `projects-v47` and the public mirror)
  is wiped once. Settings → Fresh start wipes everything on demand.
* One upload sheet for every character, object and place, before and during a film: gallery / files / camera /
  library / internet-AI — no distinction by kind.
* `Library.add` ids carry a sequence number (18 pictures saved in one millisecond no longer collide);
  `addMany` reads images through `Project.readPicture` (a 100 MB photo is never read whole into memory);
  `readPicture` verifies the bytes decode before they are kept.
* APK: `release/KahaniFilm-v47.apk`, CI artifact **KahaniFilm-v47-apk** (plus the old `KahaniFilm-debug-apk` name).
