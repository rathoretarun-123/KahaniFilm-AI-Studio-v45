# Animation engine audit and production-pipeline recommendation

**Scope:** repository inspection for the Reader → Editor → Director → Technical Director → Director final decision → shot-plan application path, and selection of existing rendering/inference engines.

## Findings from the current code

- `ScriptAI.productionConference()` calls separate model passes for the independent reader, editor, director first cut, technical-director review, reader response, editor response, director revision, technical second pass, and director final decision.
- The final report now records a versioned hand-off contract (`conference_contract_version`, `conference_contract_valid`), and does not fabricate `final_decision_by=director` when the model omits it.
- `ProductionPlan.apply()` validates that contract and applies scene-indexed final shot cues to the same `Story` consumed by `Director`. It preserves authored dialogue and blocks rendering when a versioned contract is invalid or no usable scene-indexed shot cue exists.
- This is a prompt-orchestrated multi-pass workflow, **not model-weight training**. The model is being instructed at inference time; no fine-tuning pipeline or learned animation model is present in this path.
- The Android renderer remains the app's own deterministic 2D/procedural rendering pipeline. The repository does not currently embed a full character-rigged 3D animation engine or a diffusion video runtime. An AI conference report or a successful APK build does not prove Pixar-level output or validate the final rendered film aesthetically.

## Existing engines checked

### 1. Blender — recommended for a true rigged-animation backend

Blender supports headless/background rendering and command-line rendering of stills and animation, so a separate desktop/cloud worker can create scenes and render frames without opening its UI. It is the strongest fit among these options for real 3D scenes, rigs, cameras, lighting, animation curves, and physically grounded staging.

Official documentation: https://docs.blender.org/manual/en/dev/advanced/command_line/render.html

**Integration shape:** keep the Android app as the script, asset, review and approval client; add a separately deployed Blender worker behind a versioned render-job API. The Director emits a typed shot manifest; a worker maps approved actions, character rigs, locations, camera and continuity locks into a controlled Blender scene template, then returns preview frames and render metadata. Do not bundle Blender into the Android APK.

### 2. ComfyUI — recommended optional generative-asset/video backend

ComfyUI is an open-source node-graph inference engine with reusable JSON workflows, API integration and workflows for image, video, audio and 3D generation. It can be deployed locally on a suitable GPU workstation or as a separate service. The core repository identifies GPL-3.0 licensing; review the core, custom-node and model licenses before redistributing or using any workflow commercially.

Official repository: https://github.com/Comfy-Org/ComfyUI

**Integration shape:** create explicit workflow adapters for image/asset generation and any selected video models. Keep it optional; do not make app startup or basic editing depend on a GPU service. Treat generated results as candidates until identity, motion and continuity checks pass.

### 3. Keep the current Android renderer as the reliable fallback

The existing deterministic renderer remains useful for previews, offline operation, storyboards, animatic timing, titles, simple motion and export. It should not be described as a full 3D engine or a trained video-generation model. Improve it incrementally while the optional backends are developed.

## Required hand-off contract per shot

1. **Reader:** identifies canon facts, implied causal beats, audience questions, and uncertain inferences. Uncertain plot-changing inferences require approval.
2. **Editor:** proposes shot economy, pace, reaction holds, transitions, silence and sound bridges.
3. **Director:** owns the final creative cut and supplies a scene-indexed shot plan with purpose, framing, visual action, performance, continuity locks and asset references.
4. **Technical Director:** checks actual reference availability, cast/place identity, shot feasibility, movement/contact, screen direction, lip-sync timing, sound and export blockers; distinguishes machine-checkable PASS/FAIL from NEEDS_HUMAN_REVIEW.
5. **Reader + Editor response:** address the proposed cut and technical critique explicitly.
6. **Director revision + Technical Director second pass:** record accept/reject/adapt decisions with reasons; unresolved objective blockers cannot be waived by creative preference.
7. **Apply + audit:** validate the explicit final director sign-off and shot-plan schema; apply the plan to the actual Story used by Director; record applied cue count and any omissions. A render is not conference-approved if the contract is invalid.
8. **Rendered-film QC:** generate a contact sheet and representative clips; compare with reference video for shot coverage, character identity, continuity, action legibility, pacing, dialogue/audio and titles. Automated tests verify structural invariants, but a human must judge aesthetic quality.

## Release gate

Do not label the pipeline “flawless” based on the conference JSON alone. Require (a) contract and hand-off unit tests, (b) app regression tests, (c) both bundled stories rendered through the same production path, (d) generated contact sheets/short preview clips inspected against each supplied reference video, and (e) a report separating objective checks from human-review findings. The current Actions run built and packaged the APK, but the broader regression suite failed with 32 failures, many involving missing legacy test fixtures and some rendering assertions. Therefore that run is **not a passing release gate**.

## Practical sequence

1. Stabilize the Reader/Editor/Director/Technical Director schema and add deterministic contract tests.
2. Repair missing/incorrect regression fixtures and real rendering failures; do not suppress tests merely to turn CI green.
3. Produce and inspect both story previews against their reference videos.
4. Add a versioned remote-render adapter, starting with Blender for rigged scenes and ComfyUI for optional generative assets; preserve the deterministic Android path as fallback.
5. Only consider model fine-tuning after collecting licensed, consented, quality-labelled examples of scripts, approved shot manifests, generated renders and human corrections. Prompt edits alone are not training.
