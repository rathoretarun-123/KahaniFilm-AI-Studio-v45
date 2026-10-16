import com.tarun.kahani.core.*;
import java.util.*;
import java.nio.file.*;
/** Directs every story in the list (one per line) and prints a digest of its staging: posture every 0.5 s, acts, notes. */
public class BatchProbe {
  static String dump(String path) throws Exception {
    String t = new String(Files.readAllBytes(Paths.get(path)), "UTF-8");
    Story st = ScriptParser.parse(t);
    Director d = new Director(st, new Director.Options()); d.prepare();
    Film f = d.direct(new Art());
    StringBuilder b = new StringBuilder();
    for (Film.Seg sg : f.segs) {
      if (sg.type != Film.S_SCENE) continue;
      b.append(String.format(Locale.US, "seg %.1f-%.1f set=%d\n", sg.t0, sg.t1, sg.set));
      for (Film.Actor ac : sg.actors) {
        b.append(ac.c.displayName).append(':');
        String last = "";
        for (float x = sg.t0; x < sg.t1; x += 0.5f) {
          Film.Key k = ac.stateAt(x);
          String s = k.visible ? k.body + "@" + k.anchor + "/" + k.seat + (k.holdR != 0 ? "h" + k.holdR : "") : "-";
          if (!s.equals(last)) { b.append(String.format(Locale.US, " %.1f:%s", x, s)); last = s; }
        }
        b.append(" acts");
        for (Film.Act a : ac.acts) if (a.t0 >= sg.t0 && a.t0 < sg.t1) b.append(String.format(Locale.US, " %.1f:%d", a.t0, a.type));
        b.append('\n');
      }
      for (Film.Fx x : sg.fx) b.append(String.format(Locale.US, "fx %d %.1f %.1f\n", x.type, x.t0, Math.min(x.t1, 99999f)));
    }
    return b.toString();
  }
  public static void main(String[] a) throws Exception {
    java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
    for (String p : Files.readAllLines(Paths.get(a[0]))) {
      if (p.trim().isEmpty()) continue;
      String h;
      try { byte[] dg = md.digest(dump(p).getBytes("UTF-8")); StringBuilder sb = new StringBuilder(); for (byte x : dg) sb.append(String.format("%02x", x)); h = sb.toString(); }
      catch (Throwable e) { h = "ERR " + e; }
      System.out.println(p + "\t" + h);
    }
  }
}
