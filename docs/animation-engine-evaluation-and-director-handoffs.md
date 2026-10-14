# Animation engine evaluation and production-role hand-offs

Status: engineering decision for KahaniFilm, 14 October 2026.

## Decision

Do not replace the current Android renderer with a heavyweight engine in a rushed patch. Keep the current renderer as the dependable offline/mobile path, and define a pluggable render-backend boundary before integrating an external engine. The current project renders through its own Java/Android `Director`, `Renderer`, `Rig`, and `Synth` pipeline; a multi-role LLM conference is not itself a 3D animation engine, and prompt instructions do not train model weights.

### Candidate engines

| Candidate | What it is good at | Fit / constraints | Decision |
|---|---|---|---|
| Blender | Scriptable 3D scenes, armatures, animation, camera, lighting and rendering; Python API | Best candidate for a real 3D backend, but desktop/server GPU/CPU workload and asset preparation are not appropriate to bundle inside the Android APK | Prototype as an optional desktop/cloud worker; do not make mobile/offline builds depend on it |
| ComfyUI | Composable model workflows and local API for image/video/audio/3D model pipelines | Useful for optional generative asset/video jobs on a configured machine or GPU service; model licenses, VRAM, queueing, provenance and output validation must be handled per workflow | Optional AI-generation adapter, not the deterministic film editor or story director |
| Godot | Runtime 2D/3D animation, animation tracks and blending/state machines | Good for interactive real-time scenes and controlled rigs, but adopting it requires a scene/asset pipeline and a renderer integration; it does not create a film from prose by itself | Re-evaluate if the product chooses reusable rigged real-time characters |
| FFmpeg / existing muxer | Media assembly, codecs, audio/video transforms | Does not generate acting, staging, rigs or story-aware coverage | Use only as the export/assembly layer where supported |
| Existing KahaniFilm renderer | On-device procedural staging, custom rigs, cuts, audio and offline operation | Already integrated with the Android project and its library; limitations must be addressed with shot-level tests and asset review | Keep as baseline while making AI shot plans actionable and auditable |

Official references:
- Blender Python API: https://docs.blender.org/api/current/
- Godot animation system: https://docs.godotengine.org/en/stable/tutorials/animation/
- ComfyUI project/API and GPL-3.0 project license: https://github.com/Comfy-Org/ComfyUI

## Required role hand-off contract

Every role must produce a distinct, inspectable artifact. The next role receives that artifact and the relevant source material, not just a vague summary.

1. **Reader / script supervisor**: canon facts, emotional/causal beats, explicit-vs-inferred labels, missing visual beats, continuity risks, blocking questions.
2. **Editor / animatic editor**: beat timing, cut purpose, reaction holds, transitions, silence and sound bridges; preserves dialogue and story causality.
3. **Director**: resolves each material proposal as accept/reject/adapt with a reason; issues a scene-indexed shot list with framing, visual action, performance, continuity lock and purpose.
4. **Technical director**: checks exact asset identity/availability, rig/action feasibility, geography, prop state, screen direction, dialogue/lip-sync timing and export constraints; distinguishes hard blockers from preferences.
5. **Reader/editor response and director revision**: material objections are answered explicitly. A disagreement may remain, but cannot be hidden as a pass.
6. **Second technical pass**: marks prior blockers resolved or still open with evidence.
7. **Final director decision**: explicit sign-off, non-empty decisions, a usable shot plan and a boolean readiness decision. The final plan is applied to the exact `Story` instance consumed by `Director`; invalid or empty shot plans fail closed rather than silently passing.

## Implementation invariants

- Never overwrite a model's sign-off field to manufacture approval.
- A conference report is not proof of render quality. Rendered frames/video still need objective checks and visual review.
- Preserve authored dialogue, canon, scene order and user-owned assets; inferred connective shots are bounded and confidence-gated.
- Every final shot-plan item must resolve to a valid original scene and provide at least one actionable visual cue. The actual renderer must consume the cue.
- Keep a report of role hand-offs, applied shot-plan cue count, unresolved blockers, generated assets and render/export QC.
- No engine integration may silently require a GPU, network, API key or external service. The app must retain an explicit offline fallback.
- “Training the director” currently means improving prompts, schemas, validation, test fixtures and inference-time behavior. Actual weight fine-tuning is a separate data/compute project and must not be claimed unless implemented.

## Current CI limitation to fix

The Android APK build and bundled film payload verification can pass while the broader Robolectric suite fails. The latest checked run reported 98 tests, 32 failures and 1 skip; numerous failures cite absent legacy fixtures such as `tools/testdata/upload`, `tools/testdata/sheets`, `voices/man.wav` and `app/src/main/assets/sample/char_vrinda.jpg`. Treat these as test-fixture packaging problems only when the test explicitly requires those fixtures; do not blanket-ignore the suite, since some image splitting, face preservation and render behavior failures may be genuine regressions. CI must end green only after the fixtures are restored or tests are correctly made self-contained and remaining failures are resolved.
