# Everyday Activity, Natural Effects and Physics Acceptance Matrix

## Purpose
A believable animated character must be able to perform ordinary life tasks as connected, staged actions—not merely change a pose. This matrix is a required engineering and review contract for the director, rig, prop system, renderer, sound and quality checks.

## Important status distinction
`EverydayPhysics` supplies deterministic motion primitives (projectile/floor contact, damped settling, eased transitions, gait cycle, stable noise and contact weights). It is a foundation, not a complete rigid-body engine, not a full body rig, and not proof that the current renderer already drives every action with it. Every feature below must be wired into the actual film pipeline and tested in rendered output.

## Action grammar
Represent each task as an ordered action graph with:
- **Preconditions:** actor identity, body capability, location, target object, object affordance, free hands, reachable distance, support surface and current posture.
- **Phases:** approach/orient, anticipate, contact/grasp, exert or manipulate, release/transfer, recover/settle.
- **Constraints:** joint limits, balance/support polygon, collision avoidance, object attachment, surface contact, reachability and no interpenetration.
- **Continuity:** preserve object identity, hand assignment, clothing/accessories, wetness/dirt, posture and screen direction across cuts.
- **Failure handling:** if the asset, grip or target is unavailable, choose a truthful alternative or flag for human review—never silently invent a canonical story fact.

## Required everyday activity families
1. Locomotion: stand, sit, lie, kneel, crouch, crawl, walk, run, turn, stop, climb stairs/ladder, jump, stumble, fall safely, recover, balance, carry while moving.
2. Personal care: wash hands/face, bathe (non-explicit), brush teeth, comb hair, dress/undress ordinary clothes, tie footwear, towel dry, apply makeup, shave, use a mirror.
3. Eating/drinking: prepare a plate, pick up food, use spoon/fork/chopsticks where appropriate, bite, chew, swallow, sip from cup/bottle, pour liquid, wipe mouth, set utensils down.
4. Household: sweep, mop, dust, wipe a surface, make a bed, fold clothes, wash/fold dishes, open/close drawers and doors, pick up/drop/place objects, sort, pack, unpack, carry a basket, use a broom.
5. Cooking: reach for ingredients, chop with safe tool handling, stir, knead, roll, pour, ladle, open a container, place pot on stove, handle hot cookware with protection, serve food; liquids and steam must react plausibly.
6. Social: greet, wave, handshake, hug, comfort, point, beckon, listen, interrupt politely, exchange objects, take turns, react while another character speaks, maintain gaze and interpersonal distance.
7. Work/study: sit at a desk, write, draw, type, read, turn pages, use a phone, use a tool, inspect a small object, lift/move a box, collaborate, present and respond.
8. Travel/transport: enter/exit a vehicle, open door, sit and fasten seat belt, ride bicycle, mount/dismount an animal, carry luggage, push/pull a cart, get on/off steps; props and passengers must stay attached to the correct supports.
9. Outdoor interaction: walk on uneven ground, step over obstacles, collect/plant/water, throw/catch a ball, sweep leaves, use an umbrella, react to wind/rain/snow, leave footprints and make splashes when surfaces justify them.
10. Emotion/body language: breathe, blink, shift weight, fidget, hesitate, look away, turn toward a sound, react to surprise/fear/sadness/joy, cry with tear paths, laugh, recover after exertion, and show the listener's reaction during dialogue.
11. Animal actions: species-appropriate gait, sniff, eat/drink, sit/lie/stand, shake water off, groom, react to touch/sound, jump and land, tail/ear/wing secondary motion.
12. Interaction with environment: push/pull, open/close, lift/set down, lean, brace, sit on a chair, lie on a bed, step onto a stair, press a switch, light/extinguish a lamp, pick up a dropped item and hand it to another character.

## Extended activity catalogue for reader and director
The script reader should recognise these actions when they are explicit in the source, infer only ordinary staging details, and add reversible director-facing action notes. The director should plan the required body mechanics, props, contacts, reactions, sound cues and continuity; it must not claim a capability is implemented unless the renderer can show it.

### Mobility, vehicles and public places
- Drive a car: approach, unlock, open door, enter, sit, belt in, hands to wheel, mirror check, start/stop, steering, indicator, braking, parking, unbelt, exit and close door.
- Ride bicycle/cycle: mount, push off, pedal cycle, steer, brake, dismount, park and lean it securely.
- Board and exit a bus: approach curb, queue, step up, hold rail, pay/tap where relevant, move to seat/handrail, maintain balance with bus motion, signal stop, descend and clear the vehicle.
- Ride a horse: approach from safe side, mount with support, hold reins, seat balance, walk/trot/canter as appropriate, halt and dismount; tack and rider must remain attached consistently.
- Ride an elephant: mount/dismount only using a plausible platform or handler-assisted method, stable seated posture, appropriate handholds, animal gait and weight shift; no unsafe teleporting onto its back.
- Use stairs, ramps and ladders; enter/exit a lift, press a floor button, wait for doors, step in/out, turn in a confined space and respect other passengers.
- Climb a tree: inspect/grasp branches, shift weight one limb at a time, establish foot contacts, avoid branches that cannot support the character, descend under control.
- Use a playground: sit on a seesaw and counterweight the other rider, pump or be pushed on a swing, climb and slide down a slide, land with bent knees, queue and take turns.

### Home, personal care and communication
- Watch TV with attention shifting to screen/companions; use TV/AC remote with a visible aim, button press, device response and battery/line-of-sight limitations when relevant.
- Use a mobile phone: unlock, dial, answer, hold to ear, switch hands, speak/listen, end call; type a message with gaze alternating between screen and keys, send and react to the reply.
- Use an old-style landline: lift handset, dial rotary or push buttons according to the model, hold receiver to ear and mouth, wind/unwind cord naturally, replace handset on cradle and trigger hang-up.
- Brush teeth: pick up toothbrush, apply toothpaste, brush front/side/inner surfaces, spit into sink, rinse and return items; mirror gaze and water taps must be coherent.
- Bathe/shower: turn taps, test temperature, wet/rinse, use soap/shampoo, rinse, turn water off, towel dry and dress; preserve privacy and avoid explicit nudity.
- Change clothes: choose/collect garment, remove/put on items in plausible order, thread arms/legs, adjust, fasten buttons/zips, check fit; use a screen/curtain or off-camera transition when modesty is appropriate.
- Open/close doors, windows, curtains, blinds and shutters: reach the correct handle/latch, turn/unlock, push/pull on the hinge axis, stop at frame/limit, latch; wind can rattle or move only unlatched/lightly latched items.
- Rotate a ceiling/table fan with start-up and coast-down, pull cord or press switch, adjust speed/oscillation; nearby paper, curtains and hair respond only when close enough to the airflow.
- Read a newspaper/book: hold both hands plausibly, track the text, turn one page at a time, fold newspaper as needed, adjust posture, look up to react and put the reading material down without changing its identity.
- Use a desktop computer/laptop: sit, pull chair, open laptop/lid, type with both hands, use mouse/trackpad, look at display, scroll, plug/unplug, save/close, shut lid and stand. Screen content should not be invented as legible unless supplied or generated deliberately.
- Exercise: warm-up, stretches, squats, push-ups, yoga, skipping, lifting, cycling/treadmill; maintain safe joint alignment, balance, fatigue and breathing.

### Food, work, chores and animals
- Eat with utensils or hands appropriate to context; pick up, bite, chew, swallow, sip, wipe, share, serve, clear dishes and maintain food level/identity.
- Drink tea from cup/glass: hold handle or vessel appropriately, lift to mouth, tilt slightly, sip, return to saucer/table, react to heat; steam rises and dissipates, liquid level should not refill itself.
- Cook, pour, stir, chop safely, knead, serve, wash dishes, sweep/mop/clean floor, wring cloth, dust shelves, make bed, fold clothes, iron with safe handling, wash clothes, take out rubbish and organise items.
- Milk a cow or buffalo: approach calmly, respect animal position, prepare a clean pail, sit/crouch beside the animal, hand contact and alternating squeeze/release rhythm, stream into container, stop and move away safely; show the correct animal and plausible udder/hand scale.
- Groom/feed/water domestic animals; fit a lead or saddle with continuity, brush fur, collect eggs, feed poultry, walk a dog, clean a stable and react to animal movement.
- Smoking, where the story explicitly calls for it and local context permits: retrieve cigarette, light it, draw/exhale smoke, hold ash safely, extinguish and dispose of it; ember glow and smoke must be subtle, wind-directed and not appear without the cigarette. Do not add smoking as a default idle action.
- Read/write, study, shop, count money, sign papers, pack luggage, carry boxes, sweep a courtyard, wash a vehicle, garden, plant, water, mend an item, use hand tools, open containers and exchange objects.
- Social and public interactions: handshake, hug, bow, wave, point, queue, pay, greet, help someone up, hand over an item, catch/throw a ball, dance, clap, sing, comfort, argue, listen and interrupt with believable interpersonal spacing.

### Reader-to-director action protocol
For every recognised action, the reader should emit a structured suggestion with: `action family`, `required prop(s)`, `start state`, `ordered beats`, `contact points`, `body support/balance`, `environment response`, `sound opportunities`, `continuity checks`, and `uncertainties`. The director may only schedule actions supported by parsed story text or user-provided canon. If no matching rig, prop or renderer exists, add a clear capability warning instead of implying the action will render correctly.

## Natural-effects and physical-response requirements
- Gravity and collisions: no floor penetration; stable resting contacts; plausible bounce and friction; object mass affects response.
- Balance: anticipation and counter-motion before lifting/reaching; feet reposition when the centre of mass would leave the support area.
- Grasping: hand approaches from a reachable direction, closes only at contact, object follows the hand while held, release has no teleportation.
- Secondary motion: hair, cloth, bags, straps, ears, tails, leaves and carried objects lag and settle with damped motion; avoid perpetual vibration.
- Fluids: pours have a source and target, flow direction and spill consequences; liquid level/volume should not spontaneously change.
- Weather: wind direction is consistent across hair, foliage, smoke, cloth and particles; rain hits surfaces, splashes only on suitable materials and leaves wetness that fades over time.
- Fire/smoke: buoyancy, flicker and wind drift; no smoke passing through solid barriers without a reason.
- Footing: different friction for dry ground, mud, snow, wet floor and stairs; avoid skating, foot sliding and floating shadows.
- Sound: footsteps and impacts match contact timing and material; object sounds occur at contact, not arbitrary times.
- Determinism: same actor/scene/time/seed renders the same state regardless of frame order or parallel render scheduling.
- Performance: adapt mesh detail to projected screen size; one-screen-pixel subdivision is a ceiling/target for suitable deforming image grids, not a command to create millions of triangles for every 3D character. Use LOD, culling and bounded memory.

## Acceptance tests before claiming completion
- Unit tests for each motion primitive, contact, reach, grip, balance, spring settle, gait and determinism.
- Scenario tests for each activity family with two actors and at least one manipulated prop.
- Render tests at beginning, contact, maximum force, release, recovery and final resting state.
- Automated checks for penetration, disconnected props, foot sliding, pose discontinuity, off-screen action, object identity and excessive frame-time/memory.
- Android device tests covering low-memory interruption, upload/camera/audio recording, preview, export, app pause/resume and long-film cancellation/recovery.
- Human visual review for natural timing, silhouette, acting, emotion, material response and shot continuity.

## Release gate
Do not label a feature “complete” merely because the APK compiles. Mark each action family as **implemented**, **render-tested**, **device-tested**, and **human-reviewed** separately. The target is feature-film quality; that requires iterative rendered-scene evaluation and a real character rig/animation system, not only a successful build.
