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
