import com.tarun.kahani.core.*;
import java.lang.reflect.Field;
import java.util.*;
/** Directs one-sentence directions and prints what each staged: body, acts, holds, weather, wind. */
public class StageProbe {
    static Map<Integer,String> FX = new HashMap<Integer,String>();
    static String fxName(int t) { if (FX.isEmpty()) try { for (Field f : Film.class.getFields()) if (f.getName().startsWith("FX_") && f.getType() == int.class) FX.put(f.getInt(null), f.getName()); } catch (Exception e) { } return FX.containsKey(t) ? FX.get(t) : "FX?" + t; }
    public static void main(String[] a) throws Exception {
        Map<Integer, String> gNames = new HashMap<Integer, String>(), wNames = new HashMap<Integer, String>();
        for (Field f : Film.class.getFields()) { if (f.getName().startsWith("G_") && f.getType() == int.class) gNames.put(f.getInt(null), f.getName()); if (f.getName().startsWith("W_") && f.getType() == int.class) wNames.put(f.getInt(null), f.getName()); }
        String[] sents = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(a[0])), "UTF-8").trim().split("\n");
        for (String s : sents) {
            String script = "शीर्षक: परख\n\nपात्र:\nराम: एक 30 साल का आदमी, नीला कुर्ता।\nसीता: एक 28 साल की औरत, लाल साड़ी।\n\nदृश्य 1: आँगन, सुबह\n(राम और सीता आँगन में खड़े हैं।)\nराम: \"नमस्ते।\"\n(" + s + ")\nसीता: \"ठीक है।\"\n";
            Story story = ScriptParser.parse(script);
            Director d = new Director(story, new Director.Options());
            d.prepare();
            Film film = d.direct(new Art());
            StringBuilder out = new StringBuilder();
            for (Film.Seg sg : film.segs) {
                if (sg.type != Film.S_SCENE) continue;
                for (Film.Actor ac : sg.actors) {
                    Set<String> acts = new LinkedHashSet<String>();
                    for (Film.Act x : ac.acts) if (x.t0 > sg.t0 + 2.5f) acts.add(gNames.containsKey(x.type) ? gNames.get(x.type) : "G?" + x.type);
                    Film.Key k = ac.stateAt(sg.t1 - 0.5f);
                    String body = k.body == Pose.STAND ? "STAND" : k.body == Pose.SIT ? "SIT" + (k.seat >= 0 ? "/seat" + k.seat : "") : k.body == Pose.LIE ? "LIE" : k.body == Pose.KNEEL ? "KNEEL" : k.body == Pose.CROUCH ? "CROUCH" : "B" + k.body;
                    float xa = Director.xAt(ac, sg.t0 + 2.6f), xb = Director.xAt(ac, sg.t1 - 0.3f); out.append(ac.c.displayName).append(": ").append(body).append(Math.abs(xb - xa) > 40 ? String.format(Locale.US, " x%.0f→%.0f", xa, xb) : "").append(k.holdR != 0 ? " holdR=" + k.holdR : "").append(acts.isEmpty() ? "" : " " + acts).append("  ");
                }
                for (int w = 0; w < 18; w++) { float v = 0; try { v = film.weather(w, sg.t1 - 0.5f); } catch (Throwable t) { break; } if (v > 0.05f) out.append(wNames.containsKey(w) ? wNames.get(w) : "W" + w).append("=").append(String.format(Locale.US, "%.1f", v)).append(' '); }
                Set<String> fxs = new LinkedHashSet<String>(); for (Film.Fx f : sg.fx) if (f.t0 > sg.t0 + 2.5f) fxs.add(fxName(f.type)); if (!fxs.isEmpty()) out.append(" fx").append(fxs).append(' ');
                out.append("wind=").append(String.format(Locale.US, "%.1f", film.wind(sg.t1 - 0.5f)));
            }
            System.out.println(s + "\n    -> " + out);
        }
    }
}
