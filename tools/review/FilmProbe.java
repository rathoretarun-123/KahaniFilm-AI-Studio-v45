import com.tarun.kahani.core.*;
import java.util.*;
public class FilmProbe { public static void main(String[] a) throws Exception {
  String t = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(a[0])), "UTF-8");
  Story st = ScriptParser.parse(t);
  Director.Options o = new Director.Options();
  Director d = new Director(st, o); d.prepare();
  Film f = d.direct(new Art());
  String[] bn = {"STAND","SIT","LIE","KNEEL","CROUCH","HANG","B6","B7"};
  for (Film.Seg sg : f.segs) {
    if (sg.type != Film.S_SCENE) continue;
    StringBuilder b = new StringBuilder(String.format(Locale.US, "seg %.1f-%.1f set=%d: ", sg.t0, sg.t1, sg.set));
    for (Film.Actor ac : sg.actors) {
      b.append("\n   ").append(ac.c.displayName).append(": ");
      String last = "";
      for (float x = sg.t0; x < sg.t1; x += 0.5f) {
        Film.Key k = ac.stateAt(x);
        String s = (k.visible ? (k.body < bn.length ? bn[k.body] : "B" + k.body) + (k.anchor != Film.A_GROUND ? "@a" + k.anchor : "") + (k.seat >= 0 ? "/s" + k.seat : "") : "-") ;
        if (!s.equals(last)) { b.append(String.format(Locale.US, "%.1f:%s ", x, s)); last = s; }
      }
      StringBuilder acts = new StringBuilder();
      for (Film.Act ac2 : ac.acts) if (ac2.t0 >= sg.t0 && ac2.t0 < sg.t1) acts.append(String.format(Locale.US, "%.1f:%d ", ac2.t0, ac2.type));
      b.append("\n      acts ").append(acts);
    }
    System.out.println(b);
  }
  for (String n : f.notes) if (n.contains("↳") || n.contains("seat") || n.contains("bench")) System.out.println(n);
}}
