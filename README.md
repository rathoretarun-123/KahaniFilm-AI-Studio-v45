# KahaniFilm v47 — films of the seventh round (17 October 2026, night)

These films were rendered on the desktop (`tools/jvm/.../MakeFilm`) from branch `ccr-a5fc4eef-d2p2nm` at 960×540 and
24 fps with the frame audit on: the two drawn films and the sofa story from commit `5b799a7` (drawn figures with volume,
the open book redrawn), the other three from `b70780e` (that change does not touch them).

The phone app runs the same core code (`FilmJob` uses the same Director, Casting, Renderer and frame audit). These are
the studio's own 2D renders: cut-out pictures on a mesh rig, with drawn effects. They are **not** Pixar-quality 3D
animation.

**Voices.** The films published earlier today had no voices: the speech program (`espeak-ng`) was missing on the
machine that made them. It is installed now, and every line of every film below is spoken. `espeak-ng` is a robotic
voice. The better Edge voices could not be reached from this machine. On a phone, the phone's own speech engine is used.

| Film | Pictures | Length | Lines spoken | Frame audit (every character in every frame) |
|---|---|---|---|---|
| [सोफ़े वाली जादू की बातें](sofe_wali_jadu_user_pictures.mp4) | the user's pictures | 1:32 | 9 of 9 | 0 findings |
| [आखिरी चिट्ठी](aakhiri_chitthi_user_pictures.mp4) | the user's pictures | 2:25 | 7 of 7 | 2 jolts at the limit (परी, 2.1% at 0:46.5 and 1:57.3); 1 picture change without a cross-fade (परी, 1:57.2) |
| [बाबू के लिए](babu_ke_liye_user_pictures.mp4) | the bundled reference pack | 2:03 | 4 of 4 | 0 findings |
| [आखिरी चाय](aakhiri_chai_user_pictures.mp4) | the bundled reference pack | 3:12 | 10 of 10 | 0 findings |
| [आखिरी जोड़ी](aakhiri_jodi_drawn.mp4) | drawn by the studio (no pictures) | 1:06 | 6 of 6 | 0 findings |
| [आखिरी चाय](aakhiri_chai_drawn.mp4) | drawn by the studio (no pictures) | 2:31 | 11 of 11 | 0 findings |

The audit checks every frame for:

- jolts: a change of speed over 2% of the height between frames;
- jumps: a move over 12% of the height in one frame;
- size pops: a change of size over 4% in one frame;
- a picture swapped inside a shot without a cross-fade;
- more than one face drawn for one character.

`sheets/` has one still per second of every film on contact sheets (8 across). `production/` has each film's
production document, shot list, and audit summary with the voice count.

## Changed this round, as seen in these frames

- **The sofa story.** The girls sit on the sofa in the user's own room picture, at its size, facing each other. The
  teddy sits between them, then goes into the little one's arms as she speaks. The book is open in her sister's lap,
  then closed when the line says so. The room's own red cushion is not pasted over a second time.
- **No purple edge.** The user's pictures were cut out from a magenta ground, which left a purple line round hair and
  outlines. It is cleaned off.
- **Grounded sitters.** A soft shadow on the seat and back cushion under each sitter. Hands on held things are shaded.
- **Scene changes.** Title to first scene and last scene to end now dissolve.
- **Drawn figures with volume** (आखिरी जोड़ी, आखिरी चाय drawn): every solid colour is lit from above and the left, so
  heads and hands read round and clothes are lighter at the top and in shade at the hem. They are still simple shapes.
- **The open book** shows the user's cover round its pages, curved leaves and a shaded fold.

## Still visible (not fixed)

- The open book's pages are drawn (blank lines), not the user's picture of the book open.
- The teddy in the little one's arms is a picture held in front of her own arm, not wrapped in it.
- आखिरी चिट्ठी: the two jolts and the one picture change listed above.
- बाबू के लिए: in medium shots of the shared bench, his photo's bench and the drawn bench can both be seen; the girl's
  kneeling is drawn standing.
