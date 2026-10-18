# KahaniFilm v47 — films of the tenth round (18 October 2026, morning)

These films were rendered on the desktop (`tools/jvm/.../MakeFilm`) from commit `5ad993c` on branch
`ccr-a5fc4eef-d2p2nm`, at 960×540 and 24 fps with the frame audit on. The APK of the same commit passed CI (run 354):
https://github.com/rathoretarun-123/KahaniFilm-AI-Studio-v45/releases/download/v47-build-354-5ad993c/KahaniFilm.apk

The phone app runs the same core code (`FilmJob` uses the same Director, Casting, Renderer and frame audit). These are
the studio's own 2D renders: cut-out pictures on a mesh rig, with drawn effects. They are **not** Pixar-quality 3D
animation.

**Voices.** Every line is spoken by `espeak-ng`, a robotic voice. On a phone, the phone's own speech engine is used.

| Film | Pictures | Length | Lines spoken | Frame audit (every character in every frame) |
|---|---|---|---|---|
| [सोफ़े वाली जादू की बातें](sofe_wali_jadu_user_pictures.mp4) | the user's pictures | 1:25 | 9 of 9 | 0 findings |
| [आखिरी चिट्ठी](aakhiri_chitthi_user_pictures.mp4) | the user's pictures | 2:19 | 7 of 7 | 2 jolts at the limit (परी, 2.1% at 0:43.0 and 1:53.8); 1 picture change without a cross-fade (परी, 1:53.7) |
| [बाबू के लिए](babu_ke_liye_user_pictures.mp4) | the bundled reference pack | 1:56 | 4 of 4 | 0 findings |
| [आखिरी चाय](aakhiri_chai_user_pictures.mp4) | the bundled reference pack | 3:06 | 10 of 10 | 0 findings |
| [आखिरी जोड़ी](aakhiri_jodi_drawn.mp4) | drawn by the studio (no pictures) | 0:59 | 6 of 6 | 0 findings |
| [आखिरी चाय](aakhiri_chai_drawn.mp4) | drawn by the studio (no pictures) | 2:25 | 11 of 11 | 0 findings |
| [भाव — a test of feelings and a walk](bhaav_feelings_and_walk_test.mp4) | the user's pictures of the older girl | 0:30 | 5 of 5 | 0 findings |

The audit checks every frame for:

- jolts: a change of speed over 2% of the height between frames;
- jumps: a move over 12% of the height in one frame;
- size pops: a change of size over 4% in one frame;
- a picture swapped inside a shot without a cross-fade;
- more than one face drawn for one character.

`sheets/` has one still per second of every film on contact sheets (8 across); `bhaav.png` is one still every second of the test film. `production/` has each film's
production document, shot list, and audit summary with the voice count.

## Changed this round, as seen in these frames

- **Walking where the script says.** "(वृंदा कमरे में दाईं तरफ चलकर जाती है)" was not staged at all before: she stood
  still. Now she turns to her side picture, walks to the right with her legs stepping, the camera follows, and she
  turns back to the front (भाव, 0:17–0:21).
- **The mouth on the lips.** On the older girl's front picture, a dark slit opened on her blushed cheek as she spoke
  (the lip finder took the blush for her lips). Her own lips now part as she speaks.
- **Stronger feelings on pictures.** Happy, sad, angry, surprised and scared read clearly on the face, and the body
  slumps, leans and steps back more. Gestures with the words are about twice as large. Each held shot makes one slow
  camera move (a push in on faces, a drift on wides).
- **Pacing.** The title page holds 4 s and the end page 5 s (were 7.5 s and 8 s), so every film is a few seconds
  shorter.

## Still visible (not fixed)

- These are 2D pictures moved on a mesh, not 3D models: a head turns only as far as the user's pictures show it, and
  arms and legs bend only where the rig can bend a flat picture.
- The little one's three-quarter pictures have no lips found, so in those views her mouth does not move.
- The open book's pages are drawn (blank lines), not the user's picture of the book open.
- आखिरी चिट्ठी: the two jolts and the one picture change listed above.
- बाबू के लिए: in medium shots of the shared bench, his photo's bench and the drawn bench can both be seen; the girl's
  kneeling is drawn standing.
