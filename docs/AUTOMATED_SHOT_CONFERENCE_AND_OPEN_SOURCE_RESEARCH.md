# Automated Shot Conference and Open-Source Engineering Research

Date: 2026-10-15

## Production contract

Every planned shot is rendered to a review keyframe before full export. The Reader checks narrative intent, causal readability and visible required assets. The Technical Director checks geometry, rigging, contact, frame integrity, continuity, lighting and physical plausibility. The Editor checks coverage, pacing, screen direction and continuity against adjacent shots. Each role receives the same frame in a separate review call. The Director inspects the frame independently, sees the reports, resolves disagreements with explicit reasons, and owns the final creative decision. Malformed reports, wrong shot numbers, missing evidence or absent Director sign-off fail closed.

Rejected shots receive bounded automatic repair and a fresh render/review. Current app repair actions are limited to camera widening/closer framing, calming motion and inserting a listener/reaction framing. Defects outside those capabilities block final export instead of being falsely approved. This is a first reliable control loop, not a claim that every defect can already be repaired.

Offline mode does not fabricate AI inspection. It runs deterministic image-integrity checks, records that semantic and identity review was unavailable, and never labels the fallback as a vision-conference pass.

## Open-source findings and applicability

- OpenUSD: https://openusd.org/dev/intro.html ; render guide: https://openusd.org/dev/user_guides/render_user_guide.html ; render passes: https://openusd.org/dev/user_guides/schemas/usdRender/overview.html . USD offers layered scene description and explicit render settings/passes. It is not itself a renderer or character-rigging system. Use its concepts for a future interoperable scene/shot manifest, not as a drop-in Java Android renderer.
- Blender: https://docs.blender.org/api/main/index.html ; simulation features: https://www.blender.org/features/simulation/ ; fluids: https://docs.blender.org/manual/en/latest/physics/fluid/introduction.html . Blender covers cloth, hair, rigid bodies, particles, smoke, fire and fluids. Its application license and native/GPU footprint make it a desktop/cloud backend candidate, not a direct Android dependency.
- Bullet Physics: https://github.com/bulletphysics/bullet3 . The zlib-licensed engine supports rigid/soft-body dynamics, collision detection, constraints and ragdolls. It is a candidate for a future native simulation backend; current Java EverydayPhysics should keep deterministic tests for gravity, contact, momentum, prop handling and secondary motion until native integration is justified.
- Netflix VMAF: https://github.com/netflix/vmaf . Use perceptual metrics as one signal when comparing encoded output to a known reference, not as a creative-quality score. It cannot detect wrong identity or a narratively missing prop.
- Open 2D rigging research: Iki https://github.com/zeikar/iki describes an MIT-licensed WebGL puppet runtime with mesh deformation, masks, spring/chain physics, blinking, gaze and lip-sync, but is early-stage. Rev2D https://github.com/RevStudio/Rev2D describes a deterministic headless renderer, JSON rig schema, IK, mesh deformation and physics; audit its license and maturity before reuse. These are architecture references, not dependencies to add blindly.
- Image-quality metrics: https://github.com/EadCat/NIQA and https://github.com/Xuzhuobin/Image-Quality-Assessment provide no-reference and full-reference metrics. Use them to detect blur, compression, contrast collapse and instability alongside the role conference; never let one score decide identity or story correctness.

## Per-asset contract

- Character: persistent identity ID; one front full-body image, two side, two three-quarter, one back, plus up to ten 3x2 reference sheets for expressions, poses and script activities. Keep source, split crops, confidence, view/pose labels, identity features and rig version linked.
- Props/objects: up to ten 3x2 sheets, split into individual objects/views with role, scale, material, orientation and interaction affordances.
- Places/backgrounds: single references and up to ten 3x2 sheets, preserving location ID, camera direction, time/weather/light state and spatial landmarks.
- Split pipeline: detect grid/gutters; crop every cell with margins; reject empty/duplicate crops; preserve the original; never silently discard a cell; write a reversible manifest and source hash.
- Mesh/rig: every visual has an explicit rig status. Character rigs need landmarks, occlusion-aware layers, topology, weights, joint limits, expression/mouth regions and validated bind pose. Props/backgrounds get suitable meshes or parallax/deformation rigs rather than a fake human skeleton.
- Reader preflight: resolve every script noun, action, place, prop and weather cue to a reusable asset or missing-asset request before expensive rendering. A character image does not satisfy a missing prop or location.
- Shot conference: store separate role reports with evidence grounded in exact frame bytes/hash, shot number, scene/asset revision, model/provider and contract version. Director inspects the same image and explicitly dispositions disagreements. Each repair invalidates previous approval.
- Release gate: export only after every shot passes the conference, deterministic motion/floating checks pass, the output decodes, and reference-video/frame diagnostics are attached. Missing original identity fixtures remain missing; synthetic fixtures are never evidence of continuity.

## Limits

This remains an Android sprite/mesh renderer with procedural physics, not a full USD/Blender studio pipeline. A four-role vision conference is a control system, not a guarantee of Pixar-level artistry. Build success and both supplied films' visual QA remain required release gates.
