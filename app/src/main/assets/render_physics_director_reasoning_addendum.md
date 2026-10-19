# KahaniFilm Render and Production Reasoning Addendum
Version: 1.0 | 14 October 2026
Scope: supplemental guidance for the existing reader, director, technical director, render, and quality-control pipeline. This does not replace prior manuals or silently override existing rules.

## Non-negotiable production contract
1. Preserve authored plot facts, chronology, dialogue, relationships, and ending. Label canon, safe visual inference, and uncertainty separately.
2. The narrator is opt-in only when explicitly requested by the screenplay or by a production setting. Character/place descriptions, stage directions, and scene headings are not narrator dialogue. Title narration is separately opt-in.
3. Subtitles are off by default. Add them only when the filmmaker explicitly asks in post-production. Do not burn in captions merely because a line was spoken.
4. Upload requests precede private-library matching for missing required assets. Clearly distinguish "not supplied", "not found in library", "AI-generated substitute", and "verified match". Never claim an asset exists without inventory evidence.
5. Preserve each character's identity and continuity across every shot: face landmarks, hair, skin tone, body proportions, age, wardrobe, accessories, voice, handedness where known, and screen position. Low-confidence front/back or expression classification must not invent a face or emotion.
6. Ask for at most the concrete missing references needed to unblock the next step. Batch up to 18 images per subject/reference set when the picker supports it. Keep the six-view contact-sheet convention (3 columns x 2 rows) for review, not as a reason to flatten source images into one unusable texture.
7. The story owner has final creative approval. The reader proposes; editor challenges clarity and rhythm; director resolves; technical director checks feasibility; quality control reports evidence.

## Reader: think in causal and visual beats
For every scene and beat, record:
- Who is present, their exact location, facing direction, eyeline, entrance/exit, and relation to the camera.
- What changes: emotional state, knowledge, prop ownership/state, physical location, weather, light, time, or sound.
- The audience's question and the visual evidence that answers it.
- Explicit facts versus inference; evidence in the script; confidence; whether approval is required.
- Missing characters, places, props, costume items, background elements, sounds, voice references, poses, rigs, and transition shots.
- Daily tasks as readable chains only when implied by the text: intention -> tool/prop pickup -> contact -> action -> consequence -> release/put-down -> reaction. Do not insert new story facts or make every routine action a mandatory montage.
- Blocking ambiguity as a short question, not a fabricated answer.

## Director/editor conference
Use stable issue IDs so every Reader, Editor, Director, and Technical Director issue is explicitly accepted, rejected with a reason, resolved by a change, or retained as a named risk. Never let one role silently discard another role's blocking concern.
For every important beat ask: what does the audience see, what does the character feel, what changes, what is the best shot to communicate it, and what must remain continuous in the next shot?
Plan establishing shots, master coverage, action inserts, reaction close-ups, over-the-shoulder and point-of-view shots where motivated. Vary angles for storytelling, not decoration. Keep eyelines, screen direction, geography, prop hand, light direction, and character scale coherent across cuts. Use reaction holds and silence when emotionally useful; avoid mechanically cutting every fixed number of seconds.

## Physics and natural phenomena
Effects must have causes, spatial bounds, persistence, and consequences. Wind direction affects hair, cloth, leaves, smoke, dust, water, and loose props consistently. Rain wets exposed surfaces progressively, creates ripples/splashes and changes ambience; it stops only when the script or time transition supports it. Fire has a plausible light/flicker field, smoke rises and responds to wind, and shadows follow the chosen light source. Waves, falling objects, collisions, jumps, landings, doors, carried objects, and footsteps need coherent direction, contact, momentum, gravity, and recovery. Use stylized exaggeration while preserving readable cause-and-effect; do not claim a true physical simulation when an effect is a procedural visual approximation.

## Motion, mesh, rig, and identity
Use the supplied image as the canonical identity reference. Distinguish front, back, profile, and three-quarter views before facial-expression inference. A back-of-head view must not receive invented eyes, smiles, or lip motion. Keep mesh topology and silhouette stable across poses; avoid stretching facial landmarks or clothing patterns. Lock foot contact during planted steps, transfer weight before a step, use anticipation and eased arcs, then settle/follow-through. Secondary motion (hair, cloth, ears, tails, props) follows the main body with a plausible delay. If the required sitting/lying/action pose cannot be inferred safely from a standing portrait, request a suitable pose reference or use an explicitly disclosed stylized approximation.

### Contact and support gestures
- Read "sits on a shoulder" as a support/anchor relationship; read "rests their head on someone's shoulder" as two distinct bodies in contact. Do not turn a head-rest direction into a character sitting on another character.
- For a head resting on a shoulder, preserve the written seated/standing posture, resolve possessive pronouns to the correct nearby character, and ease the resting head toward that character. No sudden teleport, sliding seated bodies, or unmotivated full-body movement.
- For a small animal or child jumping into someone's arms, animate one continuous approach/jump arc; attach the smaller actor only at arrival, then carry them with the holder until an explicit put-down. The holder's arms should visibly support the carried body; do not leave the animal floating beside or behind the holder.
- Keep contact points consistent as either actor turns or walks. Check the relationship at several consecutive frames, not only the first and last frame.

## Lip sync, voices, and sound
Dialogue audio is the timing authority. Split long lines into performable phrases; align mouth-open/closed and visible phoneme groups to speech timing; retain pauses, breaths, emotion, and turn-taking. The active speaker should own the visual focus unless a reaction shot is motivated. Non-speaking characters must not visibly lip-sync. Do not synthesize narrator lines from descriptions. Maintain voice identity, language, pitch, rate, and acoustic space across scenes. Separate dialogue, foley, effects, ambience, and music; duck music under dialogue and preserve room tone across edits. Trigger sounds from script/action cues, not merely from words that happen to contain the name of a sound.

## Camera and frame rate
Choose shot size, angle, lens impression, movement, focus, and duration based on story intent. Establish geography before tight coverage where needed. Prefer stable motivated camera motion and clean cuts; camera shake is reserved for a justified event. Higher output FPS improves temporal sampling but does not create missing motion or improve the source character rig by itself. Retain the existing selectable 24/30 fps output; if 48/60 fps is introduced, expose it as an opt-in, device-tested mode with memory, render-time, battery, and encoder checks. Never advertise a higher frame rate unless the MP4 timestamps and encoder output verify it. Preview/animatic FPS may differ from final render FPS and must be labelled accordingly.

## Asset intake and private library
At the filmmaking page, ask for missing required character/place/prop references before offering library matches. Derive missing prop requests from tangible objects in physical-action directions as well as an explicit Objects list; dialogue-only mentions, scene headings, and material adjectives must not create separate prop requests. Match complete object words (while allowing ordinary English plurals), not arbitrary substrings. Offer upload controls at the point of need, support multi-select up to 18 images, and preserve originals independently. After the user uploads or explicitly skips, offer a separate private-library lookup. Never upload private library contents to a cloud model without clear user consent. Character, location, prop, sound, and voice libraries should be searchable separately and should not cross-match merely because filenames share a word.

## Validation gates
- Parser: authored dialogue and scene order preserved; narrator absent unless explicit.
- Asset: every required reference has a state and provenance; no stale or wrong-character reuse.
- Continuity: identity, costume, prop ownership/state, screen direction, lighting, weather, time, and geography checked between adjacent shots.
- Animation: foot sliding, float, penetration, clipping, frozen expression, back/front confusion, duplicate limbs, abrupt pose changes, and unmotivated camera movement reviewed.
- Audio: dialogue intelligibility, speaker assignment, language, timing, non-speaker mouth movement, sound triggers, clipping, loudness, and room tone reviewed.
- Export: requested dimensions and FPS verified from the actual encoded file; audio/video duration and sync checked; playback smoke test completed.
- Regression: run both bundled stories, all existing unit/acceptance tests, and the render soak suite. Inspect the reference videos as visual targets, not as proof that the generated output matches them.
- Report pass/fail/needs-human-review with reproducible evidence. Do not describe a build as Pixar-level or production-ready solely because it compiles.
