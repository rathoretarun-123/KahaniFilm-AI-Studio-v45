package com.tarun.kahani.app;

import com.tarun.kahani.core.ScriptParser;
import com.tarun.kahani.core.Story;
import com.tarun.kahani.jvm.KahaniDesktop;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * v48 (the user: "make the windows application looking like the apk, with same functionalities"): KahaniFilm for
 * Windows with the phone's screens and colours (Ui) — Home (the stories), Script (type, paste or open), Studio (exactly
 * what the script asks for: StudioPlan, the phone's own rule and limits), Make film (with its progress), Film (play the
 * MP4, its stills) and Library (every picture of the stories, Kokoro's voices). The film is made by the same engine
 * (KahaniDesktop → CastFromUploads → MakeFilm) with Kokoro's voices, MoveNet, FaceMesh and YuNet.
 *
 * <p>A story is a folder of Documents/KahaniFilm: script.txt, pictures/ (laid out as KahaniDesktop reads it: a folder
 * per character with its views named front / side_left / …, स्थान/&lt;place&gt;/, वस्तुएं/&lt;thing&gt;/, title.*,
 * end.*, संदर्भ/ for the additional references), voices.txt (name=Kokoro speaker), and the film script.mp4 with
 * script_film/ (stills, log). The phone-only parts (recording a voice, the phone's speech, its share sheet) are not here.
 */
public final class DesktopApp {
    static final Color BG = new Color(0xF7F2FA), PRIMARY = new Color(0x6A1B9A), PRIMARY_DARK = new Color(0x4A148C),
            CARD = Color.WHITE, TEXT = new Color(0x1A0D2E), SUB = new Color(0x5D4B7A), GREEN = new Color(0x2E7D32),
            RED = new Color(0xC62828), BLUE = new Color(0x0D47A1);
    static final String[] VIEWS = {"Front", "Side (left)", "Side (right)", "Back", "Three-quarter (left)", "Three-quarter (right)", "Sheet / pose"};
    static final String[] VIEW_FILES = {"front", "side_left", "side_right", "back", "three_quarter_left", "three_quarter_right", "sheet"};
    static final String PLACES = "स्थान", THINGS = "वस्तुएं", REFS = "संदर्भ";

    static Font font = new Font(Font.SANS_SERIF, Font.PLAIN, 15);

    final JFrame frame = new JFrame("KahaniFilm AI Studio");
    final CardLayout cards = new CardLayout();
    final JPanel deck = new JPanel(cards);
    final JLabel heading = new JLabel();
    final JButton back = new JButton("‹ Back");
    File project;
    String current = "home";
    final List<String> trail = new ArrayList<String>();

    public static void main(String[] a) {
        loadFont();
        SwingUtilities.invokeLater(() -> new DesktopApp().show());
    }

    /** The phone's own font (Mukta: Devanagari and Latin), from the program's assets. */
    static void loadFont() {
        for (File f : new File[]{new File(KahaniDesktop.home(), "assets/fonts/Mukta-SemiBold.ttf"), new File("app/src/main/assets/fonts/Mukta-SemiBold.ttf")}) {
            try {
                if (!f.isFile()) continue;
                Font m = Font.createFont(Font.TRUETYPE_FONT, f);
                GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(m);
                font = m.deriveFont(Font.PLAIN, 15f);
                break;
            } catch (Exception ignored) { }
        }
        for (String k : new String[]{"Button.font", "Label.font", "TextArea.font", "TextField.font", "ComboBox.font", "List.font", "OptionPane.messageFont", "OptionPane.buttonFont", "CheckBox.font"})
            UIManager.put(k, font);
    }

    static File root() {
        String h = System.getProperty("kahani.stories");
        File r = h != null ? new File(h) : new File(new File(System.getProperty("user.home"), "Documents"), "KahaniFilm");
        r.mkdirs();
        return r;
    }

    void show() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(PRIMARY);
        bar.setBorder(new EmptyBorder(10, 12, 10, 12));
        heading.setForeground(Color.WHITE);
        heading.setFont(font.deriveFont(Font.BOLD, 22f));
        back.setFocusPainted(false);
        style(back, PRIMARY_DARK, Color.WHITE);
        back.addActionListener(e -> goBack());
        bar.add(back, BorderLayout.WEST);
        bar.add(heading, BorderLayout.CENTER);
        heading.setBorder(new EmptyBorder(0, 12, 0, 0));
        JPanel nav = new JPanel(new GridLayout(1, 2, 8, 0));
        nav.setBackground(PRIMARY_DARK);
        nav.setBorder(new EmptyBorder(6, 8, 6, 8));
        JButton home = button("Stories", PRIMARY_DARK), lib = button("Library", PRIMARY_DARK);
        home.addActionListener(e -> { trail.clear(); open("home"); });
        lib.addActionListener(e -> open("library"));
        nav.add(home);
        nav.add(lib);
        deck.setBackground(BG);
        frame.getContentPane().setBackground(BG);
        frame.getContentPane().add(bar, BorderLayout.NORTH);
        frame.getContentPane().add(deck, BorderLayout.CENTER);
        frame.getContentPane().add(nav, BorderLayout.SOUTH);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent e) { if (engine != null) engine.stop(); }
        });
        frame.setSize(1100, 800);
        frame.setLocationRelativeTo(null);
        open("home");
        frame.setVisible(true);
    }

    // ---- navigation -------------------------------------------------------------------------------------------

    void open(String screen) {
        if (!screen.equals(current) && !current.equals("make")) trail.add(current);
        current = screen;
        JComponent c;
        switch (screen) {
            case "script": c = scriptScreen(); heading.setText("" + title()); break;
            case "studio": c = studioScreen(); heading.setText("Studio — " + title()); break;
            case "make": c = makeScreen(); heading.setText("Making the film"); break;
            case "film": c = filmScreen(); heading.setText("" + title()); break;
            case "library": c = libraryScreen(); heading.setText("Library"); break;
            default: c = homeScreen(); heading.setText("KahaniFilm AI Studio"); trail.clear();
        }
        back.setVisible(!screen.equals("home"));
        deck.removeAll();
        deck.add(c, screen);
        cards.show(deck, screen);
        deck.revalidate();
        deck.repaint();
    }

    void goBack() {
        if (busy) { say("The film is being made — stop it first."); return; }
        String to = trail.isEmpty() ? "home" : trail.remove(trail.size() - 1);
        current = to;
        open(to);
        if (!trail.isEmpty() && trail.get(trail.size() - 1).equals(to)) trail.remove(trail.size() - 1);
    }

    String title() { return project == null ? "" : project.getName(); }

    // ---- Home ------------------------------------------------------------------------------------------------------

    JComponent homeScreen() {
        JPanel col = column();
        JPanel actions = new JPanel(new GridLayout(1, 2, 10, 0));
        actions.setOpaque(false);
        actions.setAlignmentX(Component.LEFT_ALIGNMENT);
        JButton add = button("New story", PRIMARY), openTxt = button("Open a script (.txt)", BLUE);
        add.addActionListener(e -> {
            String n = JOptionPane.showInputDialog(frame, "The story's name:", "New story", JOptionPane.PLAIN_MESSAGE);
            if (n == null || n.trim().isEmpty()) return;
            project = newProject(n.trim(), "");
            open("script");
        });
        openTxt.addActionListener(e -> {
            JFileChooser ch = new JFileChooser();
            ch.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Scripts (.txt)", "txt"));
            if (ch.showOpenDialog(frame) != JFileChooser.APPROVE_OPTION) return;
            try {
                String text = new String(Files.readAllBytes(ch.getSelectedFile().toPath()), StandardCharsets.UTF_8);
                Story st = ScriptParser.parse(text);
                String name = st.title != null && st.title.trim().length() > 0 ? st.title.trim() : KahaniDesktop.stem(ch.getSelectedFile());
                project = newProject(name, text);
                open("script");
            } catch (Exception ex) { error(ex); }
        });
        actions.add(add);
        actions.add(openTxt);
        col.add(actions);
        col.add(gap(14));
        File[] ps = root().listFiles(f -> f.isDirectory() && new File(f, "script.txt").isFile());
        if (ps == null || ps.length == 0) col.add(note("No stories yet. Make a new one, or open a script you have written."));
        else {
            Arrays.sort(ps, (x, y) -> Long.compare(new File(y, "script.txt").lastModified(), new File(x, "script.txt").lastModified()));
            for (File p : ps) col.add(spaced(storyCard(p)));
        }
        return scroll(col);
    }

    JComponent storyCard(final File p) {
        JPanel c = card();
        c.setLayout(new BorderLayout(10, 0));
        File film = new File(p, "script.mp4");
        JLabel t = new JLabel("<html><b style='font-size:15pt'>" + esc(p.getName()) + "</b><br><span style='color:#5D4B7A'>"
                + (film.isFile() ? "film ready · " : "") + KahaniDesktop.pictureCount(new File(p, "pictures")) + "pictures</span></html>");
        t.setForeground(TEXT);
        c.add(t, BorderLayout.CENTER);
        JPanel bs = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        bs.setOpaque(false);
        JButton s = outline("Script"), st = outline("Studio"), f = outline("Film"), del = outline("Delete");
        s.addActionListener(e -> { project = p; open("script"); });
        st.addActionListener(e -> { project = p; open("studio"); });
        f.addActionListener(e -> { project = p; open("film"); });
        f.setEnabled(film.isFile());
        del.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(frame, "Delete the story \"" + p.getName() + "\" with its pictures and film?", "Delete",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) return;
            deleteTree(p);
            open("home");
        });
        bs.add(s); bs.add(st); bs.add(f); bs.add(del);
        c.add(bs, BorderLayout.EAST);
        return c;
    }

    File newProject(String name, String text) {
        String safe = safeName(name);
        File p = new File(root(), safe);
        for (int i = 2; p.exists(); i++) p = new File(root(), safe + "" + i);
        new File(p, "pictures").mkdirs();
        write(new File(p, "script.txt"), text);
        return p;
    }

    // ---- Script ----------------------------------------------------------------------------------------------------

    JComponent scriptScreen() {
        JPanel pane = new JPanel(new BorderLayout(0, 10));
        pane.setBackground(BG);
        pane.setBorder(new EmptyBorder(14, 14, 14, 14));
        final JTextArea ed = new JTextArea(read(new File(project, "script.txt")));
        ed.setLineWrap(true);
        ed.setWrapStyleWord(true);
        ed.setFont(font.deriveFont(17f));
        ed.setBorder(new EmptyBorder(10, 10, 10, 10));
        pane.add(new JScrollPane(ed), BorderLayout.CENTER);
        final JLabel info = new JLabel("");
        info.setForeground(SUB);
        JPanel bs = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bs.setOpaque(false);
        JButton save = button("Save", PRIMARY), check = button("Check the script", BLUE), studio = button("Studio ", GREEN), load = outline("Open .txt");
        save.addActionListener(e -> { write(new File(project, "script.txt"), ed.getText()); info.setText("Saved."); });
        check.addActionListener(e -> { write(new File(project, "script.txt"), ed.getText()); info.setText("<html>" + esc(summary(ed.getText())).replace("\n", "<br>") + "</html>"); });
        studio.addActionListener(e -> { write(new File(project, "script.txt"), ed.getText()); open("studio"); });
        load.addActionListener(e -> {
            JFileChooser ch = new JFileChooser();
            if (ch.showOpenDialog(frame) != JFileChooser.APPROVE_OPTION) return;
            try { ed.setText(new String(Files.readAllBytes(ch.getSelectedFile().toPath()), StandardCharsets.UTF_8)); } catch (Exception ex) { error(ex); }
        });
        bs.add(save); bs.add(check); bs.add(load); bs.add(studio);
        JPanel south = new JPanel(new BorderLayout(0, 6));
        south.setOpaque(false);
        south.add(bs, BorderLayout.NORTH);
        south.add(info, BorderLayout.CENTER);
        pane.add(south, BorderLayout.SOUTH);
        return pane;
    }

    /** What the script holds, as the Studio will read it. */
    static String summary(String text) {
        Story st = StudioPlan.voicesOnly(ScriptParser.parse(text));
        StringBuilder b = new StringBuilder();
        b.append("").append(st.title).append(": ").append(st.scenes.size()).append("scenes\n");
        List<String> cs = new ArrayList<String>();
        for (Story.CharacterDef c : StudioPlan.characters(st)) cs.add(c.displayName);
        b.append("Characters: ").append(cs.isEmpty() ? "none" : String.join(", ", cs)).append('\n');
        List<String> os = new ArrayList<String>();
        for (String[] o : StudioPlan.objects(st)) os.add(o[0]);
        b.append("Objects: ").append(os.isEmpty() ? "none" : String.join(", ", os)).append('\n');
        List<String> pl = new ArrayList<String>();
        for (StudioPlan.Place p : StudioPlan.places(st)) pl.add(p.name);
        b.append("Places: ").append(pl.isEmpty() ? "none" : String.join(", ", pl));
        return b.toString();
    }

    // ---- Studio ----------------------------------------------------------------------------------------------------

    JComponent studioScreen() {
        JPanel col = column();
        Story st = StudioPlan.voicesOnly(ScriptParser.parse(read(new File(project, "script.txt"))));
        File pics = new File(project, "pictures");
        JButton make = button("Make the film", GREEN);
        make.addActionListener(e -> open("make"));
        col.add(make);
        col.add(gap(8));
        col.add(note("The Studio asks only for what the script names. Anything not given is drawn by the studio. "
                + "A sheet is cut into at most 3×2 panels; a bigger one is kept whole."));
        if (st.scenes.isEmpty()) col.add(note("The script has no scenes yet — write it first ( Script)."));
        Map<String, String> voices = readVoices();
        List<Story.CharacterDef> chars = StudioPlan.characters(st);
        if (!chars.isEmpty()) col.add(section("Characters"));
        for (Story.CharacterDef c : chars)
            col.add(slot("" + c.displayName, new File(pics, safeName(c.displayName)), StudioPlan.MAX_CHARACTER_PICTURES, true, voiceChooser(c.displayName, voices)));
        List<String[]> objs = StudioPlan.objects(st);
        if (!objs.isEmpty()) col.add(section("Objects"));
        for (String[] o : objs) col.add(slot("" + o[0], new File(new File(pics, THINGS), safeName(o[0])), StudioPlan.MAX_OBJECT_PICTURES, false, null));
        List<StudioPlan.Place> places = StudioPlan.places(st);
        if (!places.isEmpty()) col.add(section("Places and backgrounds"));
        for (StudioPlan.Place p : places)
            col.add(slot("" + p.name + (p.partOf() ? "(around every scene)" : "(scene " + scenes(p) + ")"), new File(new File(pics, PLACES), safeName(p.name)), StudioPlan.MAX_PLACE_PICTURES, false, null));
        col.add(section("Starting and ending page"));
        col.add(page("Starting page", pics, "title"));
        col.add(page("Ending page", pics, "end"));
        col.add(section("Additional references"));
        col.add(slot("Additional reference pictures (kept with the story)", new File(pics, REFS), StudioPlan.MAX_ADDITIONAL_REFERENCES, false, null));
        return scroll(col);
    }

    static String scenes(StudioPlan.Place p) {
        List<String> n = new ArrayList<String>();
        for (Story.Scene s : p.scenes) n.add(String.valueOf(s.number));
        return String.join(", ", n);
    }

    /** One request: its pictures (with the view each is), Add pictures, and for a character its voice. */
    JComponent slot(final String label, final File dir, final int limit, final boolean views, JComponent extra) { return spaced(slotCard(label, dir, limit, views, extra)); }

    JComponent slotCard(final String label, final File dir, final int limit, final boolean views, JComponent extra) {
        final JPanel c = card();
        c.setLayout(new BorderLayout(0, 8));
        final File[] fs = KahaniDesktop.pictures(dir);
        JLabel t = new JLabel("<html><b style='font-size:14pt'>" + esc(label) + "</b>  <span style='color:#5D4B7A'>" + fs.length + "/ " + limit + "</span></html>");
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(t, BorderLayout.CENTER);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        right.setOpaque(false);
        if (extra != null) right.add(extra);
        JButton add = button("Add pictures", PRIMARY);
        add.setEnabled(fs.length < limit);
        add.addActionListener(e -> {
            File[] chosen = choosePictures();
            if (chosen == null) return;
            int room = limit - fs.length;
            if (chosen.length > room) say("Only " + room + "more pictures fit here (" + limit + "in all); the first " + room + "are added.");
            dir.mkdirs();
            for (int i = 0; i < Math.min(room, chosen.length); i++) {
                String view = views ? askView(chosen[i]) : "picture";
                if (view == null) continue;
                copyInto(chosen[i], dir, view);
            }
            refresh();
        });
        right.add(add);
        top.add(right, BorderLayout.EAST);
        c.add(top, BorderLayout.NORTH);
        JPanel thumbs = new JPanel(new WrapLayout());
        thumbs.setOpaque(false);
        for (final File f : fs) thumbs.add(thumb(f, views ? viewName(f.getName()) : null, () -> { f.delete(); refresh(); }));
        if (fs.length == 0) thumbs.add(sub("No pictures yet — the studio draws it."));
        c.add(thumbs, BorderLayout.CENTER);
        return c;
    }

    JComponent page(String label, final File pics, final String stem) { return spaced(pageCard(label, pics, stem)); }

    JComponent pageCard(String label, final File pics, final String stem) {
        JPanel c = card();
        c.setLayout(new BorderLayout(8, 0));
        File have = null;
        for (File f : KahaniDesktop.pictures(pics)) if (KahaniDesktop.stem(f).equalsIgnoreCase(stem)) have = f;
        c.add(new JLabel("<html><b style='font-size:14pt'>" + esc(label) + "</b>  <span style='color:#5D4B7A'>" + (have == null ? "0" : "1") + "/ 1</span></html>"), BorderLayout.NORTH);
        JPanel row = new JPanel(new WrapLayout());
        row.setOpaque(false);
        final File h = have;
        if (h != null) row.add(thumb(h, null, () -> { h.delete(); refresh(); }));
        else row.add(sub("None — the studio makes one from the title."));
        JButton pick = button(h == null ? "Choose a picture" : "Replace", PRIMARY);
        pick.addActionListener(e -> {
            File[] ch = choosePictures();
            if (ch == null || ch.length == 0) return;
            if (h != null) h.delete();
            copy(ch[0], new File(pics, stem + ext(ch[0])));
            refresh();
        });
        row.add(pick);
        c.add(row, BorderLayout.CENTER);
        return c;
    }

    /** Kokoro's voices for a character: automatic (the story's cast) or one of the library's eight. */
    JComponent voiceChooser(final String name, final Map<String, String> voices) {
        final List<String[]> vs = kokoroVoices();
        String[] items = new String[vs.size() + 1];
        items[0] = "Voice: automatic";
        int sel = 0;
        for (int i = 0; i < vs.size(); i++) {
            items[i + 1] = "" + vs.get(i)[0] + "(" + vs.get(i)[2] + ", " + vs.get(i)[3] + ")";
            if (vs.get(i)[1].equals(voices.get(name))) sel = i + 1;
        }
        final JComboBox<String> box = new JComboBox<String>(items);
        box.setSelectedIndex(sel);
        box.addActionListener(e -> {
            int i = box.getSelectedIndex();
            Map<String, String> now = readVoices();
            if (i <= 0) now.remove(name); else now.put(name, vs.get(i - 1)[1]);
            writeVoices(now);
            if (i > 0) play(new File(KahaniDesktop.home(), "assets/" + vs.get(i - 1)[4]));
        });
        return box;
    }

    String askView(File f) {
        JLabel pic = new JLabel(icon(f, 220));
        JComboBox<String> box = new JComboBox<String>(VIEWS);
        box.setSelectedIndex(guessView(f.getName()));
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.add(pic, BorderLayout.CENTER);
        p.add(box, BorderLayout.SOUTH);
        int r = JOptionPane.showConfirmDialog(frame, p, "Which view is this picture?", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        return r == JOptionPane.OK_OPTION ? VIEW_FILES[box.getSelectedIndex()] : null;
    }

    static int guessView(String name) {
        if (!KahaniDesktop.named(name)) return 6;
        int a = KahaniDesktop.angle(name);
        return a == 0 ? 0 : a == -90 ? 1 : a == 90 ? 2 : a == 180 ? 3 : a == -45 ? 4 : 5;
    }

    static String viewName(String file) {
        String n = file.toLowerCase(Locale.ROOT);
        for (int i = 0; i < VIEW_FILES.length - 1; i++) if (n.startsWith(VIEW_FILES[i] + ".")) return VIEWS[i];
        return n.startsWith("sheet") ? VIEWS[6] : "Pose";
    }

    /** Copies the picture into the folder as &lt;view&gt;.ext (a second one of a view: &lt;view&gt;_2.ext — then read as a pose). */
    static File copyInto(File f, File dir, String view) {
        String e = ext(f);
        File to = new File(dir, view + e);
        for (int i = 2; to.exists(); i++) to = new File(dir, view + "_" + i + e);
        copy(f, to);
        return to;
    }

    File[] choosePictures() {
        JFileChooser ch = new JFileChooser();
        ch.setMultiSelectionEnabled(true);
        ch.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Pictures", "png", "jpg", "jpeg", "webp"));
        if (ch.showOpenDialog(frame) != JFileChooser.APPROVE_OPTION) return null;
        return ch.getSelectedFiles();
    }

    void refresh() {
        JScrollPane sp = deck.getComponentCount() > 0 && deck.getComponent(0) instanceof JScrollPane ? (JScrollPane) deck.getComponent(0) : null;
        final int y = sp == null ? 0 : sp.getVerticalScrollBar().getValue();
        String s = current;
        current = "";
        open(s);
        if (!trail.isEmpty()) trail.remove(trail.size() - 1);
        SwingUtilities.invokeLater(() -> {
            if (deck.getComponentCount() > 0 && deck.getComponent(0) instanceof JScrollPane) ((JScrollPane) deck.getComponent(0)).getVerticalScrollBar().setValue(y);
        });
    }

    // ---- Make film -------------------------------------------------------------------------------------------------

    volatile boolean busy;
    volatile KahaniDesktop engine;
    JTextArea log;

    JComponent makeScreen() {
        JPanel pane = new JPanel(new BorderLayout(0, 10));
        pane.setBackground(BG);
        pane.setBorder(new EmptyBorder(14, 14, 14, 14));
        final JComboBox<String> size = new JComboBox<String>(new String[]{"720p (1280×720)", "480p (848×480) — quicker", "1080p (1920×1080)"});
        final JButton go = button("Start", GREEN), stop = button("Stop", RED), see = button("See the film", PRIMARY);
        final JProgressBar bar = new JProgressBar();
        final JLabel status = new JLabel("Choose the size and start. A film of a few minutes takes a while: every frame is drawn and checked.");
        status.setForeground(SUB);
        stop.setEnabled(false);
        see.setVisible(new File(project, "script.mp4").isFile());
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        top.setOpaque(false);
        top.add(new JLabel("Size:"));
        top.add(size); top.add(go); top.add(stop); top.add(see);
        JPanel north = new JPanel(new BorderLayout(0, 8));
        north.setOpaque(false);
        north.add(top, BorderLayout.NORTH);
        north.add(bar, BorderLayout.CENTER);
        north.add(status, BorderLayout.SOUTH);
        pane.add(north, BorderLayout.NORTH);
        log = new JTextArea();
        log.setEditable(false);
        log.setFont(font.deriveFont(13f));
        pane.add(new JScrollPane(log), BorderLayout.CENTER);
        see.addActionListener(e -> open("film"));
        stop.addActionListener(e -> { KahaniDesktop k = engine; if (k != null) k.stop(); status.setText("Stopped."); });
        go.addActionListener(e -> {
            final int width = size.getSelectedIndex() == 1 ? 848 : size.getSelectedIndex() == 2 ? 1920 : 1280;
            go.setEnabled(false); stop.setEnabled(true); see.setVisible(false); back.setEnabled(false);
            bar.setIndeterminate(true);
            busy = true;
            final long t0 = System.currentTimeMillis();
            final Timer tick = new Timer(1000, ev -> {
                long s = (System.currentTimeMillis() - t0) / 1000;
                status.setText(String.format(Locale.US, "Making the film… %d:%02d", s / 60, s % 60));
            });
            tick.start();
            log.setText("");
            new Thread(() -> {
                boolean ok = false;
                try {
                    engine = new KahaniDesktop(line -> SwingUtilities.invokeLater(() -> { log.append(line + "\n"); log.setCaretPosition(log.getDocument().getLength()); }));
                    ok = engine.film(new File(project, "script.txt"), new File(project, "pictures"), width, voicesSetting());
                } catch (Exception ex) { final String m = String.valueOf(ex); SwingUtilities.invokeLater(() -> log.append("" + m + "\n")); }
                final boolean done = ok;
                SwingUtilities.invokeLater(() -> {
                    tick.stop();
                    busy = false; engine = null;
                    bar.setIndeterminate(false); bar.setValue(done ? 100 : 0);
                    go.setEnabled(true); stop.setEnabled(false); back.setEnabled(true);
                    status.setText(done ? "The film is ready." : "The film was not made — the lines above say why (the whole log: script_film/log.txt).");
                    see.setVisible(done);
                    if (done) open("film");
                });
            }, "film").start();
        });
        return pane;
    }

    String voicesSetting() {
        StringBuilder b = new StringBuilder();
        for (Map.Entry<String, String> e : readVoices().entrySet()) { if (b.length() > 0) b.append(';'); b.append(e.getKey()).append('=').append(e.getValue()); }
        return b.toString();
    }

    // ---- Film ------------------------------------------------------------------------------------------------------

    JComponent filmScreen() {
        JPanel col = column();
        final File mp4 = new File(project, "script.mp4");
        if (!mp4.isFile()) {
            col.add(note("No film yet."));
            JButton m = button("Make the film", GREEN);
            m.addActionListener(e -> open("make"));
            col.add(m);
            return scroll(col);
        }
        JPanel bs = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bs.setOpaque(false);
        bs.setAlignmentX(Component.LEFT_ALIGNMENT);
        JButton play = button("  Play the film", PRIMARY), folder = button("Open its folder", BLUE), save = outline("Save a copy…"), again = outline("Make it again");
        play.addActionListener(e -> desktopOpen(mp4));
        folder.addActionListener(e -> desktopOpen(project));
        save.addActionListener(e -> {
            JFileChooser ch = new JFileChooser();
            ch.setSelectedFile(new File(safeName(project.getName()) + ".mp4"));
            if (ch.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) copy(mp4, ch.getSelectedFile());
        });
        again.addActionListener(e -> open("make"));
        bs.add(play); bs.add(folder); bs.add(save); bs.add(again);
        col.add(bs);
        col.add(gap(8));
        col.add(note(String.format(Locale.US, "%s — %.1f MB, made %s", mp4.getName(), mp4.length() / 1048576.0,
                new java.text.SimpleDateFormat("d MMM yyyy, HH:mm").format(new java.util.Date(mp4.lastModified())))));
        String lg = read(new File(project, "script_film/log.txt"));
        List<String> facts = new ArrayList<String>();
        for (String l : lg.split("\n")) if (l.startsWith("VOICES") || l.startsWith("MODELS") || l.startsWith("FRAME AUDIT")) facts.add(l);
        if (!facts.isEmpty()) col.add(note(String.join("\n", facts.subList(Math.max(0, facts.size() - 3), facts.size()))));
        File[] stills = KahaniDesktop.pictures(new File(project, "script_film/stills"));
        if (stills.length > 0) {
            col.add(section("Stills"));
            JPanel g = new JPanel(new WrapLayout());
            g.setOpaque(false);
            g.setAlignmentX(Component.LEFT_ALIGNMENT);
            for (File s : stills) g.add(thumb(s, null, null));
            col.add(g);
        }
        return scroll(col);
    }

    // ---- Library ---------------------------------------------------------------------------------------------------

    JComponent libraryScreen() {
        JPanel col = column();
        col.add(section("Kokoro voices (choose one for a character in the Studio)"));
        for (final String[] v : kokoroVoices()) {
            JPanel c = card();
            c.setLayout(new BorderLayout());
            c.add(new JLabel("<html><b>" + esc(v[0]) + "</b> — " + esc(v[2]) + ", " + esc(v[3]) + "<br><span style='color:#5D4B7A'>“" + esc(v[5]) + "”</span></html>"), BorderLayout.CENTER);
            JButton p = button(" Listen", PRIMARY);
            p.addActionListener(e -> play(new File(KahaniDesktop.home(), "assets/" + v[4])));
            c.add(p, BorderLayout.EAST);
            col.add(spaced(c));
        }
        File[] ps = root().listFiles(File::isDirectory);
        if (ps != null) {
            Arrays.sort(ps);
            for (File p : ps) {
                List<File> all = new ArrayList<File>();
                collect(new File(p, "pictures"), all);
                if (all.isEmpty()) continue;
                col.add(section("" + p.getName() + "— " + all.size() + "pictures"));
                JPanel g = new JPanel(new WrapLayout());
                g.setOpaque(false);
                g.setAlignmentX(Component.LEFT_ALIGNMENT);
                for (File f : all) g.add(thumb(f, f.getParentFile().getName(), null));
                col.add(g);
            }
        }
        return scroll(col);
    }

    static void collect(File d, List<File> out) {
        File[] fs = d.listFiles();
        if (fs == null) return;
        Arrays.sort(fs);
        for (File f : fs) if (f.isDirectory()) collect(f, out); else if (f.getName().toLowerCase(Locale.ROOT).matches(".*\\.(png|jpg|jpeg|webp)$")) out.add(f);
    }

    /** The library's Kokoro voices: {name, speaker, language, gender, file, sample} from assets/voices/kokoro/index.json. */
    static List<String[]> kokoroVoices() {
        List<String[]> out = new ArrayList<String[]>();
        String j = read(new File(KahaniDesktop.home(), "assets/voices/kokoro/index.json"));
        if (j.isEmpty()) j = read(new File("app/src/main/assets/voices/kokoro/index.json"));
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\{[^{}]*\\}").matcher(j);
        while (m.find()) {
            String o = m.group();
            String voice = field(o, "voice"), lang = field(o, "language"), gender = field(o, "gender");
            out.add(new String[]{voice, num(o, "speaker"), lang.equals("hi") ? "Hindi" : "English", gender, field(o, "file"), field(o, "sample")});
        }
        return out;
    }

    static String field(String o, String k) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"" + k + "\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(o);
        return m.find() ? m.group(1) : "";
    }

    static String num(String o, String k) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"" + k + "\"\\s*:\\s*(\\d+)").matcher(o);
        return m.find() ? m.group(1) : "";
    }

    /** Plays a voice sample (ffmpeg turns the .ogg into sound Java can play). */
    void play(final File ogg) {
        new Thread(() -> {
            try {
                File ff = new File(KahaniDesktop.home(), KahaniDesktop.isWindows() ? "ffmpeg/ffmpeg.exe" : "ffmpeg/ffmpeg");
                String exe = ff.isFile() ? ff.getAbsolutePath() : "ffmpeg";
                File wav = File.createTempFile("kahani-voice", ".wav");
                wav.deleteOnExit();
                Process p = new ProcessBuilder(exe, "-y", "-loglevel", "error", "-i", ogg.getAbsolutePath(), wav.getAbsolutePath()).redirectErrorStream(true).start();
                p.getInputStream().readAllBytes();
                if (p.waitFor() != 0) throw new Exception("ffmpeg could not read " + ogg.getName());
                javax.sound.sampled.Clip clip = javax.sound.sampled.AudioSystem.getClip();
                clip.open(javax.sound.sampled.AudioSystem.getAudioInputStream(wav));
                clip.start();
            } catch (Exception ex) { say("The voice could not be played: " + ex.getMessage()); }
        }, "voice").start();
    }

    // ---- voices.txt ------------------------------------------------------------------------------------------------

    Map<String, String> readVoices() {
        Map<String, String> m = new LinkedHashMap<String, String>();
        for (String l : read(new File(project, "voices.txt")).split("\n")) {
            int eq = l.lastIndexOf('=');
            if (eq > 0) m.put(l.substring(0, eq).trim(), l.substring(eq + 1).trim());
        }
        return m;
    }

    void writeVoices(Map<String, String> m) {
        StringBuilder b = new StringBuilder();
        for (Map.Entry<String, String> e : m.entrySet()) b.append(e.getKey()).append('=').append(e.getValue()).append('\n');
        write(new File(project, "voices.txt"), b.toString());
    }

    // ---- look (Ui's colours, cards and buttons) -------------------------------------------------------------------

    static JPanel column() {
        JPanel col = new JPanel();
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        col.setBackground(BG);
        col.setBorder(new EmptyBorder(14, 14, 14, 14));
        return col;
    }

    static JComponent scroll(JPanel col) {
        JPanel wrap = new Page();
        wrap.setBackground(BG);
        wrap.add(col, BorderLayout.NORTH);
        JScrollPane sp = new JScrollPane(wrap, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sp.setBorder(null);
        sp.getVerticalScrollBar().setUnitIncrement(24);
        return sp;
    }

    static JPanel card() {
        JPanel c = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0, 0, 0, 0x22));
                g2.fillRoundRect(0, 1, getWidth() - 1, getHeight() - 1, 24, 24);
                g2.setColor(CARD);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 2, 24, 24);
                g2.dispose();
            }
        };
        c.setOpaque(false);
        c.setBorder(new EmptyBorder(12, 14, 12, 14));
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        return c;
    }

    static JComponent section(String s) {
        JLabel l = new JLabel(s);
        l.setFont(font.deriveFont(Font.BOLD, 18f));
        l.setForeground(PRIMARY_DARK);
        l.setBorder(new EmptyBorder(16, 2, 6, 0));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    static JComponent note(String s) {
        JPanel c = card();
        c.setLayout(new BorderLayout());
        JLabel l = new JLabel("<html>" + esc(s).replace("\n", "<br>") + "</html>");
        l.setForeground(SUB);
        c.add(l);
        return spaced(c);
    }

    static JComponent spaced(JComponent c) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(0, 0, 8, 0));
        p.add(c);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        return p;
    }

    static JLabel sub(String s) { JLabel l = new JLabel(s); l.setForeground(SUB); return l; }

    static Component gap(int h) { return Box.createVerticalStrut(h); }

    static JButton button(String s, Color fill) {
        JButton b = new JButton(s);
        style(b, fill, Color.WHITE);
        return b;
    }

    static JButton outline(String s) {
        JButton b = new JButton(s);
        style(b, Color.WHITE, PRIMARY);
        b.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(PRIMARY, 2, true), new EmptyBorder(6, 12, 6, 12)));
        return b;
    }

    static void style(JButton b, Color fill, Color ink) {
        b.setBackground(fill);
        b.setForeground(ink);
        b.setOpaque(true);
        b.setContentAreaFilled(true);
        b.setFocusPainted(false);
        b.setBorderPainted(fill.equals(Color.WHITE));
        b.setFont(font.deriveFont(Font.BOLD, 15f));
        b.setBorder(new EmptyBorder(8, 16, 8, 16));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    /** A picture's thumbnail (click: open it), with its view below and ✕ to remove it. */
    JComponent thumb(final File f, String caption, final Runnable remove) {
        JPanel p = new JPanel(new BorderLayout(0, 2));
        p.setOpaque(false);
        JLabel pic = new JLabel(icon(f, 110));
        pic.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        pic.setToolTipText(f.getName());
        pic.addMouseListener(new MouseAdapter() { public void mouseClicked(MouseEvent e) { desktopOpen(f); } });
        p.add(pic, BorderLayout.CENTER);
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        if (caption != null) { JLabel c = sub(caption); c.setFont(font.deriveFont(12f)); row.add(c, BorderLayout.CENTER); }
        if (remove != null) {
            JButton x = new JButton("Remove");
            style(x, Color.WHITE, RED);
            x.setBorder(new EmptyBorder(0, 6, 0, 6));
            x.setToolTipText("Remove this picture");
            x.addActionListener(e -> remove.run());
            row.add(x, BorderLayout.EAST);
        }
        p.add(row, BorderLayout.SOUTH);
        return p;
    }

    static final Map<String, ImageIcon> ICONS = new java.util.HashMap<String, ImageIcon>();

    static ImageIcon icon(File f, int side) {
        String key = f.getAbsolutePath() + "|" + f.lastModified() + "|" + side;
        ImageIcon have = ICONS.get(key);
        if (have != null) return have;
        BufferedImage out = new BufferedImage(side, side, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setColor(new Color(0xEDE4F3));
        g.fillRoundRect(0, 0, side, side, 16, 16);
        try {
            BufferedImage im = ImageIO.read(f);
            if (im != null) {
                double s = Math.min(side / (double) im.getWidth(), side / (double) im.getHeight());
                int w = (int) Math.round(im.getWidth() * s), h = (int) Math.round(im.getHeight() * s);
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.drawImage(im, (side - w) / 2, (side - h) / 2, w, h, null);
            } else { g.setColor(SUB); g.drawString("?", side / 2, side / 2); }
        } catch (Exception e) { g.setColor(RED); g.drawString("unreadable", 6, side / 2); }
        g.dispose();
        ImageIcon ic = new ImageIcon(out);
        ICONS.put(key, ic);
        return ic;
    }

    // ---- small helpers ---------------------------------------------------------------------------------------------

    void say(String s) { JOptionPane.showMessageDialog(frame, s, "KahaniFilm", JOptionPane.INFORMATION_MESSAGE); }

    void error(Exception e) { JOptionPane.showMessageDialog(frame, String.valueOf(e), "KahaniFilm", JOptionPane.ERROR_MESSAGE); }

    void desktopOpen(File f) {
        try { Desktop.getDesktop().open(f); } catch (Exception e) { say("Could not open " + f.getName() + ": " + e.getMessage()); }
    }

    /** A name fit for a Windows folder (the script's own name otherwise: Hindi stays Hindi). */
    static String safeName(String n) {
        String s = n.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "").replaceAll("\\s+", "").trim();
        while (s.endsWith(".")) s = s.substring(0, s.length() - 1).trim();
        return s.isEmpty() ? "story" : s;
    }

    static String ext(File f) {
        String n = f.getName();
        int d = n.lastIndexOf('.');
        return d > 0 ? n.substring(d).toLowerCase(Locale.ROOT) : ".png";
    }

    static void copy(File from, File to) {
        try { to.getParentFile().mkdirs(); Files.copy(from.toPath(), to.toPath(), StandardCopyOption.REPLACE_EXISTING); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    static String read(File f) {
        try { return f.isFile() ? new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8) : ""; } catch (Exception e) { return ""; }
    }

    static void write(File f, String s) {
        try { f.getParentFile().mkdirs(); Files.write(f.toPath(), s.getBytes(StandardCharsets.UTF_8)); } catch (Exception e) { throw new RuntimeException(e); }
    }

    static void deleteTree(File f) {
        File[] fs = f.listFiles();
        if (fs != null) for (File x : fs) deleteTree(x);
        f.delete();
    }

    static String esc(String s) { return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"); }

    /** A page as wide as the window (its rows of pictures wrap instead of running off to the right). */
    static final class Page extends JPanel implements Scrollable {
        Page() { super(new BorderLayout()); }
        public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 24; }
        public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return r.height - 24; }
        public boolean getScrollableTracksViewportWidth() { return true; }
        public boolean getScrollableTracksViewportHeight() { return false; }
    }

    /** A FlowLayout whose preferred size wraps its rows to the width it is given. */
    static final class WrapLayout extends FlowLayout {
        WrapLayout() { super(FlowLayout.LEFT, 6, 6); }
        @Override public Dimension preferredLayoutSize(Container t) { return size(t, true); }
        @Override public Dimension minimumLayoutSize(Container t) { Dimension d = size(t, false); d.width -= getHgap() + 1; return d; }
        private Dimension size(Container t, boolean preferred) {
            synchronized (t.getTreeLock()) {
                Container c = t;
                while (c.getSize().width == 0 && c.getParent() != null) c = c.getParent();
                int max = c.getSize().width;
                if (max == 0) max = Integer.MAX_VALUE;
                Insets in = t.getInsets();
                max -= in.left + in.right + getHgap() * 2 + 40;
                Dimension d = new Dimension(0, 0);
                int rowW = 0, rowH = 0;
                for (Component m : t.getComponents()) {
                    if (!m.isVisible()) continue;
                    Dimension s = preferred ? m.getPreferredSize() : m.getMinimumSize();
                    if (rowW + s.width > max && rowW > 0) { d.width = Math.max(d.width, rowW); d.height += rowH + getVgap(); rowW = 0; rowH = 0; }
                    rowW += s.width + getHgap();
                    rowH = Math.max(rowH, s.height);
                }
                d.width = Math.max(d.width, rowW);
                d.height += rowH + getVgap() * 2 + in.top + in.bottom;
                d.width += in.left + in.right + getHgap() * 2;
                return d;
            }
        }
    }
}
