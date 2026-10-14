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
