package com.tarun.kahani.jvm;

import com.tarun.kahani.core.*;

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import javax.imageio.ImageIO;

/**
 * Desktop end-to-end test: script -> director -> voices (espeak-ng) -> mix -> frames -> MP4 (ffmpeg).
 * Usage: MakeFilm <script.txt> <assetDir|-> <out.mp4> <width> <fps> [stillsDir] [maxSeconds]
 */
public class MakeFilm {
    public static void main(String[] a) throws Exception {
        String script = new String(Files.readAllBytes(Paths.get(a[0])), "UTF-8");
        final String assets = a[1];
        String out = a[2];
        int width = Integer.parseInt(a[3]);
        String aspect = System.getenv().getOrDefault("ASPECT", "16:9");
        int height = Math.round(width / com.tarun.kahani.core.PixarLead.spec(aspect).ratio) & ~1;   // every delivery format's exact shape
        int fps = Integer.parseInt(a[4]);
        String stills = a.length > 5 ? a[5] : null;
        float maxSec = a.length > 6 ? Float.parseFloat(a[6]) : 1e9f;

        long t0 = System.currentTimeMillis();
        Story story = ScriptParser.parse(script);
        // the sizes the pictures are read at (SPRITE_SIDE / BACKDROP_SIDE to compare sizes)
        if (System.getenv("SPRITE_SIDE") == null && !assets.equals("-")) {
            // v35: the sizes the phone reads the pictures at for this output (Art.sizesFor): a phone with a 512 MB heap
            // and FREE_MB of free memory (2048 unless given)
            int[] counts = Art.pictureCounts(new String(Files.readAllBytes(Paths.get(assets, "cast.txt")), "UTF-8"));
            long free = Long.parseLong(System.getenv().getOrDefault("FREE_MB", "2048")) << 20;
            int[] sides = Art.sizesFor(height, 512L << 20, free, counts[0], counts[1]);
            Art.spriteSide = sides[0]; Art.backdropSide = sides[1];
            System.out.println("pictures read at: characters " + sides[0] + " px, places " + sides[1] + " px");
        }
        if (System.getenv("SPRITE_SIDE") != null) Art.spriteSide = Integer.parseInt(System.getenv("SPRITE_SIDE"));
        // v47.13: MESH=<screen px per cell>:<cells per picture pixel>:<most rows> — the mesh's fineness (Rig.SOURCE_CELLS)
        String mesh = System.getenv().getOrDefault("MESH", "");
        // (the face's own mesh: a third of a pixel a cell, up to three cells a picture pixel — FACE_MESH="" for the old one)
        String fm = System.getenv().getOrDefault("FACE_MESH", "0.33:3:1536");
        if (fm.length() > 0) {
            String[] m = fm.split(":");
            Rig.FACE_CELL_PX = Float.parseFloat(m[0]); Rig.FACE_SOURCE_CELLS = Float.parseFloat(m[1]); Rig.MAX_FACE_CELLS = Integer.parseInt(m[2]);
        }
        if (mesh.length() > 0) {
            String[] m = mesh.split(":");
            Rig.CELL_PX = Float.parseFloat(m[0]); Rig.SOURCE_CELLS = Float.parseFloat(m[1]);
            Rig.MAX_CELLS = Integer.parseInt(m[2]); Rig.MAX_FACE_CELLS = Math.max(Rig.MAX_FACE_CELLS, Rig.MAX_CELLS / 2);
            System.out.println("mesh: " + Rig.CELL_PX + " screen px a cell, up to " + Rig.SOURCE_CELLS + " cells a picture pixel, " + Rig.MAX_CELLS + " rows");
        }
        if (System.getenv("BACKDROP_SIDE") != null) Art.backdropSide = Integer.parseInt(System.getenv("BACKDROP_SIDE"));
        Art art = new Art();
        if (System.getenv("REFART") != null) {
            // v47.8: a bundled film's own reference pack (film_library/<id>/manifest.json: the user's pictures), cast as the
            // desktop render tests cast it — needs the test classes (ReferenceArtLoader) on the class path
            Class<?> ra = Class.forName("com.tarun.kahani.ReferenceArtLoader");
            java.lang.reflect.Method load = ra.getDeclaredMethod("load", String.class, Story.class, File.class);
            load.setAccessible(true);
            art = (Art) load.invoke(null, System.getenv("REFART"), story, new File(System.getenv().getOrDefault("REFART_ASSETS", "app/src/main/assets")));
        } else if (!assets.equals("-")) {
            Art.Loader L = new AwtLoader(assets);
            art = Art.fromManifest(new String(Files.readAllBytes(Paths.get(assets, "cast.txt")), "UTF-8"), story, L);
        }
        // v47.8: BOARD=<dir>: the story board's pictures in that folder, in name order (the first the title, the last the end)
        if (System.getenv("BOARD") != null) {
            File bd = new File(System.getenv("BOARD"));
            String[] names = bd.list();
            java.util.List<String> files = new java.util.ArrayList<String>();
            if (names != null) { java.util.Arrays.sort(names); for (String n : names) if (n.toLowerCase(java.util.Locale.ROOT).matches(".*\\.(png|jpe?g|webp)")) files.add(n); }
            art.readBoard(new AwtLoader(bd.getAbsolutePath()), files, story);
        }
        System.out.println("art: sprites=" + art.sprites.size() + " scenes=" + art.scenes.size() + " shots=" + art.shots.size()
                + " title=" + (art.title != null) + " end=" + (art.end != null) + "  (" + (System.currentTimeMillis() - t0) + "ms)");
        for (String n : art.notes) System.out.println("  picture: " + n);

        Director.Options opt = new Director.Options();
        opt.aspect = aspect;
        // the app's switches: SUBS=1 subtitles, TWOS=1 animation on twos, PACE=0.8..1.3 the speed
        opt.subtitles = System.getenv("SUBS") != null;
        opt.onTwos = System.getenv("TWOS") != null;
        opt.pace = Float.parseFloat(System.getenv().getOrDefault("PACE", "1"));
        opt.outHeight = height;      // v35: the close-up's sharpness limit at the film's own size (as on the phone)
        Director dir = new Director(story, opt);
        Film film = dir.prepare();
        float[][] voices = new float[film.lines.size()][];
        final File tmp = Files.createTempDirectory("voices").toFile();
        // v42: the voices' scratch folder goes when the tool ends (hundreds of runs had left gigabytes behind)
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                File[] fs = tmp.listFiles();
                if (fs != null) for (File f : fs) f.delete();
                tmp.delete();
            }
        });
        // SAMPLE="वानुषा=/path/sample.wav": that character's lines are moved to the sample's voice (as on the phone)
        String sampleWho = null;
        VoiceFx.Profile sampleProfile = null;
        if (System.getenv("SAMPLE") != null) {
            String[] sv = System.getenv("SAMPLE").split("=", 2);
            sampleWho = sv[0];
            sampleProfile = VoiceFx.profile(readWav(new File(sv[1])), Synth.SR);
        }
        // v47.13: KOKORO=<model dir>:<espeak-ng-data dir>[:<python>] — the lines spoken by Kokoro-82M (Apache-2.0)
        float[][] natural = System.getenv("KOKORO") != null && System.getenv("KOKORO").contains(":") ? kokoro(film, tmp) : null;
        int byKokoro = 0;
        for (int i = 0; i < film.lines.size(); i++) {
            Film.Line l = film.lines.get(i);
            voices[i] = natural != null && natural[i] != null ? natural[i] : espeak(l, tmp, i);
            if (natural != null && natural[i] != null) byKokoro++;
            if (voices[i] != null && sampleProfile != null && l.who != null && l.who.displayName.equals(sampleWho))
                voices[i] = VoiceFx.matchVoice(voices[i], sampleProfile, Synth.SR);
            // raspy / trembling / booming… from the description, shouting / crying… from the line (as on the phone)
            if (voices[i] != null) {
                VoiceStyle vs = VoiceStyle.forLine(l.who == null ? null : VoiceStyle.forCharacter(l.who), l.manner, l.whisper, false);
                if (vs.any()) voices[i] = vs.apply(voices[i], Synth.SR, false);
            }
            if (voices[i] != null) {
                l.dur = voices[i].length / (float) Synth.SR;
                l.env = Mixer.envelope(voices[i], Synth.SR);
                l.shape = Mixer.shape(voices[i], Synth.SR);
            }
        }
        // v47.9: the films had no voices when espeak-ng was missing, and nothing said so — the count is in the log now
        int spoken = 0;
        for (float[] v : voices) if (v != null) spoken++;
        System.out.println("VOICES: " + spoken + " of " + film.lines.size() + " lines spoken (" + (byKokoro > 0 ? byKokoro + " by Kokoro-82M, " + (spoken - byKokoro) + " by espeak-ng" : "espeak-ng") + ")" + (spoken < film.lines.size() ? " — install espeak-ng for the rest" : ""));
        film = dir.direct(art);
        // WIDE=1: every scene seen whole by one still camera — for looking at the staging itself (who stands where,
        // vehicles, doors), not at the film's shots
        if (System.getenv("WIDE") != null) for (Film.Seg sg : film.segs) if (sg.type == Film.S_SCENE) {
            sg.cams.clear();
            Film.Cam wc = new Film.Cam(sg.t0, 640, 360, 1f, 0);
            wc.still = true;
            sg.cams.add(wc);
        }
        // CAMS=a,b: the camera moves between a and b seconds (time, centre, zoom, ease)
        if (System.getenv("CAMS") != null) {
            String[] r = System.getenv("CAMS").split(",");
            float c0 = Float.parseFloat(r[0]), c1 = Float.parseFloat(r[1]);
            for (Film.Seg sg : film.segs) for (Film.Cam c : sg.cams) if (c.t >= c0 && c.t <= c1)
                System.out.printf(java.util.Locale.US, "CAM %.2f x=%.0f y=%.0f zoom=%.2f ease=%.2f%n", c.t, c.cx, c.cy, c.zoom, c.ease);
            for (Film.Shot sh : film.shots) if (sh.t >= c0 && sh.t <= c1) System.out.printf(java.util.Locale.US, "SHOT %.2f size=%d %s | %s%n", sh.t, sh.size, sh.subject, sh.purpose);
        }
        // KEYS=name: that character's keys as made
        if (System.getenv("KEYS") != null) for (Film.Seg sg : film.segs) for (Film.Actor ac : sg.actors) if (ac.c.displayName.equals(System.getenv("KEYS")))
            for (Film.Key k : ac.keys) System.out.printf(java.util.Locale.US, "KEY %.2f x=%.0f body=%d seat=%d move=%.2f face=%.0f emo=%d%n", k.t, k.x, k.body, k.seat, k.moveDur, k.facing, k.emotion);
        // ACTS=1: every playground act of the film as made (the swing and the slide, v41)
        if (System.getenv("ACTS") != null) for (Film.Seg sg : film.segs) for (Film.Actor ac : sg.actors) for (Film.Act x : ac.acts)
            if (x.type == Film.G_PLAYGROUND || "all".equals(System.getenv("ACTS"))) System.out.printf(java.util.Locale.US, "ACT %s type=%d item=%d %.2f-%.2f how=%d a0=%.0f a1=%.0f%n", ac.c.displayName, x.type, x.item, x.t0, x.t1, x.how, x.a0, x.a1);
        // FXS=1: every effect of the film (type, time, place)
        if (System.getenv("FXS") != null) for (Film.Seg sg : film.segs) for (Film.Fx f : sg.fx)
            System.out.printf(java.util.Locale.US, "FX type=%d %.2f-%.2f x=%.0f y=%.0f t2=%.2f color=%d%n", f.type, f.t0, f.t1, f.x, f.y, f.t2, f.color);
        // keys made out of time order (a later key placed before an earlier one) — none expected
        for (Film.Seg sg : film.segs) for (Film.Actor ac : sg.actors) for (int i = 1; i < ac.keys.size(); i++)
            if (ac.keys.get(i).t < ac.keys.get(i - 1).t - 1e-3f) System.out.printf(java.util.Locale.US, "KEY ORDER %s %.2f after %.2f%n", ac.c.displayName, ac.keys.get(i).t, ac.keys.get(i - 1).t);
        System.out.println("film: " + film.duration + "s, segs=" + film.segs.size() + " lines=" + film.lines.size() + " sfx=" + film.sfx.size());
        for (String n : film.notes) System.out.println("  " + n);
        if (System.getenv("SHOTS") != null) Files.write(Paths.get(System.getenv("SHOTS")), film.shotList.getBytes("UTF-8"));
        // PRODDOC=file.json: the production document of this film (as the phone keeps it), written now with the plan and
        // again with the render's verification when the film is made
        final com.tarun.kahani.core.ProductionDocument pdoc = System.getenv("PRODDOC") == null ? null : com.tarun.kahani.core.ProductionDocument.read(story, null);
        if (pdoc != null) {
            pdoc.bind(story); pdoc.attach(film); pdoc.assets(art); pdoc.validate();
            pdoc.review(com.tarun.kahani.core.ProductionDocument.DIRECTOR, "APPROVED", "the plan follows the script");
            Files.write(Paths.get(System.getenv("PRODDOC")), pdoc.toJson().getBytes("UTF-8"));
            System.out.println("production check: " + pdoc.report(60));
        }
        // DELIVERY=prefix: the edit list with scene markers (v38, as the app saves it)
        if (System.getenv("DELIVERY") != null) {
            Files.write(Paths.get(System.getenv("DELIVERY") + "_edit_list.txt"), com.tarun.kahani.core.FilmCraft.editList(film, fps, film.story != null ? film.story.title : "").getBytes("UTF-8"));
        }
        // KEYS=1: every character's keys (time, body, seat, eyes shut, visible) — to check staging against the frames
        if (System.getenv("KEYS") != null) for (Film.Seg sg : film.segs) for (Film.Actor ac : sg.actors) {
            StringBuilder kb = new StringBuilder();
            for (Film.Key k : ac.keys) kb.append(String.format(java.util.Locale.US, " [%.2f b%d s%d a%d%s%s]", k.t, k.body, k.seat, k.anchor, k.eyesShut ? " shut" : "", k.visible ? "" : " hidden"));
            for (Film.Act at : ac.acts) kb.append(String.format(java.util.Locale.US, " {act g%d %.2f-%.2f i%d}", at.type, at.t0, at.t1, at.item));
            System.out.printf("  keys seg %.1f-%.1f ground=%.0f %s:%s%n", sg.t0, sg.t1, sg.ground, ac.c.displayName, kb);
        }
        if (System.getenv("LINES") != null) {
            for (Film.Weather w : film.weather) System.out.printf("  weather %d %.1f-%.1f x%.2f%n", w.type, w.t0, w.t1, w.strength);
            for (Film.Seg sg : film.segs) for (Film.Fx f : sg.fx) if (f.type >= Film.FX_LIGHTNING) System.out.printf("  fx %d @%.1f-%.1f (%.0f,%.0f)%n", f.type, f.t0, f.t1, f.x, f.y);
        }
        if (System.getenv("LINES") != null) for (Film.Line l : film.lines)
            System.out.printf("  line %2d @%6.1f +%4.1f %s: %s%n", l.index, l.start, l.dur, l.who == null ? "-" : l.who.displayName, l.text.substring(0, Math.min(30, l.text.length())));
        SoundLib lib = new SoundLib(new SoundLib.Decoder() {
            public float[] decode(String path) {
                try {
                    Process p = new ProcessBuilder("ffmpeg", "-loglevel", "error", "-i", path, "-f", "f32le", "-ac", "1", "-ar", String.valueOf(Synth.SR), "-").start();
                    byte[] b = p.getInputStream().readAllBytes();
                    p.waitFor();
                    java.nio.FloatBuffer fb = java.nio.ByteBuffer.wrap(b).order(java.nio.ByteOrder.LITTLE_ENDIAN).asFloatBuffer();
                    float[] o = new float[fb.remaining()];
                    fb.get(o);
                    return o;
                } catch (Exception e) { return null; }
            }
        });
        File sounds = new File(new File(a[0]).getAbsoluteFile().getParentFile(), "sounds");
        if (!new File(sounds, "index.json").exists()) sounds = new File("app/src/main/assets/sounds");
        if (new File(sounds, "index.json").exists())
            lib.addIndex(new String(Files.readAllBytes(new File(sounds, "index.json").toPath()), "UTF-8"), sounds.getAbsolutePath() + "/");
        Edits edits = Edits.fromJson(System.getenv().getOrDefault("EDITS", "{}"));
        film.subtitles = edits.subtitles;
        short[] pcm = Mixer.mix(film, voices, lib, edits, null);
        File wav = new File(tmp, "mix.wav");
        writeWav(wav, pcm, Synth.SR, Mixer.CHANNELS);
        System.out.println("audio mixed (" + (System.currentTimeMillis() - t0) + "ms)");
        if (System.getenv("AUDIO_ONLY") != null) { Files.copy(wav.toPath(), new File(out).toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING); return; }

        float dur = Math.min(film.duration, maxSec);
        Process ff = new ProcessBuilder("ffmpeg", "-y", "-loglevel", "error", "-f", "rawvideo", "-pix_fmt", "bgr24", "-s", width + "x" + height,
                "-r", String.valueOf(fps), "-i", "-", "-i", wav.getAbsolutePath(), "-t", String.valueOf(dur),
                "-c:v", "libx264", "-preset", "veryfast", "-pix_fmt", "yuv420p", "-crf", "24", "-c:a", "aac", "-b:a", "128k", out)
                .redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.INHERIT).start();
        OutputStream os = new BufferedOutputStream(ff.getOutputStream(), 1 << 20);
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        AwtGfx g = new AwtGfx(img);
        Renderer r = new Renderer(film, art);
        r.safeZoneOverlay = System.getenv("SAFEZONE") != null;
        // v47.7: every character in every frame as drawn, read back after the film (AUDIT=0 skips it)
        final com.tarun.kahani.core.FrameAudit audit = "0".equals(System.getenv("AUDIT")) ? null : new com.tarun.kahani.core.FrameAudit(fps);
        r.audit = audit;
        if (stills != null) { lockSheets(dir, film, art, new File(stills), width, height); stillPages(dir, film, art, new File(stills)); }
        // pipeline step 6 (FinalQc): every shot played frame by frame at check size before the film; QC=0 skips it
        if (!"0".equals(System.getenv().getOrDefault("QC", "1")) && System.getenv("ONLY") == null) {
            final int qw = 256, qh = Math.max(96, Math.round(256f * height / width)) & ~1;
            final BufferedImage qimg = new BufferedImage(qw, qh, BufferedImage.TYPE_INT_ARGB);
            final AwtGfx qg = new AwtGfx(qimg);
            final int[] qpx = new int[qw * qh];
            long tq = System.currentTimeMillis();
            com.tarun.kahani.core.FinalQc.Result qr = com.tarun.kahani.core.FinalQc.check(film, art, new com.tarun.kahani.core.FinalQc.Surface() {
                public com.tarun.kahani.core.Gfx gfx() { return qg; }
                public int[] pixels() { qimg.getRGB(0, 0, qw, qh, qpx, 0, qw); return qpx; }
                public int width() { return qw; }
                public int height() { return qh; }
            }, null);
            film.shotList += "\n" + qr.text();
            System.out.print(qr.text());
            if (film.stats != null) {
                com.tarun.kahani.core.Handbook.Card card = com.tarun.kahani.core.Handbook.score(film, film.stats, qr.boilingLeft, qr.shakingLeft, qr.floating);   // what is left after the fix
                film.shotList += "\n" + card.text() + "\n" + com.tarun.kahani.core.Handbook.delivery(card, film.stats);
                System.out.print(card.text());
            }
            System.out.println("  (final check in " + (System.currentTimeMillis() - tq) + "ms)");
        }
        int frames = (int) (dur * fps);
        long tr = System.currentTimeMillis();
        int stillEvery = Math.max(1, (int) (fps * Float.parseFloat(System.getenv().getOrDefault("STILL_EVERY", "3"))));
        float only = Float.parseFloat(System.getenv().getOrDefault("ONLY", "-1"));
        // the cinematic finish (filmic curve, bloom, colour script), exactly as on the phone
        final com.tarun.kahani.core.FilmLook look = System.getenv("NO_LOOK") != null ? null : new com.tarun.kahani.core.FilmLook(width, height);
        final com.tarun.kahani.core.FilmLook.Params lp = new com.tarun.kahani.core.FilmLook.Params();
        final int[] lookPx = new int[width * height];
        // pipeline step 8 (FinalQc.Meter): the finished film metered frame by frame as it is written
        final com.tarun.kahani.core.FinalQc.Meter meter = look == null ? null : new com.tarun.kahani.core.FinalQc.Meter(film, width, height, fps);
        if (only >= 0) {
            // just write single frames at the given comma separated times
            for (String ts : System.getenv("TIMES").split(",")) {
                float t = Float.parseFloat(ts);
                r.render(g, t);
                if (look != null) finish(img, look, com.tarun.kahani.core.FilmLook.at(film, t, lp), lookPx);
                boolean png = System.getenv("PNG") != null;
                ImageIO.write(img, png ? "png" : "jpg", new File(stills, String.format(png ? "t_%08.3f.png" : "t_%08.3f.jpg", t)));
            }
            System.out.println("frames written");
            System.out.println("MODELS: " + com.tarun.kahani.core.PoseModel.status() + "; " + com.tarun.kahani.core.FaceMesh.status());
            System.out.println("STATS " + com.tarun.kahani.core.Renderer.stats());
            return;
        }
        // FROM / TO (seconds): only a stretch of the film, for looking at one moment's motion
        int f0 = Math.max(0, (int) (Float.parseFloat(System.getenv().getOrDefault("FROM", "0")) * fps));
        int f1 = Math.min(frames, (int) (Float.parseFloat(System.getenv().getOrDefault("TO", "1e6")) * fps));
        for (int f = f0; f < f1; f++) {
            float t = f / (float) fps;
            r.render(g, t);
            if (look != null) finish(img, look, com.tarun.kahani.core.FilmLook.at(film, t, lp), lookPx);
            if (meter != null) meter.frame(f, lookPx);
            byte[] px = ((java.awt.image.DataBufferByte) img.getRaster().getDataBuffer()).getData();
            os.write(px);
            if (stills != null && f % stillEvery == 0) ImageIO.write(img, "jpg", new File(stills, String.format("f%05d_%.1fs.jpg", f, t)));
        }
        os.close();
        ff.waitFor();
        long ms = System.currentTimeMillis() - tr;
        System.out.println("rendered " + frames + " frames in " + ms + "ms (" + (frames * 1000f / Math.max(1, ms)) + " fps) -> " + out);
        com.tarun.kahani.core.FrameAudit.Result audited = audit != null ? audit.read(film) : null;
        if (pdoc != null) {
            pdoc.frameAudit(audited);
            com.tarun.kahani.core.ProductionDocument.RenderRecord rr = pdoc.verifyRender(new File(out), "desktop (ffmpeg)", dur);
            Files.write(Paths.get(System.getenv("PRODDOC")), pdoc.toJson().getBytes("UTF-8"));
            System.out.println("render " + rr.status + ": " + rr.detail);
        }
        System.out.println("STATS " + com.tarun.kahani.core.Renderer.stats());
        if (meter != null) { film.shotList += meter.report(); System.out.print(meter.report()); }
        if (audited != null) { String at = audited.text(); film.shotList += "\n" + at; System.out.print(at); }
        if (System.getenv("SHOTS") != null) Files.write(Paths.get(System.getenv("SHOTS")), film.shotList.getBytes("UTF-8"));
    }

    /** Pipeline steps 1-2: the Character Lock Sheet of every character (480x640) and the Location Lock Plate of every place, next to the stills. */
    static void lockSheets(Director dir, Film film, Art art, File stills, int width, int height) throws Exception {
        stills.mkdirs();
        int n = 0, m = 0;
        for (Story.CharacterDef c : film.story.cast()) {
            int[] sz = dir.lockSheetSize(c);
            writeSeg(film, art, dir.lockSheet(c, sz[0] / (float) sz[1]), sz[0], sz[1], new File(stills, "lock_char_" + n++ + ".jpg"));
        }
        int ph = Math.max(120, Math.round(640f * height / width)) & ~1;
        for (Object[] p : dir.locationPlates()) writeSeg(film, art, (Film.Seg) p[1], 640, ph, new File(stills, "lock_place_" + m++ + ".jpg"));
        System.out.println("lock sheets: " + n + " characters, " + m + " places -> " + stills);
        System.out.println("MODELS: " + com.tarun.kahani.core.PoseModel.status() + "; " + com.tarun.kahani.core.FaceMesh.status());
    }

    /** One part on its own (a lock sheet, a plate) as a JPEG with the film's look. */
    static void writeSeg(Film film, Art art, Film.Seg s, int w, int h, File out) throws Exception {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
        AwtGfx g = new AwtGfx(img);
        new Renderer(film, art).renderSeg(g, s, 0.5f);
        finish(img, new com.tarun.kahani.core.FilmLook(w, h), com.tarun.kahani.core.FilmLook.forSeg(s, new com.tarun.kahani.core.FilmLook.Params()), new int[w * h]);
        ImageIO.write(img, "jpg", out);
    }

    /** The thumbnail (1280x720) and the poster (1080x1920), made natively (RULE_RESIZE_8), next to the stills. */
    static void stillPages(Director dir, Film film, Art art, File stills) throws Exception {
        Film.Seg[] pages = dir.stills();
        String[] names = {"thumbnail.jpg", "poster.jpg"};
        int[][] sizes = {{1280, 720}, {1080, 1920}};
        for (int i = 0; i < pages.length; i++) {
            BufferedImage img = new BufferedImage(sizes[i][0], sizes[i][1], BufferedImage.TYPE_3BYTE_BGR);
            AwtGfx g = new AwtGfx(img);
            Renderer r = new Renderer(film, art);
            r.safeZoneOverlay = System.getenv("SAFEZONE") != null;
            r.renderSeg(g, pages[i], 0.5f);
            com.tarun.kahani.core.FilmLook look = new com.tarun.kahani.core.FilmLook(sizes[i][0], sizes[i][1]);
            finish(img, look, com.tarun.kahani.core.FilmLook.forSeg(pages[i], new com.tarun.kahani.core.FilmLook.Params()), new int[sizes[i][0] * sizes[i][1]]);
            ImageIO.write(img, "jpg", new File(stills, names[i]));
        }
    }

    /** The film look on a BGR frame (converted to ARGB and back). */
    static void finish(BufferedImage img, com.tarun.kahani.core.FilmLook look, com.tarun.kahani.core.FilmLook.Params p, int[] px) {
        byte[] b = ((java.awt.image.DataBufferByte) img.getRaster().getDataBuffer()).getData();
        int n = px.length;
        for (int i = 0, o = 0; i < n; i++, o += 3) px[i] = 0xFF000000 | ((b[o + 2] & 255) << 16) | ((b[o + 1] & 255) << 8) | (b[o] & 255);
        look.apply(px, p);
        for (int i = 0, o = 0; i < n; i++, o += 3) { int c = px[i]; b[o] = (byte) c; b[o + 1] = (byte) (c >> 8); b[o + 2] = (byte) (c >> 16); }
    }

    /**
     * v47.13: every line spoken by Kokoro-82M (Apache-2.0, through sherpa-onnx: tools/tts/kokoro_batch.py), in one run:
     * a voice for each character by who they are — its Hindi voices hf_alpha / hf_beta (women, girls) and hm_omega /
     * hm_psi (men, boys), the British ones for English lines — raised for a child, lowered and slower for the old, a
     * second woman or man of the story in the other voice. A line it cannot speak is left to espeak-ng.
     */
    static float[][] kokoro(Film film, File dir) {
        float[][] out = new float[film.lines.size()][];
        try {
            String[] k = System.getenv("KOKORO").split(":");
            String py = k.length > 2 ? k[2] : "python3";
            File repo = new File(System.getProperty("kahani.assets", "app/src/main/assets")).getAbsoluteFile().getParentFile().getParentFile().getParentFile().getParentFile();
            File script = new File(repo, "tools/tts/kokoro_batch.py");
            KokoroCast cast = new KokoroCast();
            StringBuilder j = new StringBuilder("[");
            for (int i = 0; i < film.lines.size(); i++) {
                Film.Line l = film.lines.get(i);
                File w = new File(dir, "k" + i + ".wav");
                if (j.length() > 1) j.append(',');
                // (the feeling's pace here, its pitch with the character's below: v47.13 voice modulation)
                j.append("{\"text\":").append(jsonString(l.text)).append(",\"sid\":").append(cast.speaker(l))
                        .append(",\"speed\":").append(cast.pace(l)).append(",\"pitch\":1")
                        .append(",\"lang\":\"").append(l.hindi ? "hi" : "en-us").append("\",\"out\":").append(jsonString(w.getAbsolutePath())).append('}');
            }
            j.append(']');
            File jobs = new File(dir, "kokoro_jobs.json");
            Files.write(jobs.toPath(), j.toString().getBytes("UTF-8"));
            Process p = new ProcessBuilder(py, "-I", script.getAbsolutePath(), k[0], k[1], jobs.getAbsolutePath()).redirectErrorStream(true).start();
            String said = new String(p.getInputStream().readAllBytes(), "UTF-8");
            p.waitFor();
            if (!said.contains("spoke")) System.out.println("KOKORO: nothing spoken — " + said.substring(Math.max(0, said.length() - 400)).replace('\n', ' '));
            for (int i = 0; i < out.length; i++) {
                File w = new File(dir, "k" + i + ".wav");
                if (w.isFile() && w.length() > 1000) out[i] = cast.shape(film.lines.get(i), readWav(w), Synth.SR);
            }
        } catch (Exception e) {
            System.out.println("KOKORO: " + e);
        }
        return out;
    }

    static String jsonString(String s) {
        StringBuilder b = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            if (c == '"' || c == '\\') b.append('\\').append(c);
            else if (c < 0x20) b.append(String.format("\\u%04x", (int) c));
            else b.append(c);
        }
        return b.append('"').toString();
    }

    public static float[] espeak(Film.Line l, File dir, int i) {
        try {
            File w = new File(dir, "v" + i + ".wav");
            int pitch = 50, speed = 150;
            String voice = "hi";
            if (l.who != null) {
                Look k = l.who.look;
                if (k.kind == Look.GIRL) { pitch = k.height < 0.7f ? 90 : 78; speed = 160; voice = "hi+f3"; }
                else if (k.kind == Look.WOMAN) { pitch = 62; voice = "hi+f2"; }
                else if (k.kind == Look.WITCH) { pitch = 72; speed = 135; voice = "hi+f4"; }
                else if (k.kind == Look.MONSTER) { pitch = 5; speed = 125; voice = "hi+m3"; }
                else if (k.kind == Look.MONKEY) { pitch = 95; speed = 180; }
                else { pitch = 35; voice = "hi+m1"; }
            }
            File txt = new File(dir, "v" + i + ".txt");
            Files.write(txt.toPath(), l.text.getBytes("UTF-8"));
            Process p = new ProcessBuilder("espeak-ng", "-v", voice, "-p", String.valueOf(pitch), "-s", String.valueOf(speed), "-w", w.getAbsolutePath(), "-f", txt.getAbsolutePath())
                    .redirectErrorStream(true).start();
            p.getInputStream().readAllBytes();
            p.waitFor();
            return readWav(w);
        } catch (Exception e) {
            return null;
        }
    }

    public static float[] readWav(File f) throws IOException {
        byte[] b = Files.readAllBytes(f.toPath());
        int sr = (b[24] & 255) | (b[25] & 255) << 8 | (b[26] & 255) << 16;
        int pos = 12, dataPos = -1, dataLen = 0;
        while (pos + 8 <= b.length) {
            String id = new String(b, pos, 4, "US-ASCII");
            int len = (b[pos + 4] & 255) | (b[pos + 5] & 255) << 8 | (b[pos + 6] & 255) << 16 | (b[pos + 7] & 255) << 24;
            if (id.equals("data")) { dataPos = pos + 8; dataLen = Math.min(len < 0 ? b.length : len, b.length - dataPos); break; }
            pos += 8 + len;
        }
        if (dataPos < 0) return null;
        int n = dataLen / 2;
        float[] o = new float[n];
        for (int i = 0; i < n; i++) o[i] = (short) ((b[dataPos + 2 * i] & 255) | (b[dataPos + 2 * i + 1] << 8)) / 32768f;
        return Mixer.resample(o, sr);
    }

    static void writeWav(File f, short[] pcm, int sr) throws IOException { writeWav(f, pcm, sr, 1); }

    static void writeWav(File f, short[] pcm, int sr, int ch) throws IOException {
        DataOutputStream o = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(f)));
        int len = pcm.length * 2;
        o.writeBytes("RIFF"); o.writeInt(Integer.reverseBytes(36 + len)); o.writeBytes("WAVEfmt ");
        o.writeInt(Integer.reverseBytes(16)); o.writeShort(Short.reverseBytes((short) 1)); o.writeShort(Short.reverseBytes((short) ch));
        o.writeInt(Integer.reverseBytes(sr)); o.writeInt(Integer.reverseBytes(sr * 2 * ch)); o.writeShort(Short.reverseBytes((short) (2 * ch)));
        o.writeShort(Short.reverseBytes((short) 16)); o.writeBytes("data"); o.writeInt(Integer.reverseBytes(len));
        for (short s : pcm) o.writeShort(Short.reverseBytes(s));
        o.close();
    }

    /** Loads pictures from a folder with ImageIO. */
    public static class AwtLoader implements Art.Loader {
        final String dir;
        public AwtLoader(String dir) { this.dir = dir; }
        BufferedImage read(String name, int maxSide) {
            try {
                BufferedImage im = ImageIO.read(new File(dir, name));
                int w = im.getWidth(), h = im.getHeight();
                float s = Math.min(1f, maxSide / (float) Math.max(w, h));
                if (s < 1f) {
                    int nw = Math.round(w * s), nh = Math.round(h * s);
                    BufferedImage r = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_ARGB);
                    java.awt.Graphics2D g = r.createGraphics();
                    g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g.drawImage(im, 0, 0, nw, nh, null);
                    g.dispose();
                    return r;
                }
                return im;
            } catch (Exception e) { return null; }
        }
        public int[] decode(String name, int maxSide) {
            BufferedImage im = read(name, maxSide);
            if (im == null) return null;
            int w = im.getWidth(), h = im.getHeight();
            int[] o = new int[w * h + 2];
            o[0] = w; o[1] = h;
            im.getRGB(0, 0, w, h, o, 2, w);
            return o;
        }
        public Object create(int[] argb, int w, int h) {
            BufferedImage b = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            b.setRGB(0, 0, w, h, argb, 0, w);
            return b;
        }
        public Object load(String name, int maxSide) { return read(name, maxSide); }
        public int width(Object img) { return ((BufferedImage) img).getWidth(); }
        public int height(Object img) { return ((BufferedImage) img).getHeight(); }
    }
}
