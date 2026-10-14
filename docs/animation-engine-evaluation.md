# Animation engine evaluation for KahaniFilm

Date: 2026-10-14

## Decision

Keep the Android app as the script, asset-library, production-planning and lightweight render client. Do not pretend that prompting a text model trains a renderer. Add a pluggable render-backend boundary and use a separate desktop/cloud worker for heavyweight animation. The first backend to prototype for rigged 3D is Blender; Godot is the alternative when an interactive, deterministic scene runtime is more important. Keep ComfyUI as an optional AI asset/video inference service, not the authoritative scene/continuity engine.

## Engine comparison

| Engine | Best fit | Advantages | Limits for this app |
|---|---|---|---|
| Blender | Rigged 3D characters, meshes, materials, lighting, camera, scripted offline rendering | Mature production DCC; Python automation; can render headlessly on a remote worker | Not practical to embed as a full renderer inside an ordinary Android APK; requires scene templates, rigged assets, worker compute and export validation |
| Godot | Deterministic real-time or offline 2D/3D scene rendering | AnimationPlayer/AnimationTree, 3D scene runtime, offline Movie Maker rendering | Needs authored/imported rigs and animation clips; it does not infer story intent or create consistent character assets by itself |
| ComfyUI | Optional generative image/video/3D workflows | Node-based model inference and API-capable backend; workflow reproducibility | Model-specific VRAM, licensing and temporal/identity consistency vary; generated output needs separate story, asset and QC gates |
| Current KahaniFilm renderer | Android-compatible storyboard/2D compositing, timing, voices, SFX and video assembly | Low-friction device workflow and deterministic fallback | It is not equivalent to a fully rigged 3D character-animation engine; quality remains bounded by current procedural art and source assets |

## References

- Blender command-line rendering (background/headless rendering): https://docs.blender.org/manual/en/dev/advanced/command_line/render.html
- Godot offline movie rendering and AnimationPlayer/particles/shaders: https://github.com/godotengine/godot-docs/blob/master/tutorials/animation/creating_movies.rst
- Godot animation system: https://github.com/godotengine/godot-docs/blob/master/tutorials/animation/animation_tree.rst
- ComfyUI repository and GPL-3.0 license: https://github.com/Comfy-Org/ComfyUI

## Required adapter contract

1. Input: validated per-shot JSON with stable character/prop/place IDs, reference asset IDs, duration/FPS, camera/framing, action, pose/emotion, screen direction, contact/prop state, dialogue timing, sound cues and continuity locks.
2. Resolve assets before rendering; never silently replace a requested identity with a generic asset.
3. Render low-resolution preview first; return frame sequence/video, logs, missing-asset report and per-shot timing.
4. Run objective QC (duration, dimensions, codec, black/frozen frames, missing audio, scene/shot coverage) before final encode.
5. Keep human review separate from automated QC. A passing render check is not a claim of feature-animation quality.
6. Model fine-tuning, if eventually justified, is a separate data/compute/licensing project. Prompt and workflow engineering are not model training.

## Four-role hand-off contract

- Reader emits canon facts, inferred details with evidence/confidence, causal beats and ambiguity questions.
- Editor emits a proposed cut, pacing/transition notes and reaction/silence coverage.
- Director owns the final beat map and shot plan; every shot must point to an existing scene and specify a visible action or purpose.
- Technical Director validates asset identity/availability, rig feasibility, continuity, timing, lip sync and export risks; unresolved hard blockers prevent a production-ready sign-off.
- Director responds to each material role objection with accept/reject/adapt plus a reason, then issues final sign-off.
- The final shot plan must be mapped into the exact scene/shot data consumed by the renderer. A conference transcript alone is not an integration.
- Regression tests must prove that a valid plan changes the downstream shot/scene plan, a malformed or unsigned plan is rejected, and authored dialogue is unchanged.
