# The production document (v47.6) — baseline audit and what changed

This note covers Stage A (the baseline audit of the existing pipeline) and the parts of Stages B to J of the production
protocol that are now in code. Every claim here can be checked against a file, a test, or a render listed below. What
is not done is said so, in section 5.

## 1. Baseline (Stage A): the pipeline as it was

The phone makes a film in one call chain:

`MainActivity.makeFilm` → `FilmJob.start` → `FilmJob.run`:

1. **Read.** `ScriptParser.parse` turns the script into a `Story`.
2. **Production plan (optional, online).** `ScriptAI.productionConference` (cloud) is applied by `ProductionPlan.apply`.
3. **Pictures.** `Art.fromManifest(cast.txt)` loads them.
4. **Plan.** `Director.prepare` / `Director.direct` produce the `Film`: parts, cameras, shots, keys, effects, weather and sounds.
5. **Checks and mix.** Lock sheets, the review ledger, `Mixer`, Human QC, `FinalQc`.
6. **Render.** `Renderer` → `FilmLook` / `Grade` → `VideoWriter` (MediaCodec) writes the film.
7. **Files kept with the film:** `qc.txt`, `edit_list.txt`.

The desktop tool `tools/jvm/MakeFilm` runs the same reader, director and renderer, writing through ffmpeg.

Gaps found:

- **No single document for a production.** The reader's facts, the director's plan and the reviews lived in separate places:
  - the Story and the Film in memory;
  - `review_ledger.json` and `qc.txt` as text.
- **Ids changed between runs.** `DirectorsManual.charIds` (CHAR_001…) is recomputed every time and never kept, so adding a character renumbers the others.
- **Unused declarations.** `Story.ShotDirective` / `Scene.approvedShotDirectives` are declared but never used.
- **Review records don't gate rendering.** `ReviewLedger` and `ReviewConference` exist, but nothing reads `readyToRender` before a film is made.
- **No explicit / inferred / unknown marking.** Nothing marked a fact as written by the user, read from context, or not known.
- **Environment not checked.** Nothing compared what the script asks of the environment with what the plan schedules. Several of आखिरी चिट्ठी's requirements were silently dropped (section 3).
- **Physics.** Nature effects are procedural: particles and mesh sway are computed directly from time; there is no time-stepped simulation. `EverydayPhysics` is not reachable from the renderer.

## 2. What is now in code

### `core/ProductionDocument.java` (new, offline, deterministic)

It is built from what the reader and the director already produce. It does not read the story a second way, and it
does not plan shots of its own.

**Stable ids**

- Ids are CHAR_001, PROP_001, LOC_001, SCENE_01, LINE_001, ACT_S2_005, ENV_S1_03, STATE_001, SHOT_S3_21, ISSUE_001 and FACT_0001.
- People, things and places get their ids once and keep them in `production_ids.txt` in the project. A prop inserted in the middle of the list never renumbers the others (tested).

**Facts (each EXPLICIT, INFERRED or UNKNOWN, with the words it came from)**

- **Characters:** age, height, weight, posture ("back bent 90°"), bare feet, and the look kind (INFERRED).
- **Props:** size, material, condition, colour, owner, the broken part, what it supports, the foot it is worn on and the bare foot, a wound, and the writing on it.
  - The owner is EXPLICIT when the name says whose it is. When it comes from what the dialogue calls someone ("दादाजी"), it is INFERRED with the reason.
- **Places:** what a place is in front of (with the distance in steps), what it is behind, and the main location.

**Dialogue**

- Exactly as written, in order, with the speaker id and the delivery note (never part of the line).

**Steps**

- The script's descriptions are read as atomic steps, split at commas, at clause-joining "और" and after each finished verb.
- The doer is chosen by the Hindi verb's gender when the named character does not agree. For example, "धीरे से खड़ी होती है रघुवीर जी की तरफ…" is परी's step.
- Negations ("बिना आवाज किए" → MUST_NOT) and conditions ("अगर वह दौड़ती…" → HYPOTHETICAL) are read but never staged.

**The environment as causes and effects**

- Each requirement is tagged with how the film carries it out:

  | Method | What it means |
  |---|---|
  | IN_BACKDROP_PICTURE | the place's picture already shows it |
  | PROCEDURAL_ANIMATION | animated by formula |
  | PARTICLE_SYSTEM | drawn as particles |
  | SOUND_EFFECT | carried by a sound |
  | UNSUPPORTED | no method in this renderer |

- For each requirement the document also records whether the director actually scheduled it.

**Thing states**

- Where each listed thing is and who holds it, part by part. This includes what a line says happened before ("found on the ground behind the postbox": told, not shown).

**Shots**

- The director's own shot list, with the scripted flag.

**Issue register (BLOCKER / MAJOR / MINOR / WARNING)**

The technical director's checks:

- a speaker for every line;
- every explicit requirement scheduled, or said to be unsupported;
- every step the plan should show is in it;
- a picture for every person and listed thing;
- never two holders of one thing at once;
- no shot shorter than 0.5 s.

**Reviews and render records**

- **Bound to a revision.** Every review is bound to the revision it read, and any change raises the revision.
- **Director's sign-off.** It is refused over an open BLOCKER, and refused without the technical director's validation of the same revision.
- **Render records.** A render is VERIFIED only when it was made from the approved revision, its file exists and it is at least 1 KB.
  - With no file, or an empty file, it is FAILED.
  - With a file but no approval, it is UNAPPROVED.
- **Completion.** `complete()` is true only for a verified render of the current revision.

### The director (`core/Director.java`)

| Change | Script words |
|---|---|
| Wind said other ways is wind | "हवा अब भी चल रही है", "गर्म हवा से धूल उड़ रही है", "कुर्ता हवा में हल्का हिल रहा है" |
| A negation counts only in its own clause | "एक भी बादल नहीं, … गर्म हवा चल रही है" (the wind is there; before, the whole sentence was negated) |
| A light wind stays light; later sentences raise it only if they say "strong" | "हल्की-हल्की गर्म हवा" |
| Dust motes drift in the light (new `W_MOTES`), distinct from the dust storm | "धूल के कण हवा में उड़ रहे हैं" |
| Moving sun patches (new `W_DAPPLE`) | "धूप के गोल धब्बे … हिल रहे हैं" |
| Sounds the description names: cycle bell, leaf rustle in the wind, distant running steps | — |
| The bench rocks as she sits on it (new `FX_WOBBLE`) | "बेंच थोड़ी सी हिलती है क्योंकि उसका एक पैर टूटा है" |
| A bag set down from the back lies beside her — unless it is part of the user's picture of her, where it stays on her back and a note says why (two bags would be worse) | — |

- **Bench wobble.** It is a damped rock drawn on the user's bench picture: 1.8°, 2.4 rocks a second, settling in about two seconds. It is a procedural approximation, not physics, and the sitters do not rock with the bench.
- **Moving sun patches.** These are drawn in screen space and are not cast by the tree in the picture.

### Where the document is used

- **Phone film job (`app/FilmJob.java`).** After the director's plan, the job:
  - builds the document, validates it, records the reader's review and the director's sign-off;
  - writes `production_document.json` and `production_ids.txt` with the project;
  - adds the report to `qc.txt`.
  - When the film file is saved, the job records the render against the revision. The verification goes into the JSON, into `qc.txt` and onto the film page's info line.
  - A refused sign-off is shown as a warning. The film is still made and recorded as UNAPPROVED, never as verified.
- **Studio page (`app/MainActivity.showStudio`).** A "Production check" under the director's plan shows the summary and the open issues of the live plan. Pictures are checked by the cards above it.
- **Desktop (`tools/jvm/MakeFilm`).** `PRODDOC=file.json` writes the document with the plan, then again with the render's verification.

### The cast of आखिरी चिट्ठी

`tools/testdata/aakhiri_chitthi/plan.txt` leaves out `pari_upload_17`:

- that sheet shows a toddler with buns on a bench of her own;
- its pieces had been read as kneeling poses;
- so परी knelt at the postbox as that child, sitting on a second bench.

## 3. आखिरी चिट्ठी, before and after (the director's plan, user's pictures, desktop)

| Requirement (script) | Before | After |
|---|---|---|
| Light hot wind, scene 1 from the start | from 14.9 s at 0.6 (the "बादल नहीं" in the sentence negated it) | from 7.7 s at 0.35 |
| Wind in scenes 2 and 3 | none in 2; 5 s at the end of 3 | the whole of both scenes |
| Dust motes (all scenes) | not scheduled | `W_MOTES` |
| Cycle bell, leaf rustle, distant running steps | not scheduled | scheduled |
| Moving sun patches (scene 3) | not scheduled | `W_DAPPLE` (approximation) |
| Bench rocks as परी sits | not scheduled | `FX_WOBBLE` at her sit (approximation) |
| परी kneeling at the postbox | drawn as an off-model toddler on a second bench | on model; see section 5 |

The production document for the film now reports:

- 2 characters, 13 props, 3 places, 3 scenes, 7 lines and 30 steps;
- 25 of 25 environment requirements are in the plan;
- 6 open issues and no blockers:
  - leaves sliding along the ground: UNSUPPORTED;
  - setting the bag down: it is part of परी's picture;
  - her hand on his hand: no gesture;
  - her head on his shoulder: no gesture;
  - two 0.41 s cuts during her run (the technical director's 15 % motion rule).

## 4. Tests (plain JVM, `tools/robotest`)

**`ProductionDocumentTest` (6 tests)**

- ids stay when a prop is inserted;
- an approval is bound to its revision;
- the director never signs off over an open blocker, and signs off once it is resolved;
- a render is done only with its file: missing or empty is FAILED, no approval is UNAPPROVED, and an older revision is not complete;
- negations and hypotheticals are read, not staged;
- the document is valid JSON.

**`AakhiriChitthiAcceptanceTest` (8 tests, the Section 15 fixture)**

- Counts: characters, props, places and scenes.
- The seven lines: compared with lines read straight from the file, not through the parser.
- The environment's causes, the unsupported requirement in the register, and the wobble scheduled.
- The footwear, the stoop, the broken leg and the brick, the inferred owner, and where the places are.
- परी's steps in order (RUN, GO_TO the postbox, PLACE the bag, KNEEL, REACH_INTO, TAKE_OUT the letter, STAND_UP, STEP_TOWARD him), the "बिना आवाज" negation, and his TAKE_FROM / her SIT_BESIDE.
- The letter's chain: narrated on the ground, then परी, then रघुवीर जी, never in two hands.
- The closing scripted pull-back to a wide.
- Missing character pictures asked for; a failed render never complete.

**Regression**

- The plan digest (posture, acts, effects, weather and sounds) of all 2,100 stories in the test data was compared, old code against new.
- 7 stories change, all intended:
  - the चिट्ठी scripts;
  - बाबू के लिए gains the cycle bell its script names;
  - one story gets dust motes while sweeping;
  - one gets leaf rustle;
  - one keeps its light wind.
- The comparison also caught a broken `else if` chain introduced during this round: "dust storm" stories had gained rain and thunder. It was fixed before this note.

## 5. Not done, or not right yet (said, not hidden)

- **Kneeling.** परी kneeling is in the plan, but her front picture has no leg rig (`legs=false`), so she is drawn standing.
- **Contact gestures.** Her hand on his hand and her head on his shoulder have no gesture in the rig. The document marks them UNSUPPORTED.
- **The bag.** The bag cannot be set down: it is part of परी's picture.
- **Leaves on the ground.** Leaves sliding along the ground have no method.
- **Bench wobble.** The wobble rocks the bench picture only, not the sitters.
- **रघुवीर जी seated.** Seated front-on, he still reads a little as standing in front of the bench.
- **Flash cuts.** The technical director's motion rule cuts परी's run into 0.4 s shots. It is reported as MINOR and not changed: the rule is the protocol's.
- **Visual checks.** Steps marked IN_PLAN are checked against the plan the renderer draws, not against rendered pixels. The frames listed in `docs/testing-v47.md` were looked at by eye.
- **Not built:**
  - no multi-agent conference runs offline (the cloud conference in `ScriptAI` is unchanged and needs the user's own key);
  - no 3D rigging, lip-sync or physics simulation was added;
  - nothing here should be read as a claim of studio-level output.

## 6. Second round (17 October, later): the two new scripts, the standard format, the editor

### The user's scripts in the fixed format

Both are saved verbatim as fixtures; no existing screenplay was overwritten:

- आखिरी जोड़ी: `tools/testdata/aakhiri_jodi/aakhiri_jodi_numbered.txt`
- आखिरी चाय: `tools/testdata/aakhiri_chai/aakhiri_chai_numbered.txt`

Reader fixes found by these scripts (in `ScriptParser`, for every story):

- **Endings.** "समाप्त - समय: शाम 6:30 बजे" and "END." are the end. The first had been read as a seventh line spoken by "समाप्त - समय".
- **Names with a dash.** A numbered name with a dash and its description on the next line ("एक नई लाल जूतों की जोड़ी - आखिरी जोड़ी") no longer switches the whole list to the older one-line reading. That reading had dropped "मोची का औजारों वाला बक्सा".
- **Note before the words.** `name: (note) "words"` no longer leaves a stray comma in the note.
- **Headings in parentheses.** A scene heading in parentheses ("(दृश्य 1 — …)") is read as a heading. A direction such as "(दृश्य बदलता है)" stays a direction.

The production document:

- **Dialogue notes.** The notes with each line are now read as the speaker's own steps: "(दौड़ती हुई आती है …)" is नन्ही running in.
- **Owner in brackets.** "काला छाता (मीरा का)" gives an explicit owner.
- **Scene times.** A scene heading's time ("[शाम 7:15]") and times with minutes ("6:30 बजे") are read.
- **New steps.** GIVE, OPEN (with "बक्सा नहीं खोलते" read as a negated step), DRINK, POUR and ARRIVE.
- **Shared environment table.** The environment is read by one shared table for every story (`ENV_RULES`): rain, rain stopping, lightning, clouds, fog, snow, steam, smoke, fire, embers, lights, flicker, reflections, shadows, drips, puddle rings, flowing or receding water, a fluttering prop, smells and the street's sounds. Each requirement is either carried out (and checked in the plan), said to be unsupported, or said not to be filmable. The matrix is in `docs/engineering/animation-renderer-research-2026-10-17.md`.

Director fixes found by आखिरी चाय (in `Director`, for every story):

- **Nobody falls because water or tea falls.** "पानी बाल्टी में गिरता है" had thrown मीरा to the ground, and "चाय की धार … गिर रही" had thrown रघु काका. Someone falling *into* water still falls.
- **Sitting near something is sitting.** "भट्टी के पास बैठा" is seated.
- **Late entrances.** "तभी आरव दौड़ता आता है" and "तभी मीरा आती है" are entrances: they are not on the stage from the scene's start.
- **More lightning words.** "जोरदार बिजली" is lightning.
- **More sounds.** A rickshaw's bell, splashing steps, horns far off and a bulb's hum are heard. "पानी उफन" is moving water.

The plan digest of all 2,100 test stories, compared before and after these changes: 4 stories change, all from the added sounds.

### The standard screenplay format (`core/ScreenplayFormat`)

`format(Story)` writes the script from the reader's own reading, never by replacing characters:

- a plain title;
- the numbered lists;
- "(दृश्य N — …)" headings;
- descriptions in parentheses;
- `name: "words" (how it is said)`;
- a plain "समाप्त" or "THE END".

`validateFormattedText` checks those rules line by line and names each line. `validate(Story)` checks the story itself: a title, non-empty scenes, and every speaker in the list. `formatScript` refuses to format, and returns nothing, when the formatted script would read back with a different scene, speaker or word.

| Check on all 2,100 test stories | Result |
|---|---|
| Same scenes, speakers and words after formatting | 2,100 of 2,100 |
| Formatted text passes the rules | 2,100 |
| Formatting a formatted script changes nothing | 2,099 (the exception is an emoticon ":-(" inside a description, which the reader itself rewrites) |

The Studio has a "📝 Script format" card with the format notes for the script as written. "Show it in the standard format" opens the formatted script. "Use this script" replaces it only on the user's tap, and keeps the earlier one as `script_before_format.txt`.

On the user's own scripts as written, the rules note:

- the bare "दृश्य 1" headings (the standard puts them in parentheses);
- the note before the words (the standard puts it after);
- the `[CAMERA …]` block in square brackets;
- "समाप्त - समय: …" (the ending is the word alone).

The film is made either way.

### The editor (`ProductionDocument.editorReview`)

The editor runs offline, the same for every story. It notes what a script leaves open and never settles it; the notes are WARNING-level and never block. On आखिरी चिट्ठी it flags the five questions the protocol names:

- how the letter came to lie behind the postbox (told, not shown);
- why it lay unfound through "पंद्रह साल";
- whether "बापू के लिए" is the outside or all of the letter;
- that "बेटे" / "मुन्ना" and his handwriting exist only in the lines;
- "तुम सच में परी हो" as wordplay on her name.

The line is kept as written.

### Tests (all plain JVM, all passing locally)

New this round:

- `NewFormatScriptsTest` (3)
- `ScreenplayFormatTest` (7)
- `AakhiriChitthiAcceptanceTest` (+1, the editor)

These were added to the CI test list locally; nothing was pushed.

## 7. Third round (17 October, afternoon): physics frame by frame, the 3D maker removed, the Studio in the user's order

The user's notes on the last release: the bench was floating, परी walked through the bench, two benches appeared, the
3D maker should go, and the Studio should ask for pictures in the order the script lists them.

### Physics, checked at 24 frames a second (`ProductionDocument.physicsCheck`)

The technical director now steps through **every frame of every scene at 24 fps** in the plan. For each visible
character standing on the ground, it checks three rules:

| Rule | What counts as breaking it |
|---|---|
| Nobody sits on nothing | Seated (body SIT) with no seat under them |
| Nobody passes through anyone | Moving faster than 20 px/s across someone standing or sitting still on the same ground line and depth |
| Nobody passes through a thing | Moving across a set piece (a postbox, a cart, a bag set down) that stands on the same ground line |

A thing set further back (a cart placed *behind* someone) is on another ground line, so the walk passes in front of it.

- **How it is recorded.** Each finding is a MAJOR issue under the technical director, category `PHYSICS`, with the first
  frame number and its time.
- **Where it shows.** The document's summary gives the number of frames checked.

**The director's fix (`Director.passInFront`).** A walk or run whose path goes over the middle of a person or a thing
on its ground line is laid on a half-ellipse path towards the camera (`Film.Track`). The walker goes *in front of*
them, lower and larger, and is drawn after them. The fix runs after the technical director's other passes, because
those can lengthen a move. A move ending beside the obstacle is not changed.

| Script | Frames checked at 24 fps | Physics findings |
|---|---|---|
| आखिरी चिट्ठी with the user's pictures | 3,087 | 0 |
| आखिरी जोड़ी | 1,235 | 0 |
| आखिरी चाय | 3,033 | 0 |

In आखिरी चिट्ठी, परी's three crossings (30.4–32.2 s, 38.1–41.2 s and 47.1–49.8 s) pass in front of the postbox,
रघुवीर जी and the bench. Rendered frames at 31.0, 31.3, 39.7 and 48.3 s show it, with one bench standing on the ground.

**Not fixed.** This is a check of the *plan* (positions, paths, seats), not of the rendered pixels. It does not see a
hand going into a body or a picture's own outline.

### The bench

- **Two benches and the floating bench.** The current build draws the user's bench picture once, standing on the ground.
  रघुवीर जी's own pictures of himself on the bench are not used in any shot; the shot list says which picture each
  shot uses.
- **The likely cause.** Those pictures carry a bench of their own, and a seated picture is lifted to the seat's
  height. Together, that would draw a second bench in the air. Using them needs two changes, and **neither is done**:
  - the seat they carry replaces the place's bench while they play;
  - the picture is not lifted.

  Their pose tags are also misread. Five of the six were read as running, which is why they are not chosen.
- **Still wrong.** रघुवीर जी seated front-on, through his rig, still reads somewhat like standing at the bench: the
  thighs are hidden and the long kurta falls straight.

### The 3D maker is gone

- **Removed from the app.**
  - Studio: the "build in 3D" and "make the views" buttons, the 3D doll and 3D place buttons.
  - Film job: the 3D step.
  - Settings: the 3D toggles (ask before using a 3D picture, free 3D models, free image-to-3D demos) and the Meshy
    key row.
  - The texts that said missing pictures would be "made in 3D".
- **What happens instead.** A picture the user does not give is drawn by the studio, or made as an AI illustration if
  they allow it.
- **Not removed.** The 3D library classes (`Doll3D`, `Set3D`, `Figure3D`, `Studio3D`, `ImageTo3D`, `Glb`, the
  procedural world assets) are still in the source. No screen or film step reaches them now. `AppTest` exercises them,
  and `Studio3DArt` still holds 2D helpers the film uses: pose tags, angles, the inventory and the facial specs.
  Deleting them is a larger refactor, not done here.

### The Studio, in the user's order

The Studio page now reads, in order:

1. Characters.
2. Objects.
3. **Places and backgrounds, numbered as the script lists them.** There is one card per listed place, and every scene
   set there shares its picture.
4. Title and end pages.
5. **Reference images and videos for the director's checks, up to 18 each** (`Project.MAX_REF_FRAMES`,
   `MAX_REF_VIDEOS`).

**Not done.** The reference images and videos set the film's exposure (`FilmJob.referenceLook`). They are not compared
frame by frame with the film.

### Sheets of a character with a thing

- **What happens to a sheet.** It is kept whole in the library as a contact sheet, and its figures are cut out with
  their place on the sheet recorded (`SheetSaver`, v46).
- **A figure with a thing.** A figure that touches a thing (रघुवीर जी on the bench) stays one piece with it.
- **Not done.** The film does not yet use such a figure as "the character and the thing together" (see the bench
  above).

### Tests (this round)

**Plain JVM**

- New:
  - `AakhiriChitthiAcceptanceTest.nobodyPassesThroughAnyoneOrAnythingAtTwentyFourFramesASecond`
  - `NewFormatScriptsTest.bothScriptsPassThePhysicsCheckFrameByFrame`
  - `NewFormatScriptsTest.aWalkAcrossSomeoneSeatedGoesInFrontOfThem`
- The four new test classes (28 tests) pass.
- The whole plain-JVM suite passes on a clean copy of this tree: 80 tests.
- The plan digest of the 2,100 test stories is identical to the last baseline (the digest records postures, acts,
  effects, weather and sounds; it does not record the new paths).

**AppTest (Robolectric) corrections**

- `filmJobFinishesEndToEnd` no longer expects a 3D doll to be proposed.
- `v47StudioRequestsOnlyListedObjectsAndDeduplicatesPlaceVariants` compares the request key, not the label. The label
  has carried "(a thing of the story)" since before the test was written, so the test failed from the start.
- `aakhiriChitthiReferencePackIsBrowsableInTheBuiltInLibrary` expects the 101 stills the pack holds since 5e81c8a,
  not 51.

**Network:** one AppTest failed on HTTP 429 from Maven Central while Robolectric fetched its Android jar.

**The API layer**, against the local fake server (`tools/review/cloudtest`):

- A retired Gemini model falls back to the current one.
- With Gemini down, the text and vision requests go to the second provider within 10 ms.
- No real key is in the source. A scan for key patterns found none.

**APK:** built and signed locally. aapt2, zipalign, apksigner and dx come from Ubuntu's own packages; `dl.google.com`
stays blocked.

## 8. Fourth round (17 October, afternoon): the user's pictures with objects, the brick, every frame as drawn

The user's notes, in their words:

- "some pictures of characters may appear with objects as per the story, use them completely, don't split object from
  character";
- "one side of bench is broken, the brick should be used to fix it";
- "check every frame visually … no extra lips or eyes anywhere, no shaking or random movements";
- animals and animated objects (cars, toys) act and speak.

### A character's picture with its seat, used whole (`Art.ownSeat`, `Casting.ownSeatPicture`, `Renderer`)

**Recognition.** The user's panels of रघुवीर जी on the bench show the old man, the bench and its bricks in one piece.
Such a picture is recognised by its shape: a plank wider than the sitter across the lower part of the figure, standing
on legs at both ends, with open ground under it. The test is set by shape, not by name.

- **Detected in the user's cast:** 4 of the 110 pictures. Two front views and two back views from the bench sheet and
  upload 06.
- **Not detected:** none of the hunched or walking pose pictures (an earlier, looser test took 17).
- **Not detected either:** the four three-quarter bench panels. Their bench does not spread across the figure.

**Use.** Such a picture is a seated picture, whatever the upload's pose tag read; the old man on the bench was read as
running. While he sits alone on the script's bench:

- the most frontal of these pictures plays whole, and the bench's own picture steps aside;
- the picture is not lifted onto a seat, not mirrored (its bricks stay under the broken leg), and not raised to a
  standing face in close shots;
- its face is read from the sitter alone, above the plank, so his lips and blinks live. Read from the whole picture,
  the face finder put both eyes on one side of his face and read the front as a side;
- a thing in his hands rests at his chest.

**When the bench is shared.** When परी sits beside him, the bench in his picture is too short for two: she sat past
its end, then inside him. So those shots go back to the bench's own picture, with him on his rig.

### The brick under the broken leg (`Art.underBench`)

A thing the script sets under the bench ("बेंच के नीचे लगी ईंट") is drawn at the foot of the leg the script names. Here that
is the right leg ("दायें पैर के नीचे", "आगे वाला दायां पैर … टूटा"). The brick stays put while the bench rocks on it.

### Every frame as drawn (`FrameAudit`)

Every character in every frame is recorded as the renderer really drew it: where it stood, how tall, with which
picture, and how many faces. The record is read back after the film. Inside a shot it reports:

- a jolt: a change of speed over 2% of the height in one frame;
- a jump: a move over 12% of the height in one frame;
- a size pop: over 4% in one frame;
- a picture swapped without a cross-fade;
- a second face drawn in a frame.

The audit runs on the phone (`FilmJob`, all render threads) and on the desktop (`MakeFilm`; `AUDIT=0` skips it).
Its findings go into the shot list and into the production document as MAJOR issues in the category `FRAMES`.

### Animals act what the script says (`Film.G_ANIMAL`, `Renderer.animalState`)

An animal of the cast now acts its sentence (Hindi and English; a negated clause is not staged):

| The script says | What the animal does |
|---|---|
| जुगाली करना (chews the cud) | A rhythmic small jaw |
| घास खाना / चरना (grazes) | Head down to the grass, then chewing |
| भौंकना, रंभाना, हिनहिनाना, म्याऊं, मिमियाना (its call) | Its own call; a dog's jaw snaps with each bark, at the bark's time |
| पूंछ उठाना / हिलाना (tail) | Tail raised or wagging |
| कान हिलाना / खड़े करना (ears) | Ears flicking or pricked |
| सिर हिलाना (head) | A head shake, or a nod |
| बैठना, लेटना, खड़े होना | Sits, lies down, stands up |
| दौड़ना, सरपट (runs, gallops) | A run, a gallop for a horse |
| A spoken line | The jaw follows the voice smoothly |

The tail, ears and jaw now blend and never jump in one frame. `AnimalActionsTest` checks every frame at 24 fps against
these limits for a jump in one frame:

- jaw: 0.12 (0.6 in a bark);
- tail: 14°;
- head: 3°;
- ears: 0.5.

**Still wrong**

- On a photographed animal, grazing tips the head about 36° but it does not reach the grass, and sitting reads as a
  crouch.
- A tail moves only where the rig finds one; it found none on the stand-in dog and cow pictures.
- A dog sits with a uniform fold, not on its haunches.

### Things as characters: talking cars and toys (`Look.OBJECT`)

**Reading.** A character whose name and first described phrase say it is a thing is a thing character: a car, a train,
a ball, a teddy, a toy, a kettle, a lamp, or any rigid thing the script calls talking or alive. A driver, a doll, a
robot and a likeness ("कार जैसा") stay people.

**Drawing.** It is drawn rigid, never bent like a person or an animal:

- **Speaking:** a talk bounce. Lips and eyes only where a face was really found in its picture; with no picture, the
  studio draws one pair of eyes and one mouth.
- **Nod:** a forward tilt.
- **Head shake:** a side-to-side rock.
- **Sitting:** it settles down.
- **Moving:** a toy hops; a car or a train rolls on its wheels.

**Opening.** Rigid things open where the script says so:

- a box's lid, about its back edge;
- an umbrella, also in the sun;
- a book, into the user's picture of it open.

`ObjectCharactersTest` covers this. See `docs/object-characters-v47-7.md` for details and what is not done; among it:
the wheels in a user's car picture do not turn, and the spacing ignores a thing's width.

### A tremble that shook the frame

**Found.** The frame audit of आखिरी चाय found आरव jolting 2.2% of his height a frame at 28.8 s. A tremble moved his
whole body at 11 Hz, which a 24 fps film shows as a flip left and right every frame: a shaking video, not a tremble.

**Fixed.**

- Trembles are now 5 Hz, eased in and out.
- Rubbing the eyes, the cold shiver and the roar stay under 6 Hz.
- His largest change of speed there is now 0.9% of his height a frame.

### "स्टूल पर बैठा" sat on the street

In आखिरी चाय, "रघु स्टूल पर बैठा" and "वो हिलते स्टूल पर बैठता है" seated them cross-legged on the wet street. The
director's stool words had मूढ़ा, चौकी and बेंच, but not स्टूल, the word the script uses and lists as object 5.

- **Fixed:** they sit on stools, drawn under them.
- **Test:** `NewFormatScriptsTest.whoSitsOnAStoolSitsOnTheStool`.
- **Digest:** none of the 2,100 test stories changes.

### What the frame audit found in the three films, and what was done

Each film was rendered whole at 24 fps on the desktop, with the frame audit on.

| Film | First audit | Fixed | Last audit |
|---|---|---|---|
| आखिरी जोड़ी (1,235 frames) | 0 findings | — | 0 findings |
| आखिरी चाय (3,033 frames) | 4 jolts (आरव, 28.8–32.2 s) | The tremble at 5 Hz | 0 findings |
| आखिरी चिट्ठी, the user's pictures (3,087 frames) | 72 frames with two faces, 8 picture pops, 1 jolt | See below | See the final render's audit |

**Two faces (आखिरी चिट्ठी).** The cross-dissolves between parts laid the old picture's रघुवीर जी and परी over the new
one for a second: doubled eyes and lips.

- Now someone in both parts fades out of the old picture over the first half of the dissolve and into the new one over
  the second. The places still dissolve.
- Those frames are read as a transition, like a cut.

**Picture pops.** The picture pops were परी's side picture (walking) changing to her front picture as she stops.

- That change happens while the figure is narrowed into its turn, two frames of squash, and is how a quick turn reads.
  The audit now reads it as a turn.
- One change at 1:55.9 (front to side, as she starts to walk to the bench) is still counted.

**The jolt.** The jolt (परी, 1:56.1, up and down, 2.0% of her height in one frame) is at the limit, as she sits down.

## 9. Fifth round (17 October, evening): seated photographs, the girl's pictures, the forty daily activities

The user's notes, in their words: "check every frame visually for every story"; "daily activities should also be
checked properly as per the gifs and pictures I have provided for reference"; "check like this for all the stories
frame by frame".

### A photograph of someone sitting brings what they sit on (`Art.PoseSprite.withBackground`, `Casting.seatOfItsOwn`)

In बाबू के लिए the old man's seated picture came with its background: a grey wall, and a small bench under him. The
cut-out test for "a seat of its own" (`Art.ownSeat`) reads only pictures that are already cut out (with the
background it took a stride with a stick for a bench), so this picture was treated as a bare sitter. It was lifted,
bench and all, onto a drawn bench: one bench standing on another.

- A seated picture that came with its background is now taken to show what its character sits on. It is never lifted
  onto a drawn seat, and the drawn seat steps aside for it (`Renderer.seatInPicture`).
- Where someone else sits on a seat within reach in the shot (`Casting.seatShared`), such a picture is not used: its
  seat is one too many for two. The drawn seat holds both and the rig sits on it.
- Limit: the rig, folded onto the drawn bench, still reads as half standing (the legs go straight down; there is no
  foreshortened lap). In बाबू के लिए this is the last part, where the girl sits beside him.
- Limit: a seated photograph of someone cross-legged on the floor is also taken to bring its own seat. Where the script
  puts that character on a chair, the chair is not drawn.

### A dissolve between parts kept nobody's picture (`Renderer.pictureShotAt`)

Inside the dissolve between two parts no shot plays, so every character there fell back to its rigged front picture.
In बाबू के लिए, at 0:30, the old man left his bench photograph for the rig on a drawn bench for two seconds. The part
being drawn now plays the pictures of its nearest shot: its last as it fades out, its first as it fades in. This was
checked on rendered frames of that dissolve. No unit test covers it.

### The girl's twelve pictures were put aside (`Art.cutHairTone`)

The one-look check (v47.6: a dark-haired character's pose picture with light hair is another design of them) read the
top rows of each pose picture. For a picture with its background, the top rows are the background: the grey wall
above the girl's head read as light hair (190 against 22), and all twelve of her pose pictures were left out. A picture
with its background is now read from its cut-out figure. In this film, none of her pictures is put aside now.

Found and not fixed: in the over-the-shoulder shot at 0:44 the foreground shows the girl's face, not her back, though
her back picture is in the pack. This happened before this round as well.

### Eyes on something put their owner on the stage (`ScriptParser.possessive`)

In बाबू के लिए scene 3 ("बच्ची बेंच के पास घुटनों के बल बैठकर गुलाबी बैग खोलती है … बाबू की आँखें पत्र पर टिक जाती
हैं") had only the girl on the stage, in front of the tree with no bench and no बाबू. "बाबू की …" is read as a
possessive, and a possessive alone does not put anyone on the stage ("वानुषा का रिबन"). Now someone's eyes or gaze on
a thing ("की आँखें", "की नज़र", "की निगाह") puts them there, unless the sentence remembers or pictures them ("याद",
"सपने", "तस्वीर", "फोटो"). Over the 2,100 stories this changes one plan, बाबू के लिए's. He is now on his bench in all
five scenes (`SeatContinuityTest`).

Still missing: the girl's kneeling ("घुटनों के बल") is drawn standing, because her pictures have no leg rig.

### Animals in a park

The animal rule for "walks across" matched "पार" inside "पार्क", so every animal in a park walked off across the
scene. It now needs "पार कर", "पार हो", "के पार" or "उस पार".

### The forty daily activities against the user's references (`docs/reference/daily_activities/`)

Each activity in `gif_descriptions_40_hi.txt` was written as a short story, planned, and rendered at the moments that
matter. Two people worked on this in parallel, 1–20 and 21–40. Each frame was compared with the reference contact
sheets where one exists. The code is universal: word lists and rules, nothing story-specific.

What was wrong and is now staged (before → after):

| # | Activity | Before | After |
|---|---|---|---|
| 02 | Waking up | stood, then lay down in front of us | asleep in bed from the first frame; sits up, then stands |
| 08 | Holding hands | one got up and walked away | both seated, holding hands from their seats |
| 09 | Walking the dog | a dog stood alone, nobody walked | he walks; the dog goes a step ahead on a lead from his hand |
| 15 | Folding clothes | a reach | a fold task: a shirt in hand, about one fold a second |
| 16 | Digging | a push | a dig task: a spade in hand, a stroke every 1.5 s |
| 17 | Children playing | only the first one played | both play ("दौड़ते और खेलते हैं") |
| 21 | Making the bed | a reach, no bed | a bed from the start of the part; she reaches over it |
| 22 | Meditating cross-legged | "पालथी मारकर बैठते" put him on a chair | on the floor, eyes shut for 4 s |
| 27 | TV remote | sat, no gesture | sits, then reaches |
| 29 | Talking on the sofa | both sat, nobody talked | both talk in turn while seated |
| 32 | Taking out the rubbish | stayed in place | walks to the room's door, it opens, she goes out |
| 33 | Turning round on the spot | two short look-round turns | `G_SPIN`: one eased 2 s turn through the figure's edge |
| 34 | Three watching TV | sat only | all three seated, laughing one after another |
| 35 | Tying shoelaces | a bow | down on one knee, hands low, then up |
| 36 | Laptop | sat, no gesture | reaches at the keys |
| 38 | Watering | the row washed instead of watering | waters |

Already right and unchanged: 01, 03, 04, 07, 10–12, 14, 19, 20, 23–26, 28, 30, 31, 37, 39, 40.

Still wrong or missing, recorded rather than claimed:

- Not staged: 18 (a box onto a shelf), 13 (a fireplace), 06 (stairs). For 05 (wiping a window) the sound is a broom's.
- No rubbish bag, laptop or TV screen is drawn.
- Making the bed is arms out, not bending over the sheet. For the laces the body stays upright.
- Folding: the cloth is in one hand; there is no table. Digging: in a front view the body leans sideways, not forward.
- The dog's lead runs over its back, and the walker faces the camera (the reference is a side view).
- Holding hands: the two chairs overlap and the held hands do not read clearly.
- Lifting the umbrella, the other arm sticks out sideways.
- A front picture has no back view, so halfway through the spin the same front picture shows.
- The look-round turn still does not narrow drawn characters.

The plan digest over 2,100 stories changed where the new rules' words appear, and only there:

- 74 stories with an opening "asleep / lying" line now begin with them lying there.
- 3 joined plurals now have both people acting.
- 109 stories have laces, TV, laptop, cross-legged, rubbish, watering or conversation sentences.

After the two sets of rules were merged, the digest changed 181 of 2,100 stories: exactly the union of the two sets
(77 and 109, five in both). In one of them, the plan differs from either set alone, because the rules combine: "neo
mumbai" has two people named together with a plural verb, and both now reach at the controls.

A first run with broader words for folding and digging changed 30 more stories wrongly ("folds his hands and prays"
became folding clothes), and "आँसू पोंछता" swept a window. The words were narrowed, and those stories are back to how
they were.

## 10. Sixth round (17 October, evening): debugging every part, and the story board

The user: "debug everything, every functionality, reader, director etc and all their collaboration"; then "add one more
section in the studio section to take upto 100 images of 3x2 grid as per the story … train your director etc to check
these pictures and put rightly in place in animation or take reference of them; starting title and end image should be
only one".

### The story board (`core/StoryBoard`, Studio section 6)

- **Upload.** Up to 100 pictures in the story's order, kept as PNG so their clear background stays (`Project.saveBoardPicture`).
- **Title and end.** The first picture is the title picture and the last the end picture, one each; they become the title
  and end pages when the user gave none.
- **Sheets.** Every picture between them is a sheet of six moments, three across and two down.
- **Reading a moment.** Each figure standing apart in a moment is known as a character by its colours, compared with each
  character's own picture. Figures are read left to right; anything else is a thing.
- **Placing.** The director places the moments at the film's shots in story order: each moment goes to the first later
  shot with all of its characters on stage. The shot list gets a STORY BOARD section.
- **Checking.** The production document (category STORY_BOARD) reports a moment that could not be placed, and a moment
  whose characters stand the other way round in the film.
- **Children and grown-ups.** With two or more figures in a moment, their heights against each other are read too. A
  short figure is not taken for a grown-up, nor a tall one for a child, whatever the colours of their clothes.
  - The director knows each character's age and sex from the script.
  - It knows them in a picture by comparing with that character's own uploaded pictures: their colours, and now their
    height.
  - There is no general "boy / girl / man / woman" image recognition on the phone. That would need a vision model,
    which the app does not have offline.
- **Not done.** The moments are references the film is staged and checked against. They are not pasted over the animated
  characters (lips and eyes would stop), and the film's left-to-right order is not yet changed to follow them: it is
  reported.

Checked with a board made of the user's own pictures of बाबू and the girl (title, one sheet of five moments, end):

- all five moments were known correctly;
- they were placed at shots 1, 10, 11, 12 and 13;
- the one moment with them the other way round was reported;
- the title and end pages came from the board.

`StoryBoardTest` covers this with a synthetic sheet.

### Backgrounds: every view the script describes (Studio section 7, `Bible.backgroundViews`)

The user asked for the Studio page to "ask as many pictures as possible for background … as described in script", in a
new upload section like the earlier ones. Section 7 lists each place the script sets scenes in, with one button for
every background picture its words call for:

- the wide view of the place, empty, with the script's own description of it;
- the reverse angle, the place seen the other way round, which goes behind the over-the-shoulder shots;
- the close views, the walls, corners and floor behind the close shots;
- the place at each time of day its scenes are set at, when there is more than one;
- each thing of the story seen there, where it is in the place.

A thing counts as seen there when its name, the words in its brackets ("Ceiling Fan"), or the stem of its last word
("पंखा" in "पंखे") appears in the place's description or its scenes.

The views are saved as that place's own pictures, the same as section 3 saves them, up to 100 a place. The things are
saved as that thing's pictures, the same as section 2 and the film's waiting card. Nothing in the list is written for
one story: it comes from the script's words alone.

`BackgroundViewsTest` covers it with 2 tests: a place's views from a test script, and the name, bracket and stem
matches. The user's five room pictures (the fan from below, the clock over the sofa, the spinning fan, the clock
straight on, and a sheet of six fan and clock views) are kept in
`docs/reference/user_uploads/sofe_wali_jadu_ki_baaten/places/`.

### Your script is filmed as written; the film never waits on a sleeping user

The user's phone, at 19:05 to 19:13 on 17 October, showed three problems.

**1. The cloud review changed the script and then stopped the film.** The "independent reader" cloud review used to add
direction lines, places, things and up to 8 scenes to the story. When its report said "not ready", the film stopped with
"Production review needs resolution before rendering", followed by raw JSON with literal `\n` in it. The user said:
"no independent reader should rewrite my script". Now:

- The report is kept only as plain notes, one point a line (`ProductionPlan.notesOf`), in `production_review_notes.txt`.
- Nothing in the report is added to the story, and nothing in it stops the film.
- The progress line says "notes only — your script is never changed".
- `ProductionPlan.apply` still exists and its tests still pass, but the film no longer calls it.

**2. The film waited on the user with no end.** The user asked: "place a continue button, which should automatically be
pressed if I do not answer for 30 seconds, as I may be asleep". Both waits now go on by themselves after 30 seconds
without an answer (`FilmJob.AUTO_CONTINUE_SECONDS`).

The waits run in the film's own job, so they work with the screen off. Each screen shows a live countdown.

- **The pictures wait:** "Continue now" is a full-width button at the top of the card. With no answer, the studio draws
  what has no picture.
  - The wait holds for 3 minutes when an "Add a picture" button is tapped.
  - It holds another 30 seconds each time a picture arrives.
  - It does not count down while the film is paused.
- **The shot check:** with no answer, the shots are approved as planned.
  - "Wait — I'm still looking" holds the wait for 5 minutes.
  - Each fix the user taps holds it another 30 seconds.

**3. Back went straight home.** Back on the making screen, or on the shot check, now opens the Studio while the film goes
on, and the next back goes home. A film that could not be made now offers "Back to the studio".

`ProductionPlanTest.theReportIsPlainNotesForTheUser` covers the plain notes.

### What four debuggers found and fixed (each with tests; all merged)

- **The app.** Nine fixes:
  - project settings written by two threads at once were lost (writes are now whole-file and atomic);
  - codec, microphone and stream leaks on failure;
  - a crash on a speaker missing from the cast;
  - a half-made film left behind after a stop;
  - the "ready" notification missing for a second film;
  - a backup restore stopped by one bad file;
  - a dead gallery link shared;
  - a broken reference clip kept;
  - a "please wait" dialog stuck after a screen change.

  `AppAuditTest`.
- **The reader.**
  - The script's own place line ("स्थान: …") was dropped, so बाबू के लिए and आखिरी चाय lost their setting. Its time line
    ("समय: …बारिश…") was dropped too.
  - People next to someone ("माँ के पीछे") were not put on stage.
  - On-screen children called "बेटा" were flagged as unseen people.
  - A device's voice was a BLOCKER.

  `ReaderAuditTest`.
- **The director.**
  - Teleports from two rules' keys at the same moment (the sample story's witch, 1,096 px in a frame).
  - Walks re-issued mid-move restarted from standstill.
  - Walks too fast for a body: the least time is 1 height a second walking and 1.8 running.
  - Arrivals on top of someone; a path running on past a cut.
  - Stars under rain.

  `DirectorAuditTest`.
- **Casting and drawing.**
  - Named back and side views were silently dropped ("back" read as a number): the over-the-shoulder shot showed the
    girl's face.
  - The shared bench now keeps बाबू's own bench photo.
  - A folded umbrella was drawn across the boy's face.
  - Changing anchor (branch, shoulder, face) now travels eased: the sample story's monkey dropped 467 px in a frame.
  - The frame audit reads only characters inside the frame.

  `RenderAuditTest`.

### What remains (recorded, not claimed)

- Getting into a car still cross-fades the rider into the seat (a 115 px jump of the drawn base).
- One director change of this round was taken back: "a vehicle on a ride gets no path". Without that path the rider of
  a car was drawn into the seat with a jump of 40% of their height (the frame audit on v44forge story 00860, caught by
  the end-to-end sweep after the merge).
- A jump has no crouch before take-off.
- The girl's kneel reads as standing. The sit-down jolt of परी (2.0%, at the limit) remains.
- In medium shots of the shared bench the girl is placed in front of बाबू, so it reads as two benches.
- 528 "passes through someone" plan findings remain, mostly at playground apparatus, car doors and cars driving past at
  the same depth.

## 11. Seventh round (17 October, night): voices, six pictures per character, the sofa story, never stop

The user: voices must be heard with lip sync; smooth movement and scene changes; characters face each other when
talking; less blur; group pictures of characters (or a character with a thing) are used whole; six pictures per
character; references up to 18; one title and one end picture; the roles learn from each film; never ask for things the
script does not name; the director must never just stop, with a Continue the user can press.

### Voices

- **Desktop.** The published desktop films of earlier rounds had no voices: `espeak-ng` was not installed on the build
  machine. `MakeFilm` now prints `VOICES: X of N lines spoken` so a silent film cannot pass unseen. The sofa story:
  9 of 9 lines spoken.
- **Phone.** `Voices` tries the phone's default engine, then Google's, then the others. When Hindi is not available the
  line is spoken in Latin letters (`Txt.romanize`). The film page names the engine used. When every line fails it says so
  and makes the film anyway. Not tested on a phone here (NOT RUN).
- **Dialogue in quotes.** `"A" (manner) "B"` is two lines; the middle parenthesis is the manner of the first. A story
  with no dialogue is narrated from its direction sentences instead of failing.

### Reading boys and girls

- "मुस्कुराते हुए" and other "ते हुए" manners no longer count as male. Kinship words count only between two people in a
  scene, and an elder's word is never given to a child. Body and skin words are removed before counting. Ponytails,
  plaits, hair clips, anklets and "साल की" count as a girl.
- Corpus digest: 24 girls read rightly that were read as boys before, no new mistakes (`SofaStoryReaderTest`).

### Studio

- **Six pictures per character.** Front, back, two sides, two three-quarters: one tile per view, plus "All six at once".
  A picture uploaded to a tile is that view and is never split.
- **Group pictures kept whole.** Two characters side by side, or a character with a thing, stay one picture
  (`Angles.composite`, `CompositeSplitTest`).
- **Limits.** References up to 18 (things and places); characters 6.
- **Places with no scene of their own** get their own picture button. The waiting card asks only for characters and
  places: things the script names are drawn from the places' and library pictures and never hold the film.
- **Continue.** A failed film shows "▶ Continue — make it again"; it presses itself after 30 seconds, at most once every
  10 minutes. The reader only advises; it never rewrites the script. Back goes from the making page to the Studio, then
  Home.

### The sofa story (सोफ़े वाली जादू की बातें)

- **The user's sofa.** `Art.findSeats` finds the sofa in the room picture by the sofa's colour (from the thing's picture,
  or the colour word in the script). The girls sit on that sofa at its scale; the drawn sofa and the floor props are not
  added. The cushion and the book go on the seat, "बीच में" between the sitters.
- **Sitting together.** A second sitter on "the sofa" sits beside the first, on the side the script names. Two people
  sitting together hug in place.
- **Facing.** In a shot the seated view (side or front) is chosen once, not per line, so heads do not turn back and forth.
- **Scene changes.** Title to first scene and last scene to end now dissolve.
- **Sharper.** Encoding bitrate raised (0.16 / 0.28 / 0.40 bits per pixel).
- **Frame audit of the full film** (1852 frames, 3508 character-frames): jolts 0, jumps 0, size pops 0, picture pops 0.

### Render refinements (the user: "render should also be refined to get pixer level film")

- **No purple edge.** The user's pictures come cut out from a magenta ground. Their clear pixels keep the magenta, and
  the hair kept a purple cast 5 to 10 pixels in from the edge, drawn larger and sharpened in the film. The key colour is
  read from the clear pixels. The edge is unmixed from it, and the cast is taken off a band of the edge, but only where
  the figure's own colour further in has none: a lavender shirt keeps its colour (`Cutout.unkey`, `Cutout.despill`,
  `KeyFringeTest`).
- **Sitting into the sofa.** A soft shadow on the seat round the hips and a fainter one on the back cushion behind the
  body. Without them the girls sat pasted on the photograph.
- **Hands on a thing held.** Shaded and lit from above, with a shadow on the thing and the knuckles' creases. They were
  flat rounded blocks of skin colour.
- **An open book on the lap** of someone seated, not at the chest (it covered her chin in a close-up). It reads as a
  book: the user's cover shows round the pages, the leaves rise from the spine in a curve and the fold is in shade (it
  was two white rectangles).

- **Volume on drawn figures** (the films with no pictures). A figure the studio draws was flat cut paper. With
  `Gfx.shading` on (`Puppet.draw` turns it on), every solid colour it is filled with gets light from above and the left:
  an oval like a ball (highlight up and left, rim in shade), any other shape like a standing column (lighter at the top,
  darker at the bottom, warm shadows). Shadows, glows and tiny details stay as drawn. The phone and the desktop use the
  same numbers (`core/Shading`, `ShadingTest`). Drawn figures are still simple shapes: this gives them roundness, not
  Pixar's modelling.

### Things, as a person reads them (`SofaThingsTest`)

- **A two-word name said short.** "भूरा टेडी बियर" is "टेडी" all through the script. The word before the head noun
  names the thing when it is a noun itself (no colour, size, age or number) and no other thing has it. Before, the
  teddy was never staged.
- **"गोद में" (in the lap)** means held by the sitter: "गोद में खुली किताब" is open in her hands from the start. The lap
  named after the thing counts only with a verb of keeping ("टेडी को गोद में कस कर पकड़े हुए"), so the sofa she sits on
  is never taken into her hand.
- **The things a line's manner handles** are staged as the line is spoken: "किताब धीरे से बंद करके" closes the book,
  "टेडी को गोद में कस कर पकड़े हुए" puts the teddy in her arms. This only applies when the user gave pictures of the
  things; stories without them are unchanged.
- **The fan's wind in the pages** ("पंखे की हवा से किताब के पन्ने हल्के हिल रहे हैं") opens nothing.
- **Already in the picture.** A thing of a clear colour that the room's picture already shows on its sofa (the red
  cushion behind the girl) is not pasted over it a second time (`Art.shownOnSofa`: its hue within 15°, the room seen
  under white light). A brown teddy or a grey thing is never taken for part of the picture.

### The roles learn (`core/Lessons`)

After each film the phone keeps a small notebook (`lessons.txt`). It counts the director's fixes, the shots, the rough
frames and the voices spoken. A film with many rough frames makes the next one's moves slower (down to 0.7). Many
fixes per shot make the next one's shots longer (up to 1.5×). The notes are shown on the film page (`LessonsTest`).
It does not learn from the story's content.

### What remains (recorded, not claimed)

- The sofa is found only by its colour in a picture where it is the widest patch of that colour. A sofa of the wall's
  colour is not found; the drawn sofa is used.
- Edge voices could not be reached from the build machine (BLOCKED, proxy 403); desktop films use espeak-ng, which is
  robotic.
- Lip sync is mouth shapes timed to the voice's loudness, not to the sounds.
- This is cut-out animation of the user's pictures, not Pixar rendering.

## 12. Eighth round (18 October): acting, blinks, the place's light, the open sound library

The user: "you can take one more day today but output must be pixer lever"; asked whether to add an outside AI-video
service, the user chose the built-in engine only (nothing leaves the phone). Then: "save openly available sounds and
voices in library".

### Acting under the words (`Renderer.acting`, `ActingTest`)

Measured first: a speaker's head moved by under half a degree and the body by nothing, so the pictures read as frozen
with only the lips moving. Now, on a picture with a body rig:

- **A key pose for every phrase** (1.6 s): the head tilts 2° to 4.5°, mostly across to the other side from the phrase
  before. It moves into the pose in 0.45 s with a small overshoot (about 6%) and holds it.
- **The stressed syllables** (the voice's loudness above the third of a second before it, averaged over a quarter of a
  second, so it follows groups of syllables, never each one) bring a small nod and lift the brows.
- **The speaker leans into the words**; the one listening nods now and then as the other's phrases end.
- **Everyone breathes** (60% deeper than before) and shifts their weight slowly.
- **A poised character acts less.** Everything is a function of time only, so frames drawn in parallel agree.
- Measured on the sofa story: the head swings between −3° and +3° from phrase to phrase, at most 1.5° in one frame
  while it moves.

### Blinks

They came every 3.9 s exactly and were all on or all off. Now they come at uneven intervals (2.2 s to 5.2 s, the same
for the same character) and as the head moves into most new poses. The lid comes down in 0.07 s, stays shut to 0.12 s
and opens by 0.25 s; on the user's pictures it descends over the eye instead of appearing.

### The light of the place (`Renderer.pictureLight`, `Gfx.light`)

The pictures of people are lit in plain studio light and the place's picture in its own (a room's warm evening, a
night's blue), so the people sat on the place like stickers. The people and things from the user's pictures are now
drawn in the place's light: every channel is multiplied by the picture's average colour, a third of the way from
white, and a dark picture dims them by at most 16%. A drawn place leaves them as they are. The phone uses a colour
filter, the desktop the same multiply in its mesh rasteriser.

### The camera stays a tripod

A slow push-in on dialogue was considered and left out: protocol P3 of the technical director's document keeps every
shot on a locked tripod (moves made the cut-out pictures wobble). The acting carries the shot instead.

### Open sounds in the library (`SoundLibraryTest`)

81 recordings from Moodist (github.com/remvze/moodist, an MIT-licensed project; its sounds are CC0 or under the
Pixabay Content License, per its README) are now bundled. Among them: a ceiling fan, boiling water, rain on an umbrella,
a window and a tin roof, cows, chickens, crows, a barking dog, a horse, a temple, a village at night, a train, a rowing
boat, a busy street, fireworks. They are mono Vorbis cut to 40 s with soft fades (19 MB in all), and each is indexed with
English and Hindi words: the sofa story's "पंखा धीरे चल रहा है और हल्की घर-घर की आवाज" finds the fan. Each entry
names its licence, and `sounds/CREDITS-moodist.txt` records the source. Words the old library already answered keep
their old recording.

### Not done (said, not hidden)

- **Open voice recordings**: BLOCKED. Every open speech collection (OpenSLR, Mozilla Common Voice, archive.org,
  Wikimedia, Hugging Face) is unreachable from the build machine, and no voice was added without a licence that could
  be checked.
- **A better desktop voice**: the MBROLA Hindi voices were tried and left out. Their diphone sets lack many consonant
  pairs (न्द, स्त, ग्ल) and leave gaps inside words. Desktop films keep espeak-ng; the phone uses its own speech
  engine.
- **Pixar-level**: this remains 2D animation of the user's pictures on a mesh rig. These changes make it act and sit
  in its light better; they do not make it a 3D film.

## 13. Ninth round (18 October, later): the pages, the camera, lips on every side

The user: "recheck your studio page and film making page as well they are very cluttered and not upto the mark have
bugs, no continue button nothing"; "the films you are created are not animation, no motion, no lipsync no camera
movement"; "also use freely available voices from GitHub … and save them in our library"; "hard code their api in apk".

### The pages, seen as the phone shows them (`UiShotsTest`)

A new test opens every page in a phone-sized window, draws it whole into `build/ui_shots`, and checks it.

- **A real bug: invisible buttons.** `Ui.small` gives a button no width of its own and a share of its row; in a column
  that share is of the height, so the button was 0 pixels wide. The six "+ Front / + Back …" places of every character
  could not be seen or tapped, and the film page's "📷 Add" rows were blank. A column now takes such a child at full
  width (`Ui.Column`), which mends every one of them.
- **The Studio** was over 12,000 pixels tall (more than fifteen screens, 106 pieces of text), with Make film at the
  very bottom. Now:
  - Make film is at the top, under a short line saying what is ready and that everything is optional.
  - Then the characters, the things, the places, and the title and end pages, in the user's order.
  - The helpers, the extra views of each place, the references, the story board, the director's plan and the script
    format are folded under "More". They are built only when opened, and stay open after an upload. The director's
    plan used to be worked out every time the page opened.
  - The empty picture places are tappable and their names read in full ("Left side", "¾ right").
- **The film page** listed every character, thing and place twice under the progress. Now there is one button to the
  studio. A stopped film loses the "keeps being made" note.
- **Continue after the app was closed.** A film is marked as being made when it starts; the mark goes when it is done
  or stopped. If the app was closed (or the phone closed it) in between, the film page says so and offers Continue,
  which presses itself after 30 seconds once in ten minutes. The home page shows "Continue making your film" first.
  Before, the page fell back to the story with no Continue at all.

### The camera moves (the user's word over protocol P3)

Each held shot makes one slow move from cut to cut, eased out of the cut and into the next:

- a face or a pair talking: a push in, 6% closer by the cut (4% on a wider shot);
- a wide: a drift across of 4% of the frame, alternating in direction, with a slight push.

While the frame moves, nothing is snapped to its pixels. Scripted moves stay as written.

### Lips on side and three-quarter pictures (`Cutout.sideMouth`, `LipFinderTest`)

- **Why side views were silent.** A side or three-quarter picture's mouth point was on the cheek or the jaw, so the
  jaw was kept shut. The user's side and three-quarter pictures, loaded as views, had no face read at all.
- **The fix.** The mouth is now found by the lips' colour: redder and darker than the lighter half of the skin
  (darker than a blush), the widest mark at least 1.4 times as wide as tall, never a large round blush or a nostril.
  A point the face reader gave is kept when there are lips under it. Only a mouth really found opens; nothing is
  guessed.
- **On the sofa story's eight side and three-quarter pictures:** the lips were found on five, at the lips each time,
  and nowhere wrong. The little one's left side and her two three-quarter views (very faint lips) stay closed.

### Gestures

In about half of a speaker's phrases one hand comes up and out with the words, more on the stressed syllables, and
goes back as the phrase ends — never while the hands hold something.

### Expressions and pacing (the user: "no expressions, just copying pictures … and joining them")

Checked on a test of one girl speaking five lines in five feelings, close up:

- **Faces.** The feelings were there but faint. A picture's face now plays each feeling at 1.25× its old strength: a
  smile with soft eyes, sad inner brows, lowered angry brows, wide surprised eyes. At 1.45× the smile and the anger
  went lopsided: the eyes are found on a picture only roughly, and a full squint closed one eye and not the other. So
  a squint is capped at half.
- **Body language.** The slump of sadness, the lean of anger and the step back of fear play at 1.6×. The gestures
  with the words are about twice as large.
- **Pacing.** The title page held 7.5 s and the end page 8 s of a 35-second film. Now they hold 4 s and 5 s; a title
  read aloud still keeps its line.

### Blinks, corrected

The eyelid painted half way over a photographed eye read as a flat, sleepy line across the iris. The lid is drawn
again only while the eye is mostly shut, a little larger and wider than the eye found; the uneven timing stays.

### Voices from GitHub, and keys in the APK: not done (said, not hidden)

- **Voices: BLOCKED.** Only raw files on GitHub can be reached from the build machine (its search, API and release
  downloads are refused). No package found on the reachable registries (npm, PyPI) carries a Hindi voice of its own;
  the voice packages fetch their models from Hugging Face, which is unreachable, and the Edge voices are an online
  service under a non-commercial licence. No voice was added whose licence could not be checked.
- **Keys in the APK: not done.** The sounds bundled need no key, and no key to a paid service exists here. A key put
  inside an APK published on a public release can be read out by anyone and used against the account; a key the user
  gets can be entered in Settings.

## 14. Tenth round (18 October, later still): walking where the script says, the mouth on the lips

The user: "no expressions, just copying pictures … leg and body movements". A test film of one girl in a room (five
feelings and a walk), looked at frame by frame, showed three faults.

### A walk to one side (`Director.side`, `WalkSideTest`)

"(वृंदा कमरे में दाईं तरफ चलकर जाती है)" was not read as a walk at all: she stood still while the sentence went by. The
walk words now include "चलकर जा", "चलते हुए जा", "तरफ चल", "ओर चल", "walks to the right", "crosses the room", and the walk
goes to the side the sentence names. "दाईं" is the right, "बाईं" the left, in every common spelling. "She left" and
"right now" name no side. The same reading places someone on the side of a sofa. On the frames she turns to her side
view, strides across with her legs swinging, the camera follows her, and she turns back to the front.

### The mouth on the lips, not on a cheek (`Art.mouthFromPicture`, `LipFinderTest`)

As she spoke, a dark slit opened on her cheek. The face reader had put her mouth correctly under the middle of her
eyes. Then the older "lips by colour" check moved it onto her deeply blushed left cheek, beyond her eye: her thin,
skin-coloured lips showed less lip colour than the blush did. Now lips found beyond an eye are taken only on a face
turned that way. On a face seen from the front the point stays, and the lip line between the eyes is used when it is
found. On the frames her own lips part as she speaks.

### An animal's ears (`AnimalActionsTest`)

A dog's idle ear flick and the flick of a scripted ear act ("कान हिलाता है") added together and jumped 0.65 in one
frame. The shorter title page moved every frame to a new time, so one frame now landed where both peaked. The idle
flick now gives way while an ear act plays.

### The pages' tests follow the new pages

`AppTest` still looked for the studio's old open lists ("Still missing", the director's plan) and for the story page's
missing-pictures card. It now opens the studio's folded sections the way a user taps them. It uploads through a
character's own picture place, which replaced that card.

## 15. Eleventh round (18 October, evening): mouths that open, lids that close, no jolts

The user: "correct everything … pixer level". Each fault below was found on frames of the films, fixed in the
universal code, and checked again on the same frames.

### Lips on every side of a brown face (`Cutout.sideMouth`, `LipFinderTest`)

The little one's three-quarter pictures had no lips found, so in those views her mouth never moved. Her mouth is a
darker line of her own skin's colour, no redder than her brown skin, and the lip finder only took marks redder than
the skin. Now a long, flat, dark line right at the face reader's own point confirms the mouth. It is never accepted
anywhere else on the face, so brows and lashes stay out. All her views now find their lips except her back, where
none is correct.

### The inside of an open mouth (`Renderer`)

The mouth opened as a flat, near-black oval that looked pasted on. Now it has the shape of an open mouth (flatter
under the upper lip, rounder above the lower one), a deep red inside, and the tongue low in it once it opens wide.

### A blink's lid the size of the eye (`Art.eyesFromPicture`, `Cutout.eyeExtent`)

On the little one's big cartoon eyes the closed lid was smaller than the eye: the white and the lashes showed round
it, and its flat colour read as a patch. Now each eye's own extent is read from the picture (out from its point to
where the skin begins, at most 1.9 eye radii). The lid is centred on that eye, drawn its size, and shaded from the
skin under the brow to the cheek's, both sampled from the picture.

### No jolts in आखिरी चिट्ठी (`FrameAudit`)

- **Sitting down into a side picture.** The front picture changed to the side picture in mid-shot. The figure now
  turns through the change, as it already did onto a bike.
- **A shorter sitter on a shared bench.** She was lifted to the bench's height along a straight ramp over part of
  her sitting down. That ramp started and stopped at full speed. Now the lift is eased over the whole movement.
- **A hurried move across the stage.** At over five steps a second the step's rise and fall came every two or three
  frames: a shiver. That rise now fades out at such speeds; the legs still stride.

### Tears dry (`TearsDryTest`)

Tears came with a sad line and stayed for good: they ran on through her surprise. A line of another feeling now dries
them. Another sad line keeps them.

### Checked, nothing to change

- **Arms.** In medium shots her arm visibly swings out with the words. The rig has no elbow, so a forearm cannot
  rise to gesture; adding one would need new mesh regions and risks tearing the picture. That is not done.
- **बाबू के लिए.** The girl kneels (lowered onto her knees), and only his photo's own bench shows in the two-shots.
  The earlier notes on both were out of date.

## 16. Twelfth round (19 October): new stories made end to end, the phone's own drawing checked

The user: "debug again … check everything is working fine and producing … film from script". Three stories the engine
had never seen were written and made end to end, and their frames were looked at. One was in a park with the user's
pictures (नीम के नीचे), one in English with no pictures at all (The Rainy Day Dog), one in a Hindi village (गौरी की
घंटी). Every fault below was found on their frames and fixed in the universal code.

### The phone's own drawing (`PhoneFilmTest`)

No test had made a film the way the phone makes it. Now one does: a new story with two of the user's pictures from
the test data, read by the phone's own loader. Every frame is drawn by the phone's AndroidGfx with the film look, and
the frame audit reads it. Result on 858 frames: frame audit clean, faces living in every frame, lips moving through
every spoken line. One frame a second is kept.

### Faults found and fixed

- **A palace in a park.** Every park was the palace garden, and every drawn title and end page had the palace too, so
  a story in a park of today had a palace behind its title, its scenes and its end. A park or playground is now a
  garden without the palace (`Sets.D_PARK`). The title and end pages show the palace only in a story of palaces.
- **White dashes in the sky.** The palace garden's waterfall shimmer was drawn even where no waterfall was. It now
  goes with the waterfall.
- **"(उदास होकर)" played calm.** Sadness was read only from crying. Plain words of sadness now play a sad face
  (उदास, दुखी, निराश, मायूस, "sadly"), and so do worry (चिंतित, परेशान, "worried"), annoyance (नाराज़, "annoyed"),
  surprise ("surprised", "shocked") and gladness (उत्साह, "happily").
- **Tears for every sadness.** Any sad line brought tears. Tears now come only when the script has the character cry
  (रोते, रोकर, सुबक, आँसू, आँखें नम, "cries", "sobs", "weeps"); a sad line without them is a sad face, dry
  (`TearsDryTest`).
- **The puppy drawn as a man.** "Bruno (dog)" with "(a small brown puppy)" under it was read as a name with a role and
  an empty description. A character named as an animal is now that animal, and keeps its own name. A robot dog keeps
  its role.
- **Arrivals.** A character first named just after a door is opened (or knocked, scratched or rung at) comes in
  through it. One whose first line is said "(दौड़ते हुए आता है)" runs in with it. Both used to be there from the start.
- **Lifted up after a fall.** मोहन slipped and fell, and got up by himself at once, so when दादी went to him and
  "उसे उठाती हैं" she lifted no one, and her own "उठकर" (getting up) stood him up too. Now one who falls stays down
  when what follows has someone lift them up, and stands once that one has come to them. With no one coming they get
  up by themselves, as before (`FallHelpTest`).
- **A puppy that leapt.** Coming in, every walk-on took a fixed time whatever its length and the walker's size: the
  puppy dashed in at four of its heights a second. A jump rose 80 units whatever the jumper's size, thrown off the
  ground at full speed. The steps' rise was the same 6 units on a puppy as on a man. Each now goes with the size of
  the body. A jump eases off the ground and onto it, and leaves the steps' rise behind it.
- **Everyone wet indoors.** Once rain began anywhere in the film, everyone on screen was drawn dripping, including
  the family who had sat dry inside the house all along. Indoors, only one who came in from the rain while it fell is
  wet now (the puppy at the door). The room's window shows the rain outside.
- **Into her arms.** "Bruno … jumps into Maya's arms" left the puppy standing beside her. One small enough (up to
  0.65 of the holder's height) who is taken into a standing character's arms — "jumps into Maya's arms", "Papa picks
  up Riya", "मोती दादी की गोद में कूद जाता है", "माँ मुन्नी को गोद में उठा लेती हैं" — now comes to them and is held in
  front of their chest (`Film.A_IN_ARMS`), going where they go, until the part ends or they are put down ("puts Bruno
  down", "मुन्नी को नीचे उतार"). A ball picked up near the puppy leaves the puppy where it is (`InArmsTest`).
- **A puppy the size of a dog.** "A small brown puppy" was drawn as tall as the girl's chest. A young or small animal
  (puppy, kitten, cub, calf, पिल्ला, बछड़ा, मेमना; small, little, छोटा, नन्हा) is now drawn at 0.7 of its kind's size.
- **Steps and hops in proportion.** The hop into the arms is paced like a run of its length (over 0.6 s the puppy
  went 12% of its height a frame). A step's rise and fall is kept low enough that its turn at each step changes the
  speed by under 1.5% of the height a frame — a small body's quick steps jolted. A run's pace is 1.6 heights a second
  (1.8 eased up to 11% of the height a frame, at the edge of a jump).
- **Faces on chests and knees.** Every one of the user's pictures was drawn with the eyes and the mouth the studio
  found in it, and the face finder took the old man's beige kurta for skin: in most of his pictures of sitting and
  bending the "face" was on his chest, his knees or his back, and a living face (blinks, lips) was laid there. A face
  read from a picture of a pose is now animated only when it is a face — closely like the main picture's face, or else
  skin of a face's hue round the eyes, the lips found under them by their colour and only a forehead and hair above
  them — and its mouth is under its eyes (not at a hand, a collar or a foot). Turned faces speak only with their lips
  found (a mouth guessed under the middle of the "eyes" lay on the cheek, one "eye" on the ear). Checked by eye on all
  84 pictures of आखिरी चिट्ठी and the pictures of the other stories (`FaceTrustTest`). A picture whose face is not
  animated is drawn as it is, and the casting prefers a living one wherever it fits.
- **Faces found by a trained face detector.** Reading faces from the pictures' colours could not be made reliable
  (the kurta's beige passes for skin; a hand beside a turned face looks like an eye and a mouth). The studio now runs
  YuNet — the face detector of OpenCV's model zoo (MIT licence, 230 KB, `assets/models/face_yunet_2023mar.onnx`) —
  through ONNX Runtime 1.20 (the same Java API on the phone and the desktop; its Android libraries for arm64 and armv7
  are packed into the APK). It gives the two eyes, the nose and the two corners of the mouth of every face it sees. Of
  the faces it reports at a score of 0.6 or more, the largest is the figure's (a hand held up beside a turned face, or
  the knob of a stick, scores as high but is smaller). A face it found keeps its mouth where it saw it; a picture of a
  pose where it sees no face (a back, a face hidden in the hands) gets no living face. Without the model or the runtime
  the studio reads faces as before. Checked by eye on all 84 pictures of आखिरी चिट्ठी, the 12 of the sofa story and
  the 6 of भाव: the eyes and the mouth are on the eyes and the mouth in all but three (the little girl's two near
  profiles, where the mouth point sits back on the cheek, and the old man wiping his mouth, where it is on the hand).
  The skeleton model of the same zoo (MediaPipe BlazePose) was tried too: right on the old man's whole figures, wrong
  on the little girl (her big head and short body put its shoulders on her face) — not used (`FaceModelTest`).
- **A little girl's arms that never moved.** The rig decides from where the hands end whether they are held up
  (a tray, a toy: kept still) or hang (swung as she walks and gestures). Where the arms were not seen apart from the
  body that end is guessed from an adult's proportions, and the little girl's hanging arms read as held: she never
  moved them. On a small child (a head over a quarter of her height) a guessed end no longer holds the arms still.
  Checked on the rig sheets of परी and the toddler of the sofa story (pointing, walking, angry).
- **Narrator voices in the library.** The studio's narrator voices now ship in the APK as a library with their
  metadata (`assets/voices/narrators.json`, 13 voices: seven Hindi — दादी माँ, नाना जी, कथावाचक, गंभीर सूत्रधार, माँ,
  चुलबुली दीदी, बाल कथावाचक — and six English): names in Hindi and English, language, gender, age, tone, pace, what
  each suits, a sample line, and how it speaks — offline by the phone's speech engine at its pitch and pace, online by
  its neural voice. They are listed in the library and in the narrator's (and every character's) voice picker, with
  a ▶ to hear the sample. No recording is bundled (every licensed source of narrator recordings was out of reach;
  none was made up); a recorded voice from the user's library can still be chosen. The voice chosen for the narrator
  was saved and never used — the film always spoke with the default narrator; it is used now (`NarratorVoicesTest`).
- **The body's joints (MoveNet).** A trained pose model (MoveNet Thunder, Apache 2.0, 12.6 MB in the APK) finds the
  shoulders, elbows, wrists, hips, knees and ankles in every rigged picture. The rig's hips and knees are put at the
  joints — a long kurta hides them from the outline — so a seated picture folds where the legs bend; the forearm
  bends at the elbow (a gesture, a cup or a plate held up) where both arms hang free; a side picture looks the way its
  nose is ahead of its ear. Checked on rig sheets of परी and रघुवीर जी (sitting, walking, the new "explain" and "hold
  up" bends) and by `PoseModelTest`.
- **The lips and the eyelids (FaceMesh).** 468 points of the face (MediaPipe FaceMesh, Apache 2.0, 3 MB) inside the
  face the detector found: the mouth on the opening between the lips, a mouth already open in the picture never opened
  further, each eye's lid sized to its corners and lids, and no lid painted over eyes already shut. Checked by eye on
  all the old man's pictures; profiles are left to the detector alone.
- **The narrator speaks only what the script gives the narrator.** A story with no dialogue was read aloud by the
  narrator in its own words; now it is acted out from its directions with no narration, and only lines written for
  the narrator (कथावाचक: "…") are spoken (`NarratorOnlyWrittenTest`).
- **The flying bench.** In the films sent earlier, the old man's bench hung in the air beside परी in the two-shots of
  बाबू के लिए (0:48–1:02): his seated picture was lifted to put its face where a standing face would be. A seated
  picture stays on its own legs now (the fix below); checked on the same frames.
- **What the face detector changed, checked frame by frame.** बाबू के लिए rendered with the detector showed three
  faults the old readings had hidden, all fixed and checked on the same frames (24, 38, 62, 90 and 110 s):
  the seated old man rose out of the top of the frame with his bench — a seated picture, now with a face, was lifted
  to put that face where the standing one's is; it stays on its own legs now, as in a wide shot (`Renderer`). The
  over-the-shoulder shot showed his knees instead of his face for the same reason; it shows his face again. The
  sheets' big-headed poses have their eyes further apart than the slender front picture, so परी and the old man
  shrank to three quarters of themselves from one standing picture to the next; a whole figure on its feet is the
  character's own height now (`Art.sizeBy`). And परी sat on the bench with her legs hanging straight down — the
  detector's two eyes of a profile lie almost on one another, and the side view's rig sized by them folded her
  wrongly; a side view's face is read as before (`Art.makeSprite`).
- **Blinks behind glasses.** The old man's eyes are read at the lower rims of his glasses, and his lids closed there.
  No lid is painted behind glasses now.
- **The audit and the camera's cut.** The camera's own cut can fall a frame away from the shot list's start. Two frames
  with a camera cut between them are now two shots for the frame audit, not a character jumping.

### Results

| Story | Frame audit | Voices |
|---|---|---|
| नीम के नीचे (the user's pictures, a park) | 0 findings | 4 of 4 |
| The Rainy Day Dog (drawn, English) | 0 findings (it had a jump and two jolts) | 5 of 5 |
| गौरी की घंटी (drawn, a village, a cow, rain, a fall) | 0 findings | 4 of 4 |

### Not fixed

- The puppy "jumps into Maya's arms" and lands beside her on the sofa: held animals are not drawn.
- An old man's picture whose own face is sad smiles only a little: the picture's face is where the acting starts, and
  his moustache hides his mouth.
- The drawn people and places are simple shaded shapes.
