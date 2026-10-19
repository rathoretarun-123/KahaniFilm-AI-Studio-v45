# Open-source survey for KahaniFilm v47 (19 October 2026)

Two parts: what the studio tested itself today on the user's own pictures, and a survey of open-source work (code,
models, papers) that could take each part of the engine closer to a Pixar-like film. Only permissive licences
(MIT, Apache-2.0, BSD, ISC, zlib, CC0) can be linked into the app; GPL/AGPL and non-commercial models are excluded.

## Tested today on the user's pictures (102 pictures of आखिरी चिट्ठी, 12 of the sofa story, 6 of भाव)

| Model | Licence | Result | Status |
|---|---|---|---|
| YuNet face detector (OpenCV zoo), 230 KB | MIT | eyes, nose and mouth corners right on every visible face but three, none on backs or things | **in the app** (`core/FaceModel`, ONNX Runtime 1.20) |
| MediaPipe BlazePose (OpenCV zoo ONNX) | Apache-2.0 | right on the old man's whole figures; wrong on the little girl (her big head put its shoulders on her face) | not used |
| MoveNet Thunder (from the MIT `@vladmandic/human-models` package, converted to ONNX here; the full-precision model, 25 MB — an 8-bit version moved joints by 100 px and was dropped) | Apache-2.0 model, MIT package | 17 joints right on the old man standing, sitting, bending and crouching, and sensible on the little girl's whole figures (rougher on half figures); a profile's nose ahead of its ear on all four side pictures | **in the app** (`core/PoseModel`): the rig's hips and knees at the joints, the forearm bending at the elbow, a side picture's facing from nose and ear (`PoseModelTest`) |
| MediaPipe FaceMesh (same package, converted to ONNX here, 3 MB) | Apache-2.0 model, MIT package | the lips' outline and opening and each eye's corners and lids right on every front and three-quarter face of the old man (glasses too); profiles score low and are left out | **in the app** (`core/FaceMesh`): the mouth on the lips' opening, a mouth already open never opened further, the blink's lid sized to the eye, no lid over eyes already shut |

Researched on 2026-10-19. Format for each item: licence; what it improves; how to use it from Java; size; effort (S/M/L). **(?)** marks anything I could not fully verify.

## 1. Rigging and deformation of 2D pictures
- **Meta AnimatedDrawings**, https://github.com/facebookresearch/AnimatedDrawings. MIT covers the code *and* the model weights (LICENSE file plus a README statement). How the pipeline works (I read the source):
  - **Detection.** A fine-tuned Mask R-CNN (ResNet50-FPN), run through TorchServe on mmdet 2.27.
  - **Pose.** An MMPose 0.29 top-down model that outputs the 17 COCO keypoints. The backbone is reportedly ResNet-50 **(?)**.
  - **Segmentation.** No neural network: adaptive threshold, morphological close and dilate, flood fill from the borders, keep the largest contour, fill holes (`examples/image_to_annotations.py`).
  - **Mesh.** Marching-squares contour, then `approximate_polygon(tol=0.25)`, plus interior points on a 40x40 grid, then Delaunay, then drop triangles whose centroid falls outside the outline.
  - **Deformation.** ARAP (Igarashi 2009 two-step solve, `animated_drawings/model/arap.py`) with the joints as pins. A BFS assigns each triangle to its nearest bone, and the draw order of body-part groups gives layering, such as an arm in front of or behind the torso.
  - **Weight sizes.** `drawn_humanoid_detector.mar` is 327 MB and `drawn_humanoid_pose_estimator.mar` is 375 MB (from HTTP headers). Too big for the phone.
  - **What to port.** Segmentation, meshing, ARAP and bone assignment are pure algorithms, a few hundred lines of Java (S–M). This gives smooth bending at elbows and knees, which the current shoulder-only arms lack.
- **Igarashi ARAP 2005, Deform2D** by R. Schmidt, https://www.dgp.toronto.edu/~rms/software/Deform2D/. C++ reference code that bundles WildMagic 2 and has no licence statement **(?)**. Read it as a reference only and port the MIT Meta version instead. The meshes are small (<1,000 vertices), so a dense Cholesky factorisation in Java, computed once per pose picture, is fine.
- **Triangulation** (all pure Java):
  - earcut4j (ISC): https://github.com/earcut4j/earcut4j. Gives sliver triangles, so it is poor for deformation.
  - jdiemke delaunay-triangulator (MIT): https://github.com/jdiemke/delaunay-triangulator. Best fit for the AnimatedDrawings meshing recipe. Effort S.
  - poly2tri.java (BSD-3), constrained Delaunay: https://github.com/orbisgis/poly2tri.java.
- **Spine runtimes**: proprietary "Spine Runtimes License", which requires a Spine Editor licence. Not usable.
- **DragonBones runtimes** (DragonBonesJS is MIT): https://github.com/DragonBones/DragonBonesJS. Good reference for mesh-skinning data formats. I found no maintained Java runtime **(?)**.
- **Inochi2D** (BSD-2-Clause, D language): https://github.com/Inochi2D/inochi2d. Live2D-style parameter keyforms (head turn, mouth, brow) blended over meshes. The *idea* (blend 2–4 pose pictures by a parameter) is worth copying. A full port is L.

## 2. Pose and skeleton models (ONNX)
- **RTMPose / RTMO** through rtmlib (Apache-2.0): https://github.com/Tau-J/rtmlib.
  - Ready ONNX files for RTMPose-t/s/m with 17 points (256x192) and 26-point Halpe models, RTMO-s, and whole-body DWPose.
  - YOLOX person detectors trained on **Human-Art** (which includes cartoons and illustrations). The Human-Art dataset itself is non-commercial (README), so the commercial status of those weights is uncertain **(?)**.
  - Use: run once per imported picture with ORT Java to auto-place shoulders, elbows, wrists, hips, knees and ankles. Size: t/s are tens of MB **(?)**. Effort S–M.
- **MoveNet Lightning/Thunder**: Google (TF Hub / Kaggle). ONNX conversions are in PINTO_model_zoo `115_MoveNet`, whose folder LICENSE is Apache-2.0: https://github.com/PINTO0309/PINTO_model_zoo/tree/main/115_MoveNet.
  - Input 192x192 (Lightning) or 256x256 (Thunder). Lightning int8 is about 2.8 MB.
  - Lower accuracy than RTMPose and trained on real humans, so test it on the Pixar-style renders.
- **Cartoon-trained models**: the AnimatedDrawings pose model is MIT but large and needs an mmpose-to-ONNX export (M–L). Use it only if RTMPose fails on your art.
- **If pictures lack alpha**: U-2-Net (Apache-2.0, https://github.com/xuebinqin/U-2-Net) or anime-segmentation (Apache-2.0, https://github.com/SkyTNT/anime-segmentation) as ONNX.

## 3. Face landmarks for lip sync and expressions
- **MediaPipe Face Landmarker** (Apache-2.0 repo).
  - The bundle has three parts: BlazeFace at 192x192, Face Mesh V2 at 256x256 giving **478 3-D points**, and a blendshape model giving **52 expression scores** from 146 landmarks.
  - ONNX conversion with a parity check: https://huggingface.co/fernandotonon/QtMeshEditor-facemesh-onnx (Apache-2.0).
  - Gives inner and outer lip contours (warp the picture's own lips precisely), eyelid contours (blinks) and brows (emotions). A few MB. Effort S–M.
  - Pixar-style faces are near-photoreal in proportion, so detection probably works, but I have not tested this **(?)**. YuNet output can supply the crop.
- **RTMPose face6** (106 points, ONNX in rtmlib). An Apache-2.0 alternative.
- **InsightFace 2d106**: models are restricted to "non-commercial research", so exclude.
- **PFLD**: the licence of the main repo is unclear **(?)**, so skip.
- **hysts/anime-face-detector**: 28 points, MIT according to the model cards, https://huggingface.co/hysts/anime-face-detector-hrnetv2. Fallback for very stylised faces. Needs an ONNX export (M).

## 4. Lip sync
- **Rhubarb Lip Sync** (MIT; all dependencies permissive): https://github.com/DanielSWolf/rhubarb-lip-sync.
  - Mouth shapes: 6 basic (A–F) plus 3 optional (G, H, X), with a Preston-Blair export mapping.
  - Recognisers: PocketSphinx (English only) and a "phonetic", language-independent recogniser. Quality on Hindi is not documented **(?)**.
  - The valuable, portable part is `rhubarb/src/animation/`: tweening, timing optimisation, pause animation, shape rules.
  - Recommended route: port those rules to Java (M). Feed them phonemes from a Devanagari rule-based G2P (Hindi script is nearly phonemic) with timings from the TTS durations or from the audio energy envelope. The Rhubarb CLI could also run on the desktop renderer to pre-bake TSV/JSON.

## 5. Natural phenomena and physics
- **Ten Minute Physics** (Matthias Müller). Each file carries an MIT header: https://github.com/matthias-research/pages (`tenMinutePhysics/14-cloth.html`, `17-fluidSim.html`, `18-flip.html`). Small JS demos of PBD cloth, an Eulerian smoke/fire grid and FLIP water. They port line-for-line to Java (S–M).
- **Stam "Stable Fluids"**: patent US 6,266,071 is listed as "Expired – Lifetime" on Google Patents (https://patents.google.com/patent/US6266071). Smoke, fire, steam and dust at low grid resolution, upsampled.
- **XPBD/Verlet chains** (algorithm): hair locks, dupatta/saree ends, ear-rings, tails. Use 3–6 particle chains attached to bones. S.
- **Cartoon Animation Filter** (Wang et al., SIGGRAPH 2006): https://grail.cs.washington.edu/wp-content/uploads/2015/08/wang-2006-tca.pdf. Subtracting a smoothed Laplacian of Gaussian of a motion curve adds anticipation, follow-through and squash-and-stretch. It is about 20 lines on any trajectory. Very high value for the technical director. S.
- **Rigid bodies for carried and thrown objects**, all permissive:
  - JBox2D (BSD-2, pure Java but old): https://github.com/jbox2d/jbox2d.
  - dyn4j (BSD, pure Java, maintained): https://github.com/dyn4j/dyn4j.
  - Box2D v3 (MIT, C, JNI): https://github.com/erincatto/box2d.
- **Wind and rain**: curl noise over OpenSimplex2 (CC0, Java version included): https://github.com/KdotJPG/OpenSimplex2. One wind field drives the rain streak angle, cloth chains and particles together. S.

## 6. Pixar-like lighting in 2D
- **Depth Anything V2 Small**: 24.8M parameters, **Apache-2.0**. Base, Large and Giant are CC-BY-NC, so avoid them. https://github.com/DepthAnything/Depth-Anything-V2.
  - Run it once per picture at import (about 100 MB fp32; fp16/int8 is smaller **(?)**) and convert depth to a cached normal map.
  - The renderer then adds rim light, key/fill shading, ambient occlusion and soft contact shadows. Effort M.
- **MiDaS v2.1 small** (MIT, ~21M parameters): https://github.com/isl-org/MiDaS. Alternative; has an official mobile path.
- **No-ML option**: silhouette "inflation" normals from a distance transform of the alpha mask, the Lumo idea (Johnston, NPAR 2002). The related patent US7239314 is probably expired **(?)**. A richer 3-D proxy approach is in Ink-and-Ray (Sýkora et al., TOG 2014). S.
- **Laigter normal-map tool**: GPL-3, usable as an external tool only, never linked into the app.
- **Grading**: apply 3D LUTs (.cube) with tetrahedral interpolation in Java, and author the LUTs offline. S.

## 7. Script reading and directing
- **Fountain**: https://fountain.io. The reference parser nyousefi/Fountain is MIT (Objective-C): https://github.com/nyousefi/Fountain. There is no maintained Java parser, so extend the custom parser to accept Fountain: scene headings, character cues, parentheticals as emotion cues, transitions. S.
- **The Virtual Cinematographer** (He, Cohen, Salesin, SIGGRAPH 1996): https://www.microsoft.com/en-us/research/?p=150986. Film idioms as small hierarchical state machines per scene type, such as a two-person conversation, choosing shots and cut timing. Its patent US6040841 dates from 1996. Fits the Director/Editor directly. S–M.
- **Thinking Like a Director: Film Editing Patterns** (Wu et al., 2018): https://hal.archives-ouvertes.fr/hal-01950718. A formal language for shot-size, angle and on-screen-layout constraints across a sequence.
- **Toric space** (Lino & Christie, TOG 2015): https://hal.archives-ouvertes.fr/hal-01142876. Framing two subjects by their on-screen positions, reduced to 2-D framing rules.
- **Shot statistics**: MovieShots (46K trailer shots, 5 scale classes, 4 movement classes): https://arxiv.org/abs/2008.03548. Licence unclear **(?)**; use only for statistics, such as shot-length and scale distributions.

## 8. Editor
- **Continuity Editing for 3D Animation** (Galvane et al., AAAI 2015): https://ojs.aaai.org/index.php/AAAI/article/view/9288. Error costs (180° line, jump cuts, gaze and motion continuity, pacing) minimised by semi-Markov dynamic programming. Ideal for picking cuts among candidate shots. M.
- **Computational Video Editing for Dialogue-Driven Scenes** (Leake et al., SIGGRAPH 2017): https://graphics.stanford.edu/papers/roughcut. Idioms as HMMs: start wide, keep the speaker visible, go tighter as emotion rises, avoid jump cuts. Driven by script sentiment.
- **Off-the-shelf auto-editors**: auto-editor (Unlicense; silence cutting) and PySceneDetect (BSD-3; cut detection). They target real footage and are of little value here.

## 9. Hindi TTS on device
- **AI4Bharat Indic-TTS**: the repo LICENSE is MIT. FastPitch plus HiFi-GAN, 13 languages including Hindi. https://github.com/AI4Bharat/Indic-TTS.
  - FastPitch predicts per-token **durations**, which gives perfect lip-sync timing.
  - It needs an ONNX export (M–L). A community quantized ONNX export exists, but its licences are mixed **(?)**.
- **ai4bharat/vits_rasa_13** (VITS, character tokens, so no espeak): https://huggingface.co/ai4bharat/vits_rasa_13. Sources give conflicting licences, CC-BY-4.0 versus MIT **(?)**.
- **Kokoro-82M**: Apache-2.0, has Hindi voices hf_alpha, hf_beta, hm_omega and hm_psi. Its Hindi G2P uses **espeak-ng (GPL-3)**, so you would need your own Devanagari-to-IPA G2P. Roughly 80–300 MB **(?)**.
- **Piper**: the original rhasspy/piper is MIT but archived. Its successor piper1-gpl is GPL. Voices need espeak-ng (GPL). hi_IN-rohan-medium exists, and each voice's licence is in its MODEL_CARD **(?)**.
- **sherpa-onnx** (Apache-2.0, Java/Android API): https://github.com/k2-fsa/sherpa-onnx. Usable, but its Piper and Kokoro paths bundle espeak-ng data (GPL). Since the app already runs ORT Java, calling the ONNX models directly avoids it.

## What of the top 10 is in the app now (19 October, evening)

1. Anticipation, overshoot and follow-through — already in the renderer before this survey (the body dips and leans
   before setting off and settles after arriving; the head's phrase moves overshoot; hair and hems lag): kept.
2. Auto-skeleton at import — **done with MoveNet** (RTMPose's weights are on hosts this machine cannot reach).
3. Smooth bending limbs — **the forearm now bends at the elbow** on the mesh (hips and knees from the joints); a full
   ARAP re-mesh was not attempted.
4. Face landmarks — **done with FaceMesh** (468 points) on top of YuNet.
5. Rhubarb's rules — already in `Mixer.envelope` (the lips lead the sound by 40 ms, never flap shut inside a word,
   glide at most a quarter opening a frame); a Hindi letter-to-viseme mapper was not added.
6. Verlet hair and cloth chains — not done (the pictures' hair is not separated from the head).
7. 180° rule and continuity cuts — every shot looks at the stage from the same side, so each speaker keeps their
   screen side by construction (the bible lists the axis per scene); no new cut-cost model.
8. Depth or normal maps — not done (Depth Anything's weights are on Hugging Face, out of reach here).
9. Grid fluid smoke and 3D LUT — not done; the existing grade (`FilmLook`) and steam are kept.
10. Offline Hindi TTS — see the voices note in the production document.

## Top 10 (most impact per effort first)
1. **Cartoon Animation Filter on every motion curve** (S). Instant anticipation, overshoot and follow-through, the core of a "Pixar feel".
2. **RTMPose-t/s ONNX auto-skeleton at import** (S–M). Adds elbows, wrists and knees with the existing ORT setup and little user effort.
3. **Port AnimatedDrawings meshing plus ARAP (MIT) with Java Delaunay** (M). Smooth bending limbs instead of rigid shoulder-only arms, with no seams.
4. **Face Mesh V2 ONNX (478 points) for lips, eyelids and brows** (S–M). Precise mouth opening on the real lips, believable blinks and brow-driven emotion.
5. **Port Rhubarb's animation rules plus a Hindi G2P viseme mapper** (M). Smooth, non-flickery mouth timing that works for Hindi.
6. **Verlet/XPBD secondary-motion chains for hair and cloth, driven by a shared wind field** (S–M). Characters feel alive between actions.
7. **Editing idioms plus continuity-cost cut selection (180° rule, no jump cuts)** (M). Shot choice reads like real film grammar.
8. **Depth Anything V2 Small, or silhouette inflation, turned into normal maps for rim light, soft shading and contact shadows** (M). The biggest step toward a 3-D "Pixar" look in 2-D.
9. **Stam/Ten Minute Physics grid fluid for smoke, fire and steam, plus 3D-LUT grading** (S–M). Richer atmosphere and consistent filmic colour.
10. **Offline Hindi TTS with durations: Indic-TTS FastPitch ONNX, or Kokoro with your own G2P** (L). Voices plus exact lip-sync timing with no cloud, keeping GPL espeak-ng out.
