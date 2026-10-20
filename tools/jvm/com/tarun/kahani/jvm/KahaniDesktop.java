package com.tarun.kahani.jvm;

import com.tarun.kahani.core.ScriptParser;
import com.tarun.kahani.core.Story;
import com.tarun.kahani.core.Txt;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/**
 * v48 (the user: "convert it to exe"): the film maker for Windows — a window to pick the script and the folder of
 * pictures and make the film on the computer, with the same engine as the desktop films (MakeFilm) and Kokoro's
 * voices through sherpa-onnx's own program. Packaged as KahaniFilm.exe (tools/windows/build-windows.ps1).
 *
 * <p>The pictures folder, named the way the script names things:
 * <pre>
 *   pictures/
 *     वृंदा/         one folder per character: front, side, back, three-quarter pictures and sheets
 *     वनुषा/
 *     स्थान/ (or places/)    one folder per place, named as the script names it
 *       घर का छोटा लिविंग रूम/
 *     वस्तुएं/ (or things/)  one folder per thing: सोफा/, टेडी बियर/ …
 *     title.png, end.png     the starting and the ending page (optional)
 * </pre>
 * A file whose name says front / side / left / right / back / three-quarter (or सामने / बगल / पीछे) is that view of the
 * character; any other picture is read as a sheet of poses and cut by the studio's splitter. A folder that already
 * holds a cast.txt is used as it is. Everything not given is drawn by the studio.
 */
public final class KahaniDesktop {
    private final JFrame frame;
    private final JTextField script = new JTextField(40), pictures = new JTextField(40);
    private final JComboBox<String> size = new JComboBox<String>(new String[]{"720p (1280×720)", "480p (848×480)", "1080p (1920×1080)"});
    private final JTextArea log = new JTextArea(18, 70);
    private final JButton make = new JButton("🎬 Make the film");
    private volatile Process running;

    KahaniDesktop() { this(false); }
    KahaniDesktop(boolean headless) { this.headless = headless; frame = headless ? null : new JFrame("KahaniFilm — film maker"); }

    private boolean headless;
    private java.util.function.Consumer<String> sink;
    private String kvoices;

    /** v48: the engine for another window (the Windows app): its lines go to the sink. */
    public KahaniDesktop(java.util.function.Consumer<String> sink) { this(true); this.sink = sink; }

    /** Makes the film of the script with the pictures folder (or null), at the width, with the chosen voices ("name=speaker;…" or null). */
    public boolean film(File sc, File pics, int width, String voices) throws Exception { kvoices = voices; return makeFilm(sc, pics, width); }

    /** Stops the film being made. */
    public void stop() { Process p = running; if (p != null) p.destroyForcibly(); }

    /** With arguments (script [pictures folder or -] [width]) the film is made without a window. */
    public static void main(String[] a) throws Exception {
        if (a.length >= 1) {
            KahaniDesktop k = new KahaniDesktop(true);
            k.makeFilm(new File(a[0]), a.length > 1 && !a[1].equals("-") ? new File(a[1]) : null, a.length > 2 ? Integer.parseInt(a[2]) : 1280);
            return;
        }
        SwingUtilities.invokeLater(new Runnable() { public void run() { new KahaniDesktop().show(); } });
    }

    private void show() {
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 6, 4, 6);
        g.anchor = GridBagConstraints.WEST;
        row(form, g, 0, "Script (.txt):", script, chooser(script, false));
        row(form, g, 1, "Pictures folder:", pictures, chooser(pictures, true));
        g.gridx = 0; g.gridy = 2; form.add(new JLabel("Size:"), g);
        g.gridx = 1; form.add(size, g);
        g.gridx = 2; form.add(make, g);
        log.setEditable(false);
        log.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        log.setText("1. Choose the script (a .txt file, Hindi or English).\n"
                + "2. Choose the folder of pictures (a folder per character, स्थान/ for places, वस्तुएं/ for things) — or none:\n"
                + "   the studio draws what is not given.\n"
                + "3. Make the film. It is saved next to the script as <script name>.mp4.\n\n");
        make.addActionListener(e -> start());
        frame.getContentPane().add(form, BorderLayout.NORTH);
        frame.getContentPane().add(new JScrollPane(log), BorderLayout.CENTER);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent e) { Process p = running; if (p != null) p.destroy(); }
        });
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static void row(JPanel p, GridBagConstraints g, int y, String label, JTextField f, JButton b) {
        g.gridx = 0; g.gridy = y; p.add(new JLabel(label), g);
        g.gridx = 1; p.add(f, g);
        g.gridx = 2; p.add(b, g);
    }

    private JButton chooser(final JTextField into, final boolean folder) {
        JButton b = new JButton("Browse…");
        b.addActionListener(e -> {
            JFileChooser c = new JFileChooser(into.getText().isEmpty() ? null : new File(into.getText()));
            c.setFileSelectionMode(folder ? JFileChooser.DIRECTORIES_ONLY : JFileChooser.FILES_ONLY);
            if (c.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) into.setText(c.getSelectedFile().getAbsolutePath());
        });
        return b;
    }

    private void say(final String s) { if (sink != null) { sink.accept(s); return; } if (headless) { System.out.println(s); return; } SwingUtilities.invokeLater(() -> { log.append(s + "\n"); log.setCaretPosition(log.getDocument().getLength()); }); }

    private void start() {
        final File sc = new File(script.getText().trim());
        if (!sc.isFile()) { say("⚠ Choose the script first."); return; }
        final String pics = pictures.getText().trim();
        final int width = size.getSelectedIndex() == 1 ? 848 : size.getSelectedIndex() == 2 ? 1920 : 1280;
        make.setEnabled(false);
        new Thread(() -> {
            try { makeFilm(sc, pics.isEmpty() ? null : new File(pics), width); }
            catch (Exception ex) { say("⚠ " + ex); }
            finally { SwingUtilities.invokeLater(() -> make.setEnabled(true)); }
        }, "film").start();
    }

    /** The folder the program runs from (the app folder of KahaniFilm.exe, or the working folder). */
    public static File home() {
        String h = System.getProperty("kahani.home");
        if (h != null) return new File(h).getAbsoluteFile();
        try {
            File jar = new File(KahaniDesktop.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            return (jar.isFile() ? jar.getParentFile() : new File(".")).getAbsoluteFile();
        } catch (Exception e) { return new File(".").getAbsoluteFile(); }
    }

    private boolean makeFilm(File sc, File pics, int width) throws Exception {
        String text = new String(Files.readAllBytes(sc.toPath()), StandardCharsets.UTF_8);
        Story story = ScriptParser.parse(text);
        say("📖 " + story.title + ": " + story.characters.size() + " characters, " + story.scenes.size() + " scenes");
        File work = new File(sc.getAbsoluteFile().getParentFile(), stem(sc) + "_film");
        work.mkdirs();
        String assets = "-";
        if (pics != null && new File(pics, "cast.txt").isFile()) assets = pics.getAbsolutePath();
        else if (pics != null && pics.isDirectory()) {
            File plan = plan(story, pics, work);
            File castDir = new File(work, "cast");
            say("🖼 Reading the pictures…");
            int code = java(Arrays.asList("com.tarun.kahani.jvm.CastFromUploads", plan.getAbsolutePath(), castDir.getAbsolutePath()), work);
            if (code != 0 || !new File(castDir, "cast.txt").isFile()) say("⚠ The pictures could not be read; the studio draws everything.");
            else assets = castDir.getAbsolutePath();
        }
        File out = new File(sc.getAbsoluteFile().getParentFile(), stem(sc) + ".mp4");
        say("🎬 Making the film (this takes a while: every frame is drawn and checked)…");
        int code = java(Arrays.asList("com.tarun.kahani.jvm.MakeFilm", sc.getAbsolutePath(), assets, out.getAbsolutePath(),
                String.valueOf(width), "24", new File(work, "stills").getAbsolutePath()), work);
        if (code == 0 && out.isFile()) { say("✅ Done: " + out.getAbsolutePath()); return true; }
        say("⚠ The film could not be made (exit " + code + ") — see the lines above.");
        return false;
    }

    /** The plan of CastFromUploads from the pictures folder (see the class comment). */
    static File plan(Story story, File pics, File work) throws Exception {
        List<String> plan = new ArrayList<String>();
        File[] top = pics.listFiles();
        if (top == null) top = new File[0];
        Arrays.sort(top);
        for (Story.CharacterDef c : story.characters) {
            File dir = folderFor(top, c.displayName);
            if (dir == null && c.shown() != null) dir = folderFor(top, c.shown());
            if (dir == null) continue;
            File[] fs = pictures(dir);
            File front = null;
            for (File f : fs) if (angle(f.getName()) == 0 && named(f.getName())) front = f;
            if (front == null) for (File f : fs) if (angle(f.getName()) == 0) { front = f; break; }
            if (front == null && fs.length > 0) front = fs[0];
            if (front == null) continue;
            plan.add("char|" + c.displayName + "|" + front.getAbsolutePath());
            for (File f : fs) {
                if (f.equals(front)) continue;
                int an = angle(f.getName());
                if (named(f.getName()) && an != 0) plan.add("view|" + c.displayName + "|" + an + "|" + f.getAbsolutePath());
                else plan.add("figsheet|" + c.displayName + "|" + f.getAbsolutePath());
            }
        }
        File places = folderFor(top, "स्थान", "places", "place", "जगह");
        if (places != null) {
            File[] pd = places.listFiles(File::isDirectory);
            if (pd != null) {
                Arrays.sort(pd);
                File last = null;
                // (a place no scene is set in — the sofa corner of the room — is part of where the story happens: its
                // pictures are views of every scene, after the scene's own)
                java.util.Set<File> used = new java.util.HashSet<File>();
                for (int n = 1; n <= story.scenes.size(); n++) {
                    String setting = Txt.norm(story.scenes.get(n - 1).setting == null ? "" : story.scenes.get(n - 1).setting);
                    for (File d : pd) if (setting.contains(Txt.norm(d.getName()))) used.add(d);
                }
                List<File> extra = new ArrayList<File>();
                for (File d : pd) if (!used.contains(d)) extra.addAll(Arrays.asList(pictures(d)));
                for (int n = 1; n <= story.scenes.size(); n++) {
                    String setting = Txt.norm(story.scenes.get(n - 1).setting == null ? "" : story.scenes.get(n - 1).setting);
                    File best = null;
                    for (File d : pd) if (setting.contains(Txt.norm(d.getName()))) { if (best == null || d.getName().length() > best.getName().length()) best = d; }
                    if (best == null) best = last != null ? last : pd.length > 0 ? pd[0] : null;
                    if (best == null) continue;
                    last = best;
                    List<File> fs = new ArrayList<File>(Arrays.asList(pictures(best)));
                    for (File x : extra) if (!fs.contains(x)) fs.add(x);
                    // (a sheet of views of the place — up to 3×2 panels — is each of its panels, never the sheet as a backdrop)
                    int k = 0;
                    for (File f : fs) {
                        int np = panels(f);
                        for (int q = 0; q < Math.max(1, np) && k < 19; q++, k++)
                            plan.add("place|" + (k == 0 ? String.valueOf(n) : n + "v" + k) + "|" + f.getAbsolutePath() + (np >= 2 ? "|" + q : ""));
                    }
                }
            }
        }
        File things = folderFor(top, "वस्तुएं", "वस्तुएँ", "things", "objects");
        if (things != null) {
            File[] td = things.listFiles(File::isDirectory);
            // (a thing's sheet gives its first view; a single picture is used as it is)
            if (td != null) for (File d : td) { File[] fs = pictures(d); if (fs.length > 0) plan.add("prop|" + d.getName() + "|" + fs[0].getAbsolutePath() + (panels(fs[0]) >= 2 || pieces(fs[0]) >= 2 ? "|0" : "")); }
        }
        for (File f : top) {
            String n = f.getName().toLowerCase(Locale.ROOT);
            if (f.isFile() && n.startsWith("title.")) plan.add("title|" + f.getAbsolutePath());
            if (f.isFile() && n.startsWith("end.")) plan.add("end|" + f.getAbsolutePath());
        }
        File p = new File(work, "plan.txt");
        Files.write(p.toPath(), (String.join("\n", plan) + "\n").getBytes(StandardCharsets.UTF_8));
        return p;
    }

    /** The panels of a sheet with gutters (2 to 6, the user's 3×2), or 0 when the picture is one picture. */
    static int panels(File f) {
        try {
            java.awt.image.BufferedImage im = javax.imageio.ImageIO.read(f);
            if (im == null) return 0;
            int w = im.getWidth(), h = im.getHeight();
            List<int[]> p = com.tarun.kahani.core.Angles.sheetPanels(im.getRGB(0, 0, w, h, null, 0, w), w, h);
            return p == null || p.size() < 2 || p.size() > 6 ? 0 : p.size();
        } catch (Exception e) { return 0; }
    }

    /** The cut-outs on a sheet without gutters (things on black), 0 for one. */
    static int pieces(File f) {
        try {
            java.awt.image.BufferedImage im = javax.imageio.ImageIO.read(f);
            if (im == null) return 0;
            int w = im.getWidth(), h = im.getHeight();
            int n = com.tarun.kahani.core.Angles.split(im.getRGB(0, 0, w, h, null, 0, w), w, h, false).size();
            return n < 2 || n > 6 ? 0 : n;
        } catch (Exception e) { return 0; }
    }

    static File folderFor(File[] top, String... names) {
        for (File f : top) {
            if (!f.isDirectory()) continue;
            String n = Txt.norm(f.getName()).toLowerCase(Locale.ROOT).trim();
            for (String w : names) if (w != null && n.equals(Txt.norm(w).toLowerCase(Locale.ROOT).trim())) return f;
        }
        return null;
    }

    /** v48: the pictures in a folder and the folders inside it. */
    public static int pictureCount(File dir) {
        File[] fs = dir.listFiles();
        if (fs == null) return 0;
        int n = 0;
        for (File f : fs) n += f.isDirectory() ? pictureCount(f) : f.getName().toLowerCase(Locale.ROOT).matches(".*\\.(png|jpg|jpeg|webp)$") ? 1 : 0;
        return n;
    }

    public static File[] pictures(File dir) {
        File[] fs = dir.listFiles(f -> f.isFile() && f.getName().toLowerCase(Locale.ROOT).matches(".*\\.(png|jpg|jpeg|webp)$"));
        if (fs == null) return new File[0];
        Arrays.sort(fs);
        return fs;
    }

    /** True when a file's name says which side it shows. */
    public static boolean named(String name) {
        String n = name.toLowerCase(Locale.ROOT);
        return n.matches(".*(front|side|left|right|back|three|quarter|3q|3-4|profile|सामने|बगल|पीछे).*");
    }

    /** The view a file's name says: 0 front, 90 side, 180 back, -45 three-quarter. */
    public static int angle(String name) {
        String n = name.toLowerCase(Locale.ROOT);
        if (n.matches(".*(back|पीछे).*")) return 180;
        boolean left = n.matches(".*(left|बाएं|बायें|बाईं).*");
        if (n.matches(".*(three|quarter|3q|3-4).*")) return left ? -45 : 45;
        if (n.matches(".*(side|left|right|profile|बगल).*")) return left ? -90 : 90;
        return 0;
    }

    public static String stem(File f) { String n = f.getName(); int d = n.lastIndexOf('.'); return d > 0 ? n.substring(0, d) : n; }

    /** Runs a class of this program in a Java of its own (the settings of one film never leak into the next). */
    private int java(List<String> mainAndArgs, File work) throws Exception {
        File h = home();
        String javaExe = new File(System.getProperty("java.home"), "bin/" + (isWindows() ? "java.exe" : "java")).getAbsolutePath();
        List<String> cmd = new ArrayList<String>();
        cmd.add(javaExe);
        cmd.add("-Xmx" + filmMemoryMb() + "m");
        cmd.add("-Djava.awt.headless=true");
        cmd.add("-Dfile.encoding=UTF-8");
        cmd.add("-Dstdout.encoding=UTF-8");
        File assets = new File(h, "assets");
        if (assets.isDirectory()) cmd.add("-Dkahani.assets=" + assets.getAbsolutePath());
        cmd.add("-cp");
        // (each film runs in its own folder: the class path is made absolute)
        StringBuilder cpath = new StringBuilder();
        for (String e : System.getProperty("java.class.path").split(java.util.regex.Pattern.quote(File.pathSeparator))) {
            if (e.isEmpty()) continue;
            if (cpath.length() > 0) cpath.append(File.pathSeparator);
            cpath.append(new File(e).getAbsolutePath());
        }
        cmd.add(cpath.toString());
        cmd.addAll(mainAndArgs);
        ProcessBuilder pb = new ProcessBuilder(cmd).directory(work).redirectErrorStream(true);
        File ff = new File(h, isWindows() ? "ffmpeg/ffmpeg.exe" : "ffmpeg/ffmpeg");
        if (ff.isFile()) pb.environment().put("FFMPEG", ff.getAbsolutePath());
        File tts = new File(h, isWindows() ? "sherpa/sherpa-onnx-offline-tts.exe" : "sherpa/sherpa-onnx-offline-tts");
        File kd = new File(h, "kokoro");
        if (tts.isFile() && new File(kd, "voices.bin").isFile()) { pb.environment().put("KOKORO_CLI", tts.getAbsolutePath()); pb.environment().put("KOKORO_DIR", kd.getAbsolutePath()); }
        pb.environment().put("QC", "0");
        if (kvoices != null && kvoices.length() > 0) pb.environment().put("KVOICES", kvoices);
        Process p = pb.start();
        running = p;
        // (the whole of the engine's log is kept in the film's folder: log.txt)
        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8));
             java.io.Writer w = new java.io.OutputStreamWriter(new java.io.FileOutputStream(new File(work, "log.txt"), true), StandardCharsets.UTF_8)) {
            for (String l; (l = r.readLine()) != null; ) { w.write(l + "\n"); if (worth(l)) say("   " + l); }
        }
        int code = p.waitFor();
        running = null;
        return code;
    }

    /** v48: memory for a film's own Java: a third of the computer's (16 GB → about 5 GB), between 1.5 and 8 GB. */
    static long filmMemoryMb() {
        long total = 0;
        try {
            java.lang.management.OperatingSystemMXBean os = java.lang.management.ManagementFactory.getOperatingSystemMXBean();
            if (os instanceof com.sun.management.OperatingSystemMXBean) total = ((com.sun.management.OperatingSystemMXBean) os).getTotalMemorySize() >> 20;
        } catch (Throwable ignored) { }
        if (total <= 0) return 2048;
        return Math.max(1536, Math.min(8192, total / 3));
    }

    /** The lines of the engine's log worth showing (the rest is in the film's folder). */
    static boolean worth(String l) {
        return l.startsWith("VOICES") || l.startsWith("MODELS") || l.startsWith("KOKORO") || l.contains("frames") || l.startsWith("audio mixed")
                || l.contains("Exception") || l.startsWith("FRAME AUDIT") || l.startsWith("•") || l.contains("manifest lines") || l.startsWith("  unsure")
                || l.startsWith("pictures read at") || l.startsWith("WARNING") || l.startsWith("MAJOR");
    }

    public static boolean isWindows() { return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win"); }
}
