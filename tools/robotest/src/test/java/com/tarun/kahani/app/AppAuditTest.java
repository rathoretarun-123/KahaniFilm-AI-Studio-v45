package com.tarun.kahani.app;

import static org.junit.Assert.*;

import java.io.File;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;

/**
 * v47.8 (the user: "debug everything, every functionality"): the project folder is written by two threads at once — the
 * film job (the voices it chose, the film's signature, the time it was made) and the screens (the user's own settings,
 * the script). A setting written by one must never wipe the others', and a file read while it is being written is
 * either the old text or the new one, never half of it.
 */
public final class AppAuditTest {
    static File tempDir() throws Exception {
        File d = Files.createTempDirectory("kahani-project").toFile();
        d.deleteOnExit();
        return d;
    }

    @Test public void settingsWrittenFromTwoThreadsAreAllKept() throws Exception {
        final Project p = new Project(tempDir());
        p.setSetting("name", "बाबू के लिए");
        final int n = 150;
        Thread[] ts = new Thread[2];
        for (int k = 0; k < ts.length; k++) {
            final int who = k;
            ts[k] = new Thread(new Runnable() {
                public void run() { for (int i = 0; i < n; i++) p.setSetting("t" + who + "." + i, String.valueOf(i)); }
            });
        }
        for (Thread t : ts) t.start();
        for (Thread t : ts) t.join();
        assertEquals("the project's name is kept", "बाबू के लिए", p.setting("name", "lost"));
        int missing = 0;
        for (int k = 0; k < ts.length; k++) for (int i = 0; i < n; i++) if (!String.valueOf(i).equals(p.setting("t" + k + "." + i, "lost"))) missing++;
        assertEquals("settings lost when two threads wrote at once", 0, missing);
    }

    /** The voices kept for the next film are read back as they were written (the WAV writer closes its file on failure now). */
    @Test public void aVoiceWrittenIsReadBackTheSame() throws Exception {
        File f = new File(tempDir(), "line.wav");
        float[] pcm = new float[20000];
        for (int i = 0; i < pcm.length; i++) pcm[i] = (float) Math.sin(i * 0.05) * 0.6f;
        AudioIO.writeRaw(f, pcm);
        float[] back = AudioIO.readRaw(f);
        assertNotNull(back);
        assertEquals(pcm.length, back.length);
        float worst = 0;
        for (int i = 0; i < pcm.length; i++) worst = Math.max(worst, Math.abs(pcm[i] - back[i]));
        assertTrue("16-bit round trip: " + worst, worst < 1e-3f);
        assertEquals("the file's own size", 44 + pcm.length * 2, f.length());
    }

    @Test public void aFileIsReadWholeWhileItIsBeingWritten() throws Exception {
        final Project p = new Project(tempDir());
        StringBuilder a = new StringBuilder(), b = new StringBuilder();
        for (int i = 0; i < 20000; i++) { a.append('क'); b.append('ख'); }
        final String one = a.toString(), two = b.toString();
        p.write("script.txt", one);
        final AtomicReference<String> torn = new AtomicReference<String>();
        final boolean[] stop = {false};
        Thread reader = new Thread(new Runnable() {
            public void run() {
                while (!stop[0] && torn.get() == null) {
                    String s = p.read("script.txt");
                    if (!s.equals(one) && !s.equals(two)) torn.set(s.length() + " characters");
                }
            }
        });
        reader.start();
        for (int i = 0; i < 300 && torn.get() == null; i++) p.write("script.txt", i % 2 == 0 ? two : one);
        stop[0] = true;
        reader.join();
        assertNull("the script was read half-written", torn.get());
        assertFalse("no temporary file is left in the project", new File(p.dir, "script.txt.tmp").exists());
    }
}
