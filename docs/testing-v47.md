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
