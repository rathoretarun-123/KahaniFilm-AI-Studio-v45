package com.tarun.kahani.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Independent script-reader pass. It does not rewrite the author's story or silently invent canon.
 * It detects underspecified transitions and gives the director grounded, reversible staging inferences.
 * Pure Java so the pass can be unit-tested on desktop and on Android.
 */
public final class StoryReasoner {
    private StoryReasoner() {}

    public static final class Finding {
        public final String category, observation, inference, confidence;
        Finding(String category, String observation, String inference, String confidence) {
            this.category=category; this.observation=observation; this.inference=inference; this.confidence=confidence;
        }
        public String toString() {
            return "[Reader/" + confidence + "/" + category + "] " + observation + " -> Director: " + inference;
        }
    }

    /** A recognised physical activity and a compact, reversible staging protocol for the director. */
    public static final class ActivityPlan {
        public final String family, trigger, staging, propNeeds, physics, sound, continuity;
        ActivityPlan(String family,String trigger,String staging,String propNeeds,String physics,String sound,String continuity) {
            this.family=family; this.trigger=trigger; this.staging=staging; this.propNeeds=propNeeds;
            this.physics=physics; this.sound=sound; this.continuity=continuity;
        }
        public String toString() {
            return "[Reader/ACTIVITY/"+family+"] trigger="+trigger+"; staging="+staging
                    +"; props="+propNeeds+"; physics="+physics+"; sound="+sound+"; continuity="+continuity;
        }
    }

    private static final String[][] ACTIVITY_RULES = {
        {"DRIVE_CAR","drive|driving|car|steering wheel|गाड़ी चल|कार चल","approach, open door, sit, belt, mirror check, hands to wheel, start, steer/brake, park, unbelt and exit","car, door, seat, belt, steering wheel","seat support, steering reach, vehicle acceleration and braking, wheels roll only while moving","door latch, engine, indicator, tyre/road ambience","driver, seat, belt and vehicle position remain continuous"},
        {"CYCLE","bicycle|bike|cycling|cycle|साइकिल","mount, push off, pedal, steer, brake, dismount and park","bicycle, pedals, handlebar, brakes","alternating pedal cycle, balance, wheel rotation, grounded tyre contact","chain, tyre and brake sounds at matching moments","hands/feet stay on the correct controls"},
        {"PHONE_MOBILE","phone call|mobile phone|cell phone|calling|फोन पर बात","retrieve, unlock/dial, raise to ear, listen/speak, lower and end call","mobile phone","hand grip, wrist orientation, gaze and arm support","ring/vibration only when the story calls for it","same device and hand unless a transfer is shown"},
        {"PHONE_TEXT","text message|typing message|send a message|whatsapp|मैसेज टाइप","look at screen, hold device, type, pause/read, send and react","mobile phone","thumb/finger contact and gaze alternate between screen and surroundings","tap/vibration only if motivated","message content is not invented; phone remains in hand"},
        {"LANDLINE","landline|rotary phone|telephone receiver|टेलीफोन","lift receiver, dial appropriate model, listen/speak, replace on cradle","landline phone, receiver, cord","cord has slack and bounded pull; handset stays at ear/mouth","ring/dial tone only when indicated","receiver is returned to its cradle to end the call"},
        {"WATCH_TV","watch tv|watching television|television|टीवी देख","settle into seat, orient gaze to screen, react, reach remote if specified","television, seat, optional remote","body supported by seat; eye-line points toward screen","room tone/TV sound only when relevant","screen and remote positions remain stable"},
        {"REMOTE_CONTROL","remote|remote control|ac remote|tv remote|रिमोट","aim toward device, press button, wait for device response","remote control, TV or AC","finger presses buttons; device response follows the press","button click and device beep only if plausible","remote stays with actor or is visibly put down"},
        {"TOOTHBRUSH","brush teeth|toothbrush|दाँत साफ|दांत साफ","pick up brush, apply paste, brush several surfaces, rinse/spit, return brush","toothbrush, paste, sink, tap","hand-to-mouth reach, wrist movement, water follows tap state","tap, brushing, spit/rinse sounds where available","paste/brush stay in hand until placed down"},
        {"CHANGE_CLOTHES","change clothes|get dressed|undress|put on shirt|कपड़े बदल","obtain clothes, change through plausible garment order, fasten/adjust, check fit","garments, buttons/zips, mirror or privacy screen if present","joint range and cloth follow-through; preserve modesty","fabric rustle, fasteners","outfit changes persist across subsequent shots"},
        {"STAIRS","climb stairs|walk upstairs|descend stairs|सीढ़ियाँ चढ़|सीढ़ियाँ उतर","approach, step on each stair, transfer weight, continue, reach landing","staircase, handrail if present","foot contacts match riser/tread; centre of mass remains supported","footsteps change with surface","no teleporting between floors"},
        {"ELEVATOR","elevator|lift|use the lift|लिफ्ट","approach, press call/floor button, wait, enter, turn, ride, exit","lift, doors, buttons","door timing, cabin support and other passengers respected","button chime, motor/door sound if available","floor and character location update only after travel"},
        {"DOOR_WINDOW","open door|close door|open window|close window|दरवाज़ा खोल|खिड़की खोल","reach handle, turn latch, push/pull on hinge axis, stop, latch","door/window, handle, latch","hinge pivot and frame collision; wind moves only unsecured panels","latch/hinge creak, wind rattle","panel remains open/closed as last action dictates"},
        {"CLIMB_TREE","climb a tree|climbing tree|पेड़ पर चढ़","inspect branch, grip, place foot, shift weight, climb one support at a time, descend","tree with reachable branches","branch support, three-point contact, gravity and reachable limb limits","bark/leaf rustle","same branch structure and climbing height across cuts"},
        {"BUS","board bus|enter bus|exit bus|get off the bus|बस में चढ़|बस से उतर","approach, queue if needed, climb step, hold rail, move/seat, stand, descend","bus, steps, rail, seat","vehicle motion affects balance; foot contacts on steps","engine, doors, road ambience","boarding side, seat and travel direction remain continuous"},
        {"HORSE","ride a horse|mount horse|dismount horse|घोड़े पर सवार","approach, mount with support, settle seat, hold reins, ride, halt and dismount","horse, saddle/reins if established","four-beat gait appropriate to speed; rider follows body motion","hoofbeats and tack only on contact","rider stays attached to saddle and reins"},
        {"ELEPHANT","ride an elephant|elephant ride|हाथी पर सवार","approach with handler/platform, mount safely, settle, ride and dismount safely","elephant, platform/handler if present","heavy gait, trunk/ear secondary motion, rider balance","heavy footfalls and animal sounds when motivated","do not teleport rider onto animal"},
        {"MILKING","milk a cow|milk a buffalo|milking|गाय का दूध|भैंस का दूध","approach calmly, prepare pail, crouch/sit beside animal, alternate squeeze/release, stop","cow or buffalo, clean pail, stool if present","plausible hand/udder scale, stable animal position, liquid stream into pail","pail/liquid/animal ambience","animal species, pail fill and hand placement remain consistent"},
        {"CLEAN_FLOOR","mop floor|sweep floor|clean the floor|पोछा लग|झाड़ू लग","grip tool, push/pull in overlapping strokes, reposition, wring/rinse if scripted","broom/mop, bucket, floor","tool head contacts surface; wet patch and drying are local","bristles, mop, bucket sounds","cleaned area progresses rather than resetting"},
        {"READING","read newspaper|read a book|reading a book|अखबार पढ़|किताब पढ़","hold, focus gaze, scan, turn page, look up/react, set down","newspaper or book","hands support pages; page turn uses fingertips","paper rustle/page turn","page order and held object persist"},
        {"PLAYGROUND","seesaw|sea-saw|swing set|play on swing|slide down|झूला|फिसलपट्टी","mount, play through a cycle, coordinate with partner, dismount/land","seesaw, swing or slide, support frame","pivot/pendulum motion, gravity, landing with bent knees","creak/chain/landing sounds","seat, pivot and other rider remain attached"},
        {"EXERCISE","exercise|workout|push-up|pushup|squat|yoga|व्यायाम","prepare posture, perform repeated controlled reps, recover and breathe","mat or equipment only if established","joint alignment, weight, balance, fatigue and breath","breath/footfall/equipment only when motivated","repetitions and equipment state progress"},
        {"COMPUTER","computer|laptop|typing|keyboard|mouse|कंप्यूटर|लैपटॉप","sit, open device, type/use pointer, inspect screen, save/close and stand","computer/laptop, keyboard, mouse, desk/chair","hands contact keys and pointer; chair and wrists supported","keyboard/mouse and fan hum if present","device open/closed state persists"},
        {"TEA","drink tea|cup of tea|chai|चाय पी","pick up cup, orient handle, sip, react to heat, return to saucer","cup/glass, tea, saucer if present","liquid level, tilt and steam respond plausibly","cup/saucer contact and pour sounds","cup fill and held hand stay consistent"},
        {"EAT_DRINK","eat|drinking water|drink water|भोजन करना|खाना खा","pick up food/vessel, bring to mouth, bite/sip, chew/swallow, set down","food, plate, cup or utensil","hand-mouth trajectory, food quantity changes only when consumed","utensil/chew/sip sounds only if available","food and utensil continuity"},
        {"BATHING","take a bath|bathe|shower|नहाना","turn water on, test, wash/rinse, turn off, towel dry, dress","bathroom, tap/shower, soap, towel","water follows gravity and surfaces; wetness persists and dries gradually","water, drain and towel sounds","privacy; water state and wetness carry over"},
        {"SMOKING","smoke a cigarette|smoking|सिगरेट पी","retrieve, light, draw, exhale, hold ash safely, extinguish/dispose","cigarette, lighter/ashtray only if established","ember and smoke are local, wind-directed and time-dependent","lighter/cough/ash only if motivated","never introduce smoking as unprompted idle behaviour"},
        {"COOKING","cook|cooking|fry|boil|stir a pot|खाना पकाना|सब्ज़ी काट","prepare ingredients, use established utensils, stir/turn food, check doneness, serve and turn heat off","stove, pan/pot, utensils, ingredients only when established","hands avoid hot surfaces; steam and liquid follow heat and gravity; food changes only through shown steps","sizzle/boil/chop sounds only when the action warrants them","ingredient, utensil, burner and food state persist"},
        {"GARDENING","water plants|gardening|plant a tree|पौधों में पानी|पौधा लगाना","approach plant, handle watering can/tool, water or tend soil, put tools down","plant, soil, watering can or tool","water follows gravity; soil darkens locally; leaves respond gently to contact","water pour, soil/tool sounds","plant size and watered areas change gradually, not instantly"},
        {"LAUNDRY","wash clothes|do laundry|hang clothes|fold clothes|कपड़े धोना|कपड़े सुखाना","sort, wash/rinse if shown, wring only when plausible, hang/fold and put away","clothes, basin/machine, line, clips or basket as established","wet fabric is heavier and drapes differently; cloth reacts to hand and wind","water, fabric and machine sounds only when present","wet/dry/folded state remains consistent"},
        {"SLEEP_WAKE","fall asleep|wake up|wakes up|go to bed|सो जाना|जागना|उठकर बैठ","settle body, eyelids close/open, breathing slows or quickens, stretch and orient after waking","bed/pillow/blanket only if present","body weight compresses bedding; blanket follows limbs with delay","quiet room tone, bedding rustle, yawn if motivated","time-of-day and sleeping position carry across the transition"},
        {"PHONE_PHOTO","take a photo|take a picture|selfie|photograph|तस्वीर खींच|सेल्फी","raise camera, frame subject, focus/pose, tap shutter, inspect result and lower device","phone or camera","lens points toward target; screen gaze and wrist angle are plausible","shutter sound only if device setting/story supports it","captured image is not invented or displayed unless requested"},
        {"WASH_HANDS","wash hands|washing hands|हाथ धोना","turn tap on, wet hands, soap/rub palms and fingers, rinse, turn tap off, dry","sink, tap, soap, towel","water flow follows tap state and gravity; droplets stay local","tap, water and towel sounds","tap state and wetness carry through the beat"},
        {"LOCK_UNLOCK","lock the door|unlock the door|lock door|unlock door|ताला खोल|ताला लग","reach lock/key/bolt, turn or slide mechanism, test handle where natural","door, lock, key or bolt only if established","hand contacts the actual mechanism; door remains constrained by hinges","key/bolt click and latch sound","locked state persists until explicitly changed"},
        {"WALK_RUN","walk|walking|run|running|jog|चलते हुए|दौड़ना","anticipate, transfer weight, alternate foot contacts, swing arms, decelerate and settle","ground and footwear as established","feet contact ground; body mass and pace affect stride; no foot sliding","footsteps match surface and pace","travel distance and screen direction remain continuous"},
        {"SWIM","swim|swimming|dive into water|तैरना|पानी में कूद","enter water if needed, establish buoyancy, stroke/kick, breathe, turn and exit if scripted","water body, swimwear/floatation only if established","buoyancy, drag, ripples and splashes match body movement","muffled underwater sound or splashes only when viewpoint supports it","wetness and position persist after leaving water"},
        {"DANCE","dance|dancing|नाचना|नृत्य","establish rhythm, weight shifts, coordinated steps/arms, turn and finish with a settled pose","music source only if established","feet remain supported; spins and cloth follow through","music/footfalls only if story calls for them","partner spacing and rhythm remain consistent"},
        {"PLAY_INSTRUMENT","play piano|play guitar|play violin|play drum|वाद्य बजाना|गिटार बजाना","position hands, contact keys/strings/skins in time, pause and release naturally","instrument, stool/stand if established","finger contact matches instrument; strings/keys do not respond before contact","notes align with visible action; room reverberation fits space","instrument orientation and musical continuity persist"}
    };

    /** Detects explicit activities in script text; does not silently add new story actions. */
    public static List<ActivityPlan> activities(String text) {
        List<ActivityPlan> out=new ArrayList<ActivityPlan>();
        if(text==null || text.trim().isEmpty()) return out;
        String x=text.toLowerCase(java.util.Locale.ROOT);
        for(String[] r:ACTIVITY_RULES) if(containsAny(x,r[1].split("\\|"))) {
            out.add(new ActivityPlan(r[0],r[1],r[2],r[3],r[4],r[5],r[6]));
        }
        return out;
    }

    /** Natural phenomenon cue recognised by the reader and passed to director review. */
    public static final class PhenomenonPlan {
        public final String kind, trigger, visualPhysics, sound, lighting;
        PhenomenonPlan(String kind,String trigger,String visualPhysics,String sound,String lighting) {
            this.kind=kind; this.trigger=trigger; this.visualPhysics=visualPhysics; this.sound=sound; this.lighting=lighting;
        }
        public String toString() {
            return "[Reader/PHENOMENON/"+kind+"] trigger="+trigger+"; visual/physics="+visualPhysics
                    +"; sound="+sound+"; light="+lighting;
        }
    }

    private static final String[][] PHENOMENON_RULES = {
        {"LIGHTNING","lightning|lightning flash|बिजली चमक","brief world-wide flash then fast decay; optional weaker after-flash; no persistent daylight","thunder follows after a distance-dependent delay","brief broad environmental flash; preserve night ambience after strike"},
        {"THUNDER","thunder|गरज","low-frequency event with possible distant/near variation","thunder roll decays over time; no exact visual flash unless specified","no direct illumination unless paired with lightning"},
        {"WIND_WINDOW","wind rattles the window|wind blows the curtains|gust through window|हवा से खिड़की","unlatched window/shade responds around hinge with bounded damped motion; nearby curtain follows airflow","frame/hinge rattle and fabric flutter","shadows move subtly only if light source or foliage actually blocks light"},
        {"FAN","fan rotates|ceiling fan|table fan|पंखा घूम","blades rotate around fixed axis, spin up/down after switch, nearby paper/curtains respond only within plausible airflow","motor hum changes with speed; switch/click at control","fan does not generate its own light"},
        {"STARS","stars twinkle|starry night|तारे टिमटिम","small bounded deterministic brightness variations; preserve positions across frames","night insects/wind only if location supports them","stars remain points of light; no full-frame flashing"},
        {"FLAME_LIGHT","candle|diya|lantern|campfire|fireplace|मिट्टी का दीया|अलाव","flame flickers and smoke rises/drifts with wind; nearby materials may glow","fire crackle only when close enough","localized warm falloff around flame; distant walls/background stay dark"},
        {"VEHICLE","car drives past|vehicle passes|bus moving|truck drives|गाड़ी गुजर","wheels rotate consistently with translation; suspension reacts to bumps; shadows move with vehicle","engine/tyre/road sound moves and fades with distance","headlights/taillights illuminate only in beam/nearby surfaces at night"},
        {"RAIN","rain falls|raining|बारिश","gravity-driven drops, splashes only on suitable surfaces, puddles accumulate only where supported","rain bed and nearby impacts with material-dependent character","wet surfaces may reflect available light, not emit light"},
        {"WATER","waterfall|river flows|waves|समुद्र की लहर|झरना","flow direction consistent; foam/spray drift with wind; shoreline interaction","continuous water ambience and close splash only when justified","specular highlights follow the actual light direction"},
        {"AMBIENCE","crickets|cicadas|birds chirping|night sounds|forest sounds|झींगुर|पक्षियों की आवाज","ambience tied to scene, time, weather and distance; no arbitrary source visible","fade beds at scene boundaries and duck beneath dialogue","sound alone does not change lighting"},
        {"CLOUDS_FOG","clouds drift|overcast sky|fog rolls|mist|बादल छाए|कोहरा","clouds/mist move slowly and coherently; visibility changes by depth without hiding subjects arbitrarily","wind and distant ambience remain subtle","diffuse light under cloud; fog lowers contrast with distance, not a glowing wall"},
        {"SUNRISE_SUNSET","sunrise|sunset|dawn|dusk|सूर्योदय|सूर्यास्त|भोर|सांझ","sun direction and shadow length change gradually; sky colour transitions over time","birds/dawn or evening ambience only if location supports it","warm low-angle light with consistent shadow direction; do not jump from day to night in one frame"},
        {"SNOW_ICE","snowfall|snow falls|snowing|frost|बर्फ़बारी|बर्फ गिर","flakes drift with wind and gravity; accumulation only on exposed surfaces over time","wind-muted ambience and soft foot crunch when contact occurs","snow reflects available light; no self-emission"},
        {"TREE_FOLIAGE","leaves rustle|branches sway|tree bends in wind|पत्ते हिल|पेड़ झूम","gusts travel from branches to twigs and leaves with delayed follow-through; avoid synchronized mechanical motion","leaf rustle rises with gusts and fades after","dappled shadows move only when foliage crosses the light path"},
        {"DUST_DEBRIS","dust in sunlight|dust blows|sandstorm|धूल उड़|आंधी","particles drift with airflow and settle under gravity; density respects distance and visibility","wind and grit sound only when close enough","sunbeams reveal particles only when the light path and atmosphere support them"},
        {"SHADOWS","moving shadow|shadow passes|flickering shadow|परछाईं हिल","shadows follow the actual object, light direction and distance; soft/hard edges match source size","no added sound unless caused by an audible event","never animate a shadow independently of its caster or light"},
        {"CLOCK_TIME","clock ticking|clock chimes|घड़ी की टिक टिक|घंटा बजा","hands advance at a consistent rate; pendulum swings in a stable arc if present","tick/chime only from the established clock and at the shown time","clock does not illuminate the scene unless it has a real light source"},
        {"BOILING_STEAM","water boiling|steam rises|केतली उबल|भाप उठ","bubbles form at the heated liquid surface; steam rises then diffuses with air","boil/kettle hiss fades when heat is removed","steam catches existing light; it is not a light source"},
        {"TRAFFIC","traffic passes|vehicles in traffic|horn honks|ट्रैफिक|हॉर्न बजा","each vehicle follows a lane/path and speed; wheels, spacing, braking and stop-start flow remain plausible","engine/horns vary by distance; avoid constant horns unless scene supports it","headlights affect only nearby surfaces in their beams at night"},
        {"BIRDS_FLIGHT","birds fly|bird flaps|पक्षी उड़|पंख फड़फड़","wingbeats and glide alternate plausibly; flock direction and scale remain coherent","calls match visible/distant birds only when appropriate","birds cast shadows only when scale, distance and light make them visible"},
        {"SMOKE","smoke rises|chimney smoke|धुआँ उठ","smoke rises from source, curls and disperses with heat and wind; preserve source continuity","fire/engine sound only if source supports it","smoke occludes light subtly but does not emit light"},
        {"REFLECTIONS","reflection ripples|reflection in water|पानी में प्रतिबिंब","reflection responds to surface motion and view angle; reflected objects remain geometrically consistent","water ambience only if present","reflections reproduce existing light and colour, not extra light"}
    };

    /** Recognise environmental effects explicitly present in a script beat. */
    public static List<PhenomenonPlan> phenomena(String text) {
        List<PhenomenonPlan> out=new ArrayList<PhenomenonPlan>();
        if(text==null || text.trim().isEmpty()) return out;
        String x=text.toLowerCase(java.util.Locale.ROOT);
        for(String[] r:PHENOMENON_RULES) if(containsAny(x,r[1].split("\\|")))
            out.add(new PhenomenonPlan(r[0],r[1],r[2],r[3],r[4]));
        return out;
    }

    /** Expanded bilingual lexical libraries; use as hints, not as permission to invent story facts. */
    public static final String[] ACTION_VOCAB = {"walk","walking","run","running","sprint","stroll","approach","leave","enter","exit","sit","stand","kneel","crawl","climb","jump","fall","recover","turn","look","glance","stare","blink","nod","shake head","wave","point","reach","grasp","hold","release","carry","lift","drag","push","pull","place","open","close","lock","unlock","knock","ring","press","switch","type","scroll","dial","call","text","read","write","fold","pour","stir","chop","knead","eat","chew","swallow","sip","drink","brush","wash","bathe","dress","undress","comb","shave","sweep","mop","clean","dust","milk","drive","steer","brake","park","pedal","board","dismount","ride","gallop","fly","flap","land","swim","dive","throw","catch","kick","dance","exercise","stretch","smile","frown","laugh","cry","sob","gasp","sigh","tremble","shiver","flinch","hesitate","stumble","balance","breathe","whisper","shout","murmur","चलना","दौड़ना","भागना","बैठना","खड़ा होना","चढ़ना","कूदना","गिरना","संभलना","मुड़ना","देखना","झाँकना","पलक झपकना","सिर हिलाना","हाथ हिलाना","इशारा करना","पकड़ना","छोड़ना","उठाना","खींचना","धकेलना","रखना","खोलना","बंद करना","ताला लगाना","खटखटाना","घंटी बजाना","दबाना","टाइप करना","पढ़ना","लिखना","उँडेलना","हिलाना","काटना","गूँथना","खाना","चबाना","निगलना","पीना","ब्रश करना","धोना","नहाना","कपड़े पहनना","कपड़े बदलना","कंघी करना","दाढ़ी बनाना","झाड़ू लगाना","पोछा लगाना","साफ करना","दूध निकालना","गाड़ी चलाना","साइकिल चलाना","बस में चढ़ना","सवारी करना","उतरना","उड़ना","पंख फड़फड़ाना","तैरना","फेंकना","पकड़ लेना","नाचना","व्यायाम करना","मुस्कुराना","हँसना","रोना","सिसकना","आह भरना","काँपना","ठिठकना","हिचकिचाना","लड़खड़ाना","संतुलन बनाना","साँस लेना","फुसफुसाना","चिल्लाना","chalna","daudna","baithna","uthna","pakadna","chhodna","dekhna","bolna","sunna","hasna","rona","phone karna","message type karna","cycle chalana","chai peena","kapde badalna","seedhi chadhna","darwaza kholna","safai karna","gaadi chalana","cook","cooking","fry","boil","chop vegetables","water plants","gardening","plant a tree","wash clothes","hang clothes","fold clothes","wake up","fall asleep","take a photo","selfie","wash hands","lock","unlock","swim","dive","dance","play guitar","play piano","play violin","play drums","cook खाना पकाना","पौधों में पानी देना","कपड़े धोना","कपड़े सुखाना","सोना","जागना","तस्वीर खींचना","सेल्फी लेना","हाथ धोना","ताला खोलना","ताला लगाना","तैरना","नृत्य करना","गिटार बजाना","पियानो बजाना","बादल","कोहरा","सूर्योदय","सूर्यास्त","बर्फ़बारी","पत्ते हिलना","धूल उड़ना","परछाईं","घड़ी की टिक टिक","भाप उठना","ट्रैफिक","हॉर्न","पक्षी उड़ना","धुआँ उठना","प्रतिबिंब","clouds drifting","fog rolling","sunrise","sunset","snowfall","leaves rustling","dust blowing","moving shadows","clock ticking","steam rising","traffic passing","birds flying","smoke rising","reflections"};
    public static final String[] EMOTION_VOCAB = {"happy","joyful","delighted","excited","curious","confused","worried","anxious","afraid","terrified","angry","irritated","frustrated","sad","lonely","grief","relieved","embarrassed","ashamed","proud","jealous","suspicious","disgusted","surprised","shocked","hopeful","determined","tender","affectionate","playful","mischievous","guilty","patient","impatient","calm","nervous","awkward","hesitant","thoughtful","sleepy","exhausted","pain","disbelief","खुश","प्रसन्न","आनंदित","उत्साहित","जिज्ञासु","उलझन","चिंतित","घबराया","डरा","भयभीत","गुस्सा","क्रोधित","झुँझलाया","निराश","दुखी","उदास","अकेला","शोक","राहत","शर्मिंदा","गर्व","ईर्ष्या","संदेह","घृणा","हैरान","स्तब्ध","आशावान","दृढ़","स्नेह","प्यार","शरारती","अपराधबोध","धैर्य","अधीर","शांत","बेचैन","संकोची","थका हुआ","दर्द","अविश्वास","khush","udaas","gussa","dara hua","hairaan","pareshan","sharminda","thaka hua"};
    public static final String[] CAMERA_VOCAB = {"wide shot","establishing shot","medium shot","close-up","extreme close-up","over-the-shoulder","point of view","tracking shot","dolly","pan","tilt","crane","handheld","rack focus","depth of field","silhouette","screen direction","eyeline","reaction shot","insert","cutaway","वाइड शॉट","मध्यम शॉट","क्लोज़-अप","दृष्टिकोण","कैमरा पैन","कैमरा टिल्ट","फोकस बदलना","सिल्हूट","नज़र की दिशा","प्रतिक्रिया शॉट"};

    /** Reviews story structure and returns concise director-facing decisions, not narration. */
    public static List<Finding> review(Story story) {
        List<Finding> out=new ArrayList<Finding>();
        if (story==null) return out;
        String previousSetting="";
        Set<String> introduced=new HashSet<String>();
        for (Story.CharacterDef c:story.characters) if (c!=null && c.id!=null) introduced.add(c.id);

        for (int si=0;si<story.scenes.size();si++) {
            Story.Scene scene=story.scenes.get(si);
            if (scene==null) continue;
            String setting=clean(scene.setting);
            if (setting.length()==0) {
                if (previousSetting.length()>0) out.add(new Finding("CONTINUITY",
                        "Scene "+scene.number+" has no explicit setting",
                        "inherit the previous scene's location and lighting only as a provisional default; preserve any time jump implied by the script",
                        "MEDIUM"));
                else out.add(new Finding("LAYOUT",
                        "Scene "+scene.number+" has no explicit setting",
                        "establish a neutral, story-compatible wide shot; do not assert a specific new location without evidence",
                        "LOW"));
            } else previousSetting=setting;

            int dialogue=0, directions=0;
            Set<String> speakers=new HashSet<String>();
            boolean hasActionCue=false, hasEmotionalCue=false;
            for (Story.Beat beat:scene.beats) {
                if (beat==null || beat.text==null) continue;
                String text=beat.text.trim();
                if (beat.type==Story.Beat.DIALOGUE) {
                    dialogue++;
                    if (beat.speaker!=null && beat.speaker.id!=null) speakers.add(beat.speaker.id);
                    if (containsAny(text,"?","क्यों","कैसे","क्या","where","why","how","what","who")) hasEmotionalCue=true;
                } else {
                    directions++;
                    if (containsAny(text,"चल","दौड़","उठ","बैठ","मुड़","देख","खोल","रख","उठा","गिर","प्रवेश","बाहर","walk","run","turn","look","open","put","enter","leave")) hasActionCue=true;
                    if (containsAny(text,"डर","गुस्सा","रो","हँस","हंस","मुस्कुरा","चौंक","सोच","झिझक","afraid","angry","cry","laugh","smile","surprise","think","hesitat")) hasEmotionalCue=true;
                }
            }

            if (dialogue>=2 && directions==0) out.add(new Finding("PERFORMANCE",
                    "Scene "+scene.number+" is dialogue-only on the page",
                    "direct listener reactions, eye-line changes, breathing and pauses; use restrained inserts only when they clarify subtext, never add plot facts",
                    "MEDIUM"));
            if (dialogue>0 && !hasActionCue) out.add(new Finding("BLOCKING",
                    "Scene "+scene.number+" contains dialogue but little explicit physical action",
                    "choose motivated idle business from the setting (gaze, posture, hand movement or a prop already established); keep hands free of invented story-critical objects",
                    "MEDIUM"));
            if (dialogue>0 && !hasEmotionalCue) out.add(new Finding("SUBTEXT",
                    "Scene "+scene.number+" gives limited explicit emotional direction",
                    "infer a subtle playable intention from the words and prior beat, then show it through eyes, timing and posture; keep the inference reversible and avoid exaggerated emotion",
                    "LOW"));
            if (scene.beats.size()>0 && speakers.size()>1) out.add(new Finding("COVERAGE",
                    "Scene "+scene.number+" has "+speakers.size()+" speaking characters",
                    "plan speaker/listener coverage and a reaction shot at meaningful turns; preserve screen direction and eyelines across cuts",
                    "HIGH"));
            if (scene.beats.size()>5 && directions==0) out.add(new Finding("PACING",
                    "Scene "+scene.number+" has a long run of dialogue beats without written action",
                    "group lines by dramatic turn, add short reaction holds and vary shot size only where the emotional beat changes; do not mechanically cut on every line",
                    "MEDIUM"));
        }
        if (story.scenes.isEmpty()) out.add(new Finding("SCRIPT",
                "No scene blocks were parsed",
                "stop automatic film generation and show a clear script-structure warning instead of fabricating a complete movie",
                "HIGH"));
        return out;
    }

    /** Short notes for the existing film review panel. */
    public static List<String> notes(Story story) {
        List<String> notes=new ArrayList<String>();
        for (Finding f:review(story)) notes.add(f.toString());
        return notes;
    }

    private static String clean(String s) { return s==null ? "" : s.trim(); }
    private static boolean containsAny(String s,String... terms) {
        String x=s.toLowerCase(java.util.Locale.ROOT);
        for (String term:terms) if (x.contains(term.toLowerCase(java.util.Locale.ROOT))) return true;
        return false;
    }
}
