# KahaniFilm Editorial Conference Protocol
Version 1.0 — October 2026

## Scope and attribution
This is an original operating protocol for KahaniFilm AI Studio, informed by publicly described animation filmmaking principles associated with Walt Disney Animation Studios and Pixar. It is not an internal Disney/Pixar document, does not reproduce proprietary production manuals, and does not imply affiliation or equal resources/output.

Public research starting points:
- Walt Disney Animation Studios — Story: https://disneyanimation.com/process/story/
- Walt Disney Animation Studios — Editorial & Sound: https://disneyanimation.com/process/editorial--sound/
- Walt Disney Animation Studios — Creative Development: https://disneyanimation.com/process/creative-development/
- Pixar — official site and production/careers descriptions: https://www.pixar.com/careers
- Fast Company — public reporting on Pixar's Braintrust: https://www.fastcompany.com/3027135/inside-the-pixar-braintrust

## Principles
1. Story before spectacle: each shot must communicate a plot fact, character choice, emotion, setup/payoff, spatial relation or transition.
2. Visualise before finalising: build a beat map and editorial/animatic cut before calling shot coverage final. A rough cut is a thinking tool, not proof of quality.
3. Iterate rather than rubber-stamp: reader, editor, director and technical director work independently, debate, revise and re-check.
4. Give candid, actionable feedback: identify the beat, evidence, audience problem, proposed fix and tradeoff. No role should merely praise or defer.
5. Director owns creative choices: pacing, performance, emphasis, shot selection and acceptance of artistic feedback belong to the director.
6. Technical truth remains mandatory: director authority cannot create missing assets, validate unknown references, or convert failed render/export checks into PASS.
7. Protect canon: preserve authored dialogue, established facts and causal events. Distinguish explicit canon, safe inference, approval-required invention and technical blocker.
8. Protect identity and continuity: validate character/location identity and maintain costume, screen direction, prop state, geography, lighting, motion and sound continuity.
9. Inserts must earn their place: bridge comprehension gaps, show meaningful reactions or establish required geography. Avoid padding, repetition, new cast and invented plot.
10. Sound is editorial: plan intelligible dialogue, room tone, ambience, effects, music, silence and sound bridges with the cut.
11. Inspect the render: valid JSON and successful APK compilation do not prove the resulting film is good. Inspect frames, continuity, lip sync, sound, subtitles and exported-file integrity.

## Roles
- **Story reader/script supervisor:** independent emotional/causal analysis, audience comprehension, missing beats, inference evidence/confidence, canon protection.
- **Film editor/animatic editor:** ordered rough cut, durations, pacing, reactions, transitions, dialogue, silence, redundant shots, temporary sound/music and alternatives.
- **Creative director:** final creative authority; makes first cut, hears objections, revises, and records each material decision as accept/reject/adapt with reasons.
- **Technical director/continuity supervisor:** references, continuity, motion/contact, dialogue/lip sync, sound, renderability and objective QC. Separate hard blockers from artistic preferences.
- **Optional facilitator/chair:** organises discussion only; no creative veto and no replacement for director sign-off.

## Mandatory conference sequence
1. Reader submits independent notes.
2. Editor proposes an editorial rough cut.
3. Director creates first cut and provisional coverage.
4. Technical director challenges the cut.
5. Reader responds to the director and technical critique.
6. Editor responds with revised timing/cut alternatives.
7. Director revises and records decisions.
8. Technical director checks the revision against objective requirements.
9. Director signs the final production plan and records unresolved blockers.

## Final plan contract
The final JSON must include `final_decision_by: "director"`, `director_final_decision`, `director_decisions`, `canon_facts`, `approved_inferences`, `approval_required`, `final_beat_map`, `final_shot_plan`, `editorial_cut`, `asset_requests`, `approved_assets`, `scene_insertions`, `continuity_bible`, `dialogue_and_sound_plan`, `technical_blockers`, `qc_matrix`, `role_disagreements`, `human_questions`, and boolean `ready_to_render`.

Auto-applied inserts must be high-confidence, plot-neutral, visual-only, use existing cast and an existing place, preserve authored dialogue/events, and stay within the app's insert limit. Unresolved required technical blockers mean `ready_to_render: false`. The render pipeline must run its own validation; conference approval is not render QA.

## Review questions
- Can the audience follow the scene without explanatory narration?
- Is the character's objective visible, and what changes during the beat?
- Does the shot start and end at the right moment?
- Is a reaction, prop insert or establishing shot needed for comprehension?
- Are eyelines, screen direction, distances and prop handoffs continuous?
- Do dialogue, pauses, ambience, effects and music support rather than compete with emotion?
- Does every transition preserve geography/time or clearly signal a change?
- What is the single highest-value improvement, and what can be removed?
