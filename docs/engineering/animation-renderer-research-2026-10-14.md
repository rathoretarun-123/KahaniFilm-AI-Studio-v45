# KahaniFilm AI Studio — rendering-engine integration research

**Research date:** 2026-10-14. This is a source-backed engineering assessment, not a claim that these engines are already integrated.

## Recommendation

Keep Android responsible for script ingestion, private asset library, job management, preview and deterministic fallback. Treat the approved production plan as a versioned contract consumed by the actual renderer. Prototype a separate **Blender headless rendering worker** for repeatable true-3D scenes, with FFmpeg as a finishing stage after license/build verification. Consider Godot for real-time preview, OpenToonz for a dedicated 2D workflow, and diffusion/video providers only behind an optional adapter with model-by-model license, cost, VRAM, latency and identity-consistency checks. Do not embed a desktop DCC or large diffusion stack in the APK.

## Comparison

| Candidate | Actual capability | Deployment and constraints | Recommendation |
|---|---|---|---|
| **Blender** | Full 3D content creation: meshes, materials, rigging, animation, cameras, lights, rendering, compositing, sequencer and Python automation. Background/CLI rendering is supported. | Separate Linux/Windows worker or render farm; Android submits structured shot jobs and receives outputs. Not a small Android library. Scenes still need usable models, rigs, acting animation and lighting. GPL license means distribution and derivative-work obligations need review before bundling. | Best first prototype for repeatable true 3D. |
| **Godot** | Real-time 2D/3D engine with scene graphs, cameras, animation systems, skeletal rigs and glTF import. | Desktop/cloud or separately packaged runtime; scene and rig authoring remains necessary. A game engine, not a generative video model or automatic cinematic animator. MIT license, with third-party components checked separately. | Use when interactive preview or real-time 3D is a measured need. |
| **OpenToonz** | 2D animation authoring and compositing workflow. | Desktop/batch workflow; export frames/video to Android-managed jobs. Not a 3D rig/mesh engine. Main project has BSD-style licensing, but third-party components/assets have their own terms. | Optional explicit 2D/2.5D workflow. |
| **FFmpeg** | Media decode/encode, filters, scaling, overlays, concat, audio mixing and muxing. | CLI/library in backend worker; Android integration requires checking native ABI, package size and actual build configuration. LGPL/GPL obligations depend on build configuration and codecs. Not an animator. | Use for deterministic assembly, sound mix and encoding after dependency/license verification. |
| **ComfyUI and diffusion/video models** | Node-based image/video generation workflows, custom nodes and model pipelines. | Separate GPU workstation/cloud service. Model weights, custom nodes and APIs have separate licenses/dependencies. VRAM, loading latency, queue time, cost and temporal identity drift vary. Not inherently a consistent character-animation engine. | Optional provider for selected shots/assets after per-model license and quality testing. |
| **Speech and lip-sync** | TTS, phoneme alignment, audio-driven facial motion and rigged visemes are separate capabilities. | Modular services linked to the shot timeline. Voice/model rights, consent, privacy and language support require review. | Preserve per-line timing and phoneme/viseme tracks in the shot contract; don't claim TTS alone creates acting. |

## Official upstream sources

- Blender: https://www.blender.org/
- Blender CLI rendering: https://docs.blender.org/manual/en/5.4/advanced/command_line/arguments.html
- Blender Python automation: https://github.com/blender/blender/blob/main/doc/python_api/rst/info_advanced_blender_as_bpy.rst
- Blender license: https://github.com/blender/blender/blob/main/COPYING
- Godot: https://godotengine.org/
- Godot 3D scene import: https://docs.godotengine.org/en/stable/tutorials/assets_pipeline/importing_3d_scenes/index.html
- Godot import configuration/animation: https://docs.godotengine.org/en/stable/tutorials/assets_pipeline/importing_3d_scenes/import_configuration.html
- Godot source/license: https://github.com/godotengine/godot
- OpenToonz: https://opentoonz.github.io/
- OpenToonz source/license: https://github.com/opentoonz/opentoonz
- FFmpeg: https://ffmpeg.org/
- FFmpeg legal/licensing: https://ffmpeg.org/legal.html
- ComfyUI source: https://github.com/comfyanonymous/ComfyUI
- ComfyUI docs: https://docs.comfy.org/
- Model-specific licensing: https://huggingface.co/docs/hub/models-cards

## Proposed renderer contract

Keep requests provider-neutral: job/film IDs; schema version and idempotency key; screenplay/canon version and approved conference hash; ordered scene/shot IDs and durations; shot size, camera movement, screen direction and continuity locks; character IDs/reference images; environments and props; physical interactions; performance/action cues; dialogue/voice/audio and phoneme timing; lighting/aspect ratio/frame rate/resolution; fallback policy. The backend returns renderer/version, source shot IDs, per-shot status, output locations/checksums, dimensions, frame rate, duration, audio streams and warnings. QC rejects missing or reordered shots.

## Phased integration

1. Protect current Android/offline behavior; validate approvals honestly and test approved directives against actual shot instructions.
2. Prototype Blender on three reproducible test shots: establishing, reaction close-up and prop interaction. Compare identity, camera, motion, timing and continuity before scaling.
3. Use FFmpeg for assembly only after legal/package verification.
4. Add Godot/OpenToonz/ComfyUI as optional backend adapters only when a benchmark demonstrates value.
5. Keep voice, alignment and viseme/facial animation separate and versioned.
6. Gate each provider on contract tests, identity/continuity benchmark, cost/latency budget, failure handling and license review.

## Current limitations and honest claims

The repository is a Java Android app with its own planning/rendering and media assembly code, plus Cloud-related orchestration and Studio3DArt. That does not prove Blender, Godot, OpenToonz, ComfyUI, a full skeletal animation runtime or a production-grade video model is integrated. Verify calls and actual outputs before calling a backend operational.

Changing prompts/configuration is **inference orchestration**, not model-weight training. Fine-tuning changes model weights; animation quality additionally depends on assets, rigs, motion, shot execution, editing, sound and QC. “Pixar-level” is an aspiration, not a verified quality claim.

## Prototype acceptance

- Rendered shot IDs exactly match approved IDs/order.
- Missing assets, rigs and unsupported camera/action requests become blockers or explicit fallbacks.
- Character identity/wardrobe remain stable across adjacent shots.
- Prop state/contact, screen direction and location continuity are testable.
- Dialogue/audio and shot durations align to edit.
- Outputs decode and have expected dimensions/aspect/fps with no black/empty frames.
- Both Aakhiri Chai and Babu Ke Liye use isolated assets/manifests.
- Renderer manifest and artifact checksums are retained for regression.


## Follow-up research and implementation decisions (14 October 2026)

### Verified upstream documentation

- Blender's official command-line manual documents background/headless still and animation rendering, including frame ranges and thread controls: https://docs.blender.org/manual/en/latest/advanced/command_line/render.html . This supports a separate worker/service integration; it is not evidence Blender can be embedded as a small Android library. Blender physics and rendering are desktop/backend workloads whose CPU/GPU, memory, scene-file and queue costs must be measured on the chosen worker.
- Godot's licensing documentation identifies the engine as MIT-licensed and explicitly reminds distributors to account for third-party libraries and assets: https://docs.godotengine.org/en/stable/about/complying_with_licenses.html . Godot is a plausible real-time 3D/runtime option, but it would require scene, rig, asset-import and render-adapter work; no current repository evidence establishes that a Godot runtime is integrated.
- FFmpeg's official legal page states LGPL 2.1-or-later applies to the project, while optional GPL components can change obligations: https://www.ffmpeg.org/legal.html . Pin and audit the actual binary/build configuration; do not assume every prebuilt binary has identical licensing or codec support.
- OpenToonz describes itself as a 2D animation application and documents a modified BSD license for much of the project, with separate third-party and brush-library notices: https://github.com/opentoonz/opentoonz and https://github.com/opentoonz/opentoonz/blob/master/LICENSE.txt . It is more relevant to 2D production than as a drop-in 3D character renderer.
- MediaPipe Face Landmarker for Android can return 3D facial landmarks, blendshape scores and transformation matrices for images or video streams: https://developers.google.com/edge/mediapipe/solutions/vision/face_landmarker/android . This can support face/expression measurement or avatar driving, but it does not itself supply a character rig, identity guarantee, authored acting, or finished lip-sync.
- ComfyUI is a graph/node-based diffusion workflow and backend, with its source and license available upstream: https://github.com/Comfy-Org/ComfyUI . Model weights, model cards and individual workflow nodes may have separate licenses; review each checkpoint and dependency before commercial deployment.
- The OpenToonz upstream repository and its official documentation are preferable to third-party download mirrors. Treat community reports as debugging leads, not as proof of licensing or reproducible quality.

### Implications for this app

1. Keep Android as the project/library/job UI, upload-first reference collection, offline deterministic preview, and delivery client. Do not inflate the APK by adding a desktop renderer or large model weights without measured device feasibility.
2. Put true 3D work behind a versioned renderer adapter and a separately deployable worker. A Blender worker is the strongest first candidate for offline/headless cinematic rendering and physics-rich scenes; Godot is a candidate when an interactive runtime is specifically needed. Select only after a representative two-film benchmark.
3. Treat rigid-body contact, cloth, fluid, fire/smoke and wind as simulation tasks with state and temporal evaluation, not as prompts that merely depict the effect. A safe fallback may use authored animation or layered effects, but the QC manifest must identify it as an approximation.
4. Character matching should rank references using face/visual embeddings, pose/scene features and explicit user labels, then preserve a human-confirmed identity mapping across the film. Face landmarks are useful for geometry/expression, not a substitute for a canonical identity reference. Never choose unrelated stills independently per frame.
5. Natural motion needs a rigged skeleton, retargetable motion clips or mocap, contact constraints and continuity state. Daily activities should be represented as action graphs (approach → anticipate → reach → contact → manipulate → release → settle) with object state and handedness, then checked against rig/asset capability.
6. Lip-sync needs dialogue-to-audio alignment, phoneme/viseme timing and a compatible facial rig. TTS/voice services require explicit provider, language, consent, retention, latency, price and voice-license checks. Keep authored dialogue immutable and keep narration opt-in.
7. FPS is an output property, not a quality claim. 48/60 fps should be enabled only if animation is sampled or interpolated meaningfully, the encoder supports the target, audio duration remains aligned, and adjacent-frame motion tests show benefit. Duplicating 24/30 fps frames does not create smoother animation.
8. Google account chooser UI is not proof of OAuth authentication. Verify Credential Manager / Google ID token validation and backend audience/signature checks against official guidance before declaring login real. Credentials, OAuth client IDs and SHA fingerprints must be configured from the owner's Google Cloud project; never fabricate them.
9. Maintain the four-role production conference as structured, versioned data. Raw role responses, issue IDs, dispositions, unresolved blockers, final shot IDs, renderer-consumed directives and QC evidence must survive serialization. The current ScriptAI implementation validates that the final model response contains a director sign-off, non-empty decision/shot plan and boolean readiness; contract validation is stronger than a forced sign-off, but it is not proof that every role handoff or rendered shot has been executed correctly.

### Fixture and asset safety follow-up

The failing workflow's logs show absent paths including `tools/testdata/sheets`, `tools/testdata/upload`, `tools/testdata/sheets38`, `tools/testdata/sheets40`, `app/src/main/assets/sample/char_vrinda.jpg` and `voices/man.wav`. The four committed story archives must not be assumed to contain these older general-purpose test fixtures. The preparation script now searches extracted packet contents for matching fixture directories and reports when they are not present; it preserves existing fixture files with non-clobbering copies. It also no longer removes each film's bundled-library directory before rebuilding it. This reduces destructive behavior but does not by itself create absent fixtures or repair image decoder failures. CI must confirm the result.

### Scope and non-claims

This web research establishes feasible integration options, not that these engines/models are already integrated, that production-grade motion is available, or that the current APK provides Pixar-level output. Before adopting a model, engine, voice or service, record the exact version, model/asset license, runtime requirements, costs and measured acceptance results. Official references above were inspected on 2026-10-14.
