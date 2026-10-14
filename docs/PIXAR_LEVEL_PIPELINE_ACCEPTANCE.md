# Film-quality pipeline: acceptance contract

This document is the product contract for the next animation-engine iterations. It does not claim that these capabilities are already complete.

## Product goal
Given a script, character/prop/location reference images and optional user-owned audio, produce a coherent 3D animated film with persistent identities, story-aware direction, natural performances, consistent staging, accurate dialogue timing, cinematic framing and reliable export. A successful APK build is necessary but not sufficient.

## Required pipeline
1. **Ingest and provenance** — index reference images, sound effects, ambience and music; retain source path, licence/provenance, dimensions, detected subject and user tags. Prefer user assets; use only legally reusable public assets and record source/licence. Never silently download or embed copyrighted material.
2. **Story reader** — parse scenes, beats, dialogue, action, emotional turns, character goals, relationships, continuity state, time/place and required props. Report ambiguities rather than inventing critical plot facts.
3. **Visual bible** — lock character identity, proportions, face landmarks, materials, costumes, prop dimensions and environment layout across shots. Use uploaded references as primary truth. Store canonical IDs and image-to-asset mappings.
4. **Director and storyboard** — produce a shot plan with objective, emotional intent, shot size, lens, blocking, screen direction, eye-lines, camera path, lighting, transitions, dialogue coverage and continuity constraints. Check the plan before rendering.
5. **Asset pipeline** — use imported rigged 3D assets when available; use reference-guided generated/procedural geometry only as a fallback. Rig humanoids, quadrupeds, birds and articulated props with explicit joints, limits, bind poses and attachment points. Environments need scale, walkable ground, collision/placement anchors and lighting.
6. **Animation** — use time-based animation independent of frame rate; eased motion curves, anticipation, follow-through, foot/contact constraints, gaze, blinks, breathing and secondary motion. Interpolate transforms and facial controls continuously; do not rely on frame-count increments. Detect foot sliding, clipping and teleporting.
7. **Dialogue and lip sync** — align audio to words/phonemes where available; map phonemes to visemes with coarticulation, jaw/lip timing and emotion overrides. Calibrate to actual audio timestamps, preserve pauses, and avoid mouth movement during silence. If only text is available, label timing as estimated, not exact.
8. **Layout and camera** — place actors, props and set dressing in a persistent world coordinate system. Validate scale, contact, occlusion, collisions, safe frame margins, focus target, depth of field and screen direction. Use aspect-aware contain/cover rules without stretching reference images; keep face detail sharp at close-up resolution.
9. **Lighting and rendering** — maintain consistent materials, key/fill/rim lighting, shadows, ambient occlusion and color pipeline. Use adaptive quality for phone thermal/memory limits; do not trade away deterministic timing or asset identity.
10. **Sound and final edit** — mix dialogue, ambience, effects and music with ducking, head/tail fades and scene continuity. Verify duration, sync drift, clipping, missing media and output decodability.
11. **Quality review** — a director/reader pass checks story comprehension and emotional continuity; a technical pass checks geometry, staging, camera, motion, lip sync, sound and export. Human review remains a required release gate for world-class quality.

## Release gates
- **Build:** clean Android build, APK exists, install/package verification passes.
- **Assets:** every referenced asset resolves; graceful fallback and clear warnings for missing assets; no broken paths.
- **Determinism:** same project/settings produce repeatable shot timing and no frame-rate-dependent motion.
- **Continuity:** character identity, costumes, props, handedness, geography and lighting remain consistent across cuts.
- **Animation:** no unintended jumps, frozen limbs, foot sliding, intersections or sudden speed changes in sampled clips.
- **Lip sync:** measure audio/viseme onset error and drift on dialogue test clips; test silence, pauses, fast speech and multiple speakers.
- **Camera/composition:** face/prop framing, aspect ratio, crop, focus and screen direction are checked in wide, medium, close and vertical shots.
- **Audio:** no missing tracks, clipping, abrupt starts/ends or unintentional sync drift.
- **Export:** play the entire exported film from beginning to end on a real Android device; inspect representative frames and transitions.
- **Regression:** test supplied reference sets and scripts, including crowded scenes, animals, prop hand-offs, location changes, long dialogue, silence and unusual aspect ratios.

## Implementation status
Procedural mesh factories and a mesh render smoke test are an initial foundation only. They do not yet constitute a complete rigged character system, a universal script-to-film pipeline, verified phoneme-level lip sync, or a Pixar-quality result. Keep the existing renderer path stable while integrating and testing each capability behind small, reversible changes.


## User-controlled opening/closing, narration, score and captions

- **Title image:** the first film page must use the title-page picture uploaded by the user. If none is present, generation must show a clear missing-title-image warning rather than presenting a generated fallback as the user's picture.
- **Final page:** use a separately uploaded ending image when supplied; otherwise reuse the user's uploaded title-page image for the final page. Never silently substitute a random story background when title art exists.
- **Narration:** the narrator speaks only lines explicitly marked `Speak:`, `Narrator:`, `Voice-over:` (or the supported Hindi equivalents) and only when narration is enabled. Ordinary directions are not automatically spoken.
- **Subtitles:** off by default. Add subtitles only as a deliberate post-production step when requested by the user; do not infer that subtitles should be burnt into the master film.
- **Music:** retain the existing score pass: title/end themes, scene mood cues, crossfades and volume shaping beneath dialogue. Add score only where it supports the dramatic beat; preserve dialogue intelligibility and use ambience/SFX as separate layers.
- **Release tests:** verify title art at frame zero, end art on the last page, marked Speak lines spoken once, unmarked directions not voiced, captions absent in the default master, and music ducked under dialogue.


## Articulated props, animals and environment objects
Every visible asset must be classified as static, rigid-movable, articulated, deformable, or simulated. A prop is not considered animated merely because its root can translate.

- **Props:** doors, windows, blinds, handles, drawers, lids, hinges, switches, fans, wheels, steering wheels, pedals, levers, tools, cups, bottles, kettles, pots, phones, remotes, books/pages, furniture, toys, vehicles and machinery need appropriate pivots, contact anchors and state transitions.
- **Animals:** species-aware skeletons and controls for spine, neck/head, limbs, paws/hooves, tail, ears, wings, jaw and gaze; gait changes with walking/running/turning/standing/lying, plus interaction and contact with riders, handlers, ground and props.
- **Environment:** trees/branches/leaves, grass, flowers, curtains, shades, signs, flags, cables, water surfaces, fire/smoke and loose objects require wind, collision, spring/damping or particle behaviours appropriate to the asset.
- **Rig metadata:** local origin, parent/child hierarchy, pivot/axis, bind pose, joint limits, mass/inertia, collision shape, material/surface, grip/contact anchors, action states and level-of-detail policy.
- **Motion contract:** every action is deterministic from absolute timeline time, supports interpolation and reversal for scrubbing, uses physically plausible contacts, and preserves identity/state between shots. No prop teleports from hand to table; no wheel spins without matching travel unless slipping is intended.
- **Mesh quality:** adaptive tessellation by projected pixel footprint and deformation needs. Use finer topology around joints, silhouettes, faces, hooves, fingers and close-up props; use LOD/culling at distance. “Pixel-level” is a quality goal, not a mandate for unbounded triangles or memory.
- **Integration gate:** these controls must be consumed by the actual film renderer and timeline; smoke tests alone do not prove an action is visible in an exported film.
