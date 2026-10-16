import com.tarun.kahani.core.*;
public class CamProbe { public static void main(String[] a) throws Exception {
  for (String p : a) {
    Story st = ScriptParser.parse(new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(p)), "UTF-8"));
    Director d = new Director(st, new Director.Options()); d.prepare();
    Film f = d.direct(new Art());
    System.out.println("== " + new java.io.File(p).getName() + " script shots=" + d.scriptShots + " duration=" + f.duration);
    for (String n : f.notes) if (n.contains("script's camera")) System.out.println(n);
  }
}}
