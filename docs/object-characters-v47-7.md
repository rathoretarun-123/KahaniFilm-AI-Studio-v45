# Things as characters, and rigid things that open (v47.7, 17 October 2026)

The user asked for two things:

- "even some animated objects like cars, toys may also have similar activities like speaking and other things like humans" (सर हिलाना, बैठना, खड़े होना);
- "umbrella is rigid but can open, book is rigid but can open".

## Things that are characters

**Before.** A car or a toy in the cast list was read as a person: a MAN, a WOMAN or a child. Its own picture was then
rigged as one, or, being wide, as an animal with a jaw, ears, a tail and legs.

**Now.** Such a character is read as a thing (`Look.OBJECT`, with `objectType`).

- **How it is read.** The name and the first phrase of the description that is not the name say what it is: "चिंटू: एक
  छोटी लाल खिलौना कार" is a car, "भोलू: एक भूरा टेडी बियर" is a teddy.
- **What is never read as a thing:**
  - a person who drives or owns one ("ड्राइवर, कार चलाता है");
  - a doll, a puppet or a robot, which have arms and legs;
  - a likeness ("कार जैसी तेज़ लड़की").
- **Types:** car, train, ball, teddy, toy, kettle, lamp, and any other rigid thing that the description calls talking or
  magic.

**Drawn rigid** (`Rig.build`):

- Its picture is never bent. It gets the face-only rig only when a face is really found in it.
- With no face found, nothing is drawn on it: no lips and no eyes.
- With no picture, the studio draws the thing (`Renderer.drawnObject`) with one pair of eyes and one mouth.

**Acting with its whole body** (`Renderer.objectMotion`). Every part is eased, so nothing jumps between frames.

| What the script says | What it does |
|---|---|
| Its words | A talk bounce: it stretches up a little as its mouth opens (the mouth averaged over a fifth of a second) |
| हाँ में सिर हिलाना (nods) | Tips forward |
| ना में सिर हिलाना (shakes its head) | Rocks side to side |
| Laughs | Shakes with it |
| Sits | Settles down, squashed a little |
| Stands | Rises again |
| A toy walks or runs | Hops (1.8 or 2.4 hops a second; squashed as it lands, stretched in the air) |
| A ball moves | Rolls round with the ground covered |
| A car or train moves | Rolls: drawn wheels turn with the ground, the body bobs on its springs, leans back as it pulls away and dips as it stops |

**How the director changed** (`Director.objectGesture`):

- A thing character takes the whole-body gestures above from the everyday table, which used to give gestures to
  people only.
- Its walks and runs are laid in front of anyone in the way (`passInFront`), as people's are.
- The physics check now steps through its frames too. It is never flagged for "sitting on nothing": it settles where it
  stands.

**Measured** on the test story at 24 fps:

- The largest change between two frames was 2.7° (the car's head shake) and 1.9 % of its height (the teddy's laugh
  bounce).
- The test bounds are 3° and 2 %/2.5 %.

## Rigid things that open

`Rigidity.hinge` records how a rigid thing opens: its leaves part (a book, a letter), its lid turns at the back edge
(a box, a trunk, a tiffin, a postbox), or its canopy spreads (an umbrella). Each part keeps its shape. The material
ledger names it ("opens at its lid").

- **A box opened by the words.**
  - "लकड़ी का बक्सा खोलती है" opens it where it lies, or in the hands: the lid turns back about its back edge and the dark
    inside shows (`Renderer.lidOpen`).
  - Opened where it lies, the box stays there; before, the words picked it up.
  - Words that open its lock ("डाक-बक्से का ताला") do not open its lid.
- **An umbrella opened by the words** ("छाता खोलता है", "opens her umbrella") opens over 1.2 s from then, rain or shine.
  Before, only rain opened it.
- **The user's own picture of a thing open** ("खुली नीली किताब" beside "नीली किताब"):
  - It is that thing's open view. When the thing opens, the closed picture fades into it (`Renderer.openedPicture`).
  - It is never used as the thing itself (`Director.openViewOfAnother`). Before, it could be.
- **Books** already opened and closed in the hands (v47.4). That is unchanged, except that the open picture, when there
  is one, takes the place of the drawn leaves.

## Checked

| Check | Result |
|---|---|
| `ObjectCharactersTest` (6 tests: reading, staging, smoothness, no face → no rig, opening, drawing every second of the story with and without the car's picture) | PASS |
| Frames rendered and looked at | toy car and teddy drawn by the studio; the user's car picture with no face; book, box and umbrella, with and without a character picture |

## Not done

- **Wheels and limbs in a picture.** The wheels in the user's picture of a car do not turn; wheels are not found in a
  picture. A teddy's arms in its picture do not move: it is rigid.
- **Spacing.** The director's spacing does not yet know a thing character's width. A car can stop overlapping the legs
  of someone standing where it stops.
- **A drawn holder.** A drawn person (no picture) holds the studio's drawn book, not the user's book picture. That was so
  before, and the open picture is used only by a character with a picture.
- **Opening in the hands only for a few things.** In the hands, only books, letters and boxes are drawn opening; the
  umbrella opens through its own dome over the head. A laptop, a door or scissors opening are not drawn.
- **The umbrella and a full hand.** An umbrella is not drawn while the same hand holds another thing (an existing rule).
