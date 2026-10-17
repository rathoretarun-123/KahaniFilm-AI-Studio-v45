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
