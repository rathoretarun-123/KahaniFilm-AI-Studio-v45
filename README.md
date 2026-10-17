# KahaniFilm v47 — final films of the debug round (17 October 2026)

These films were rendered on the desktop (`tools/jvm/.../MakeFilm`) from branch `ccr-a5fc4eef-d2p2nm`, from the core code
as it stood just before commit `a390e66`: 960×540, 24 fps, with the frame audit on. That code includes the four
debuggers' fixes (reader, director, renderer, app), the seated-photo fixes and the dissolve fix. The later commits change
car rides, the story board's height cue and the Studio page. None of these five films has a ride or a story board.

The phone app uses the same core code (`FilmJob` runs the same Director, Casting, Renderer and frame audit). These are the
studio's own 2D renders: cut-out pictures on a mesh rig, with drawn effects. They are **not** Pixar-quality 3D animation.

| Film | Pictures | Length | Frame audit (every character in every frame) |
|---|---|---|---|
| [आखिरी चिट्ठी](aakhiri_chitthi_user_pictures.mp4) | the user's pictures | 2:24 | 2 jolts at the limit (परी at 0:46.5, 2.1%; परी sitting down at 1:56.1, 2.0%); 1 picture change without a cross-fade (परी, 1:55.9); 0 extra faces |
| [बाबू के लिए](babu_ke_liye_user_pictures.mp4) | the bundled reference pack | 2:03 | 1 picture change without a cross-fade (बच्ची, 1:23.7); 0 jolts; 0 extra faces |
| [आखिरी चाय](aakhiri_chai_user_pictures.mp4) | the bundled reference pack | 3:05 | 0 findings |
| [आखिरी जोड़ी](aakhiri_jodi_drawn.mp4) | drawn by the studio (no pictures) | 1:07 | 0 findings |
| [आखिरी चाय](aakhiri_chai_drawn.mp4) | drawn by the studio (no pictures) | 2:22 | 0 findings |

The audit looks for these, frame by frame: jolts (over 2% of the height per frame²), jumps (over 12% of the height in
one frame), size pops (over 4% in one frame), a picture swapped inside a shot without a cross-fade, and more than one
face drawn for one character. `sheets/` has every second of every film on contact sheets (8 across). `production/`
has each film's production document, shot list and audit summary.

## Fixed this round, as seen in these frames

- बाबू's own photo on his bench is no longer lifted onto a drawn bench, so the floating bench-on-bench is gone.
- The dissolve between parts keeps his photo; it used to switch to the rig on a drawn bench for two seconds.
- Scene 3: बाबू is now on his bench while the girl searches her bag. Before, she was alone at the tree.
- The girl's twelve pose pictures are used now. Before, the grey background was read as light hair.
- The over-the-shoulder shots show the back of the near person. Named views ("back", "side") were being dropped.
- बाबू के लिए: where two share the bench, the old man keeps his own seated photo. He used to be drawn with the rig.
- Moves are paced at walking speed, nobody jumps across the stage between shots, and people keep a little space between them.

## Still visible (not fixed)

- बाबू के लिए: in medium shots of the shared bench, his photo's bench and the drawn bench can both be seen.
- आखिरी चिट्ठी, the end: on the shared bench, रघुवीर जी is still drawn with the rig.
- बाबू के लिए: the girl's kneeling ("घुटनों के बल") is drawn standing.
- In the shared-bench shots of बाबू के लिए, a thin pale sliver shows beside the bench's left leg (source not found).
- आखिरी चिट्ठी: रघुवीर जी's glasses appear only in the last part.
