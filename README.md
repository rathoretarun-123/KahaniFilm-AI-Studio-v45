# KahaniFilm v47 — films rendered from code `a815d06`

These films were rendered on the desktop (`tools/jvm/.../MakeFilm`) from branch `ccr-a5fc4eef-d2p2nm` at commit
`a815d06`: 960×540, 24 fps, with the frame audit on. The phone app uses the same core code (`FilmJob` runs the same
Director, Casting, Renderer and frame audit). These are the studio's own 2D renders: cut-out pictures on a mesh rig,
with drawn effects. They are **not** Pixar-quality 3D animation.

बाबू के लिए was rendered at `a815d06`. The other four were rendered from the two commits before it (`9800f25`, and for the
two drawn films `67b6dcf`). Neither later change alters those four films' plans, which were compared line by line, or
their pictures: the dissolve fix acts only on the user's pictures, and the drawn films have none.

| Film | Pictures | Length | Frame audit (every character in every frame) |
|---|---|---|---|
| [आखिरी चिट्ठी](aakhiri_chitthi_user_pictures.mp4) | the user's pictures | 2:24 | 1 jolt at the limit (परी sitting down, 1:56.1, 2.0%); 1 picture change (1:55.9); 0 extra faces |
| [बाबू के लिए](babu_ke_liye_user_pictures.mp4) | the bundled reference pack | 2:06 | 0 findings |
| [आखिरी चाय](aakhiri_chai_user_pictures.mp4) | the bundled reference pack | 3:07 | 0 findings |
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

## Still visible (not fixed)

- बाबू के लिए scenes 4–5, and the end of आखिरी चिट्ठी: where two share the bench, the old man is drawn with the rig on a
  drawn bench, and he reads as half standing.
- बाबू के लिए 0:44: the over-the-shoulder shot shows the girl's face in the foreground, not her back. Her kneeling
  ("घुटनों के बल") is drawn standing.
- In the shared-bench shots of बाबू के लिए, a thin pale sliver shows beside the bench's left leg (source not found).
- आखिरी चिट्ठी: रघुवीर जी's glasses appear only in the last part.
