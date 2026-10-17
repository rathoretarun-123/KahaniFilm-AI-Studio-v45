# Renderer capabilities and research notes — 17 October 2026

This note continues `animation-renderer-research-2026-10-14.md`. That earlier note compares Blender, Godot, OpenToonz,
FFmpeg and ComfyUI and lays out a phased integration. Nothing in it has changed. **No new dependency was added in this
round**, and none of those engines is integrated. The reason: no integration path could be tested here. The Android SDK
host (dl.google.com) is denied by this environment's network policy, and no GPU or engine binary is available.

What changed is that the studio now knows, and says, which of a script's environmental requirements its own renderer
can carry out. It records this per requirement, in the production document (`core/ProductionDocument.ENV_RULES`).

## Capability matrix (what the shared rule table maps each requirement to)

The methods are:

- **PARTICLE_SYSTEM:** drawn by `Nature` from time; there is no time-stepped simulation.
- **PROCEDURAL_ANIMATION:** formula-driven motion of a picture or a mesh.
- **IN_BACKDROP_PICTURE:** present only as the user's picture shows it.
- **SOUND_EFFECT:** synthesised by `Synth`, or a recording from the sound library.
- **UNSUPPORTED:** no method; recorded as an issue.
- **NOT_FILMABLE:** a smell, for example.

| Requirement | Method | In the plan as | Status |
|---|---|---|---|
| Wind | PROCEDURAL_ANIMATION | `W_WIND`: cloth stir, foliage mesh sway | implemented (approximation) |
| Leaves sway on a tree in the place picture | PROCEDURAL_ANIMATION | `Nature.backdropMesh` under wind | implemented (approximation) |
| Leaves lift / fly | PARTICLE_SYSTEM | `W_LEAVES` | implemented |
| Leaves slide along the ground | UNSUPPORTED | — | not implemented |
| Dust storm | PARTICLE_SYSTEM | `W_DUST` | implemented |
| Dust motes drifting in light | PARTICLE_SYSTEM | `W_MOTES` (new) | implemented this round |
| Moving sun patches (dappled light) | PROCEDURAL_ANIMATION | `W_DAPPLE` (new), screen-space | implemented this round (approximation, not cast by the tree) |
| Rain, heavy rain, splashes | PARTICLE_SYSTEM | `W_RAIN` / `W_STORM` | implemented |
| Raindrop rings on puddles | PARTICLE_SYSTEM | the rain's ground splashes | approximation (no rings) |
| Lightning, thunder | PROCEDURAL + SOUND | `FX_LIGHTNING` + `SFX_THUNDER` | implemented |
| Rain stops | PROCEDURAL | the rain weather closes | implemented |
| Water flowing | PROCEDURAL | `W_SEA` animates the picture's water | implemented (approximation) |
| Water receding, drips from a roof edge | UNSUPPORTED | — | not implemented |
| Reflections on water | UNSUPPORTED | — | not implemented |
| Characters' long cast shadows | UNSUPPORTED | contact shadow only | not implemented |
| A light flickering or swaying | UNSUPPORTED | — | not implemented |
| A prop fluttering in wind (a calendar) | UNSUPPORTED | — | not implemented |
| Steam, smoke | PARTICLE_SYSTEM | `FX_STEAM` / `FX_SMOKE` (from description cues) | implemented where a character's sentence names it; an opening description with no one in it is not staged (recorded) |
| A bench rocking on a broken leg | PROCEDURAL | `FX_WOBBLE` (new): a damped rock of the bench picture | implemented this round (the sitters do not rock with it) |
| Birds, bells, rustle, footsteps, horns, hum, sips | SOUND_EFFECT | `Synth` effects | implemented |
| Rain patter | SOUND_EFFECT | ambience "rain", from the user's sound library | plays only when the library has a rain recording |
| A dog barking with no dog in the story, the azaan | UNSUPPORTED | no recording | not implemented |
| Smell | NOT_FILMABLE | — | by definition |

The action side works the same way. Contact gestures (a head on a shoulder, a hand on a hand), kneeling with a picture
that has no leg rig, and taking off a bag that is part of the character's picture are reported as UNSUPPORTED, with the
reason, in the issue register.

## Why no engine was adopted this round

1. **No tested path.** Blender (GPL), Godot (MIT), OpenToonz (BSD-3) and ComfyUI (GPL-3) all need either a desktop or
   server render step, or a GPU and model weights. None can run inside the phone app. None could be installed or exercised
   here: their download hosts were not tested, and the Android SDK host returned 403.
2. **Licensing.** An app bundling GPL code would itself become GPL. A server-side render step would avoid that, but it
   needs a backend this project does not have.
3. **What an engine does not fix.** The gaps that matter most for the user's three scripts are already measured in code:
   - leg rigs for kneeling;
   - contact gestures;
   - a bag baked into a character's picture.

   These are gaps in the art and the rig, not in the renderer, and an engine would not close them without new rigged
   character assets.

## Verification (this round, local)

| Check | Result |
|---|---|
| Plan digest of 2,100 stories, before and after (posture, acts, effects, weather, sounds) | Every change is an intended one (see `docs/production-document-v47.md`) |
| Screenplay formatter on all 2,100 stories: same scenes, speakers and words after formatting | 2,100 |
| Formatter output passes the format rules | 2,100 |
| Formatter idempotent | 2,099; the exception is an emoticon ":-(" inside a description |
| APK build | BLOCKED: no aapt2 / d8 / zipalign / apksigner in this container; `dl.google.com` denied by the network policy; apt has no packages for them |
| App compile | The whole app source tree compiles with javac against android-all-14 |
