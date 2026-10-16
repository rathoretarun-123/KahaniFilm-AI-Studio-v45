# Picture folders for desktop film renders

Each folder is the assets directory MakeFilm takes: the pictures plus a `cast.txt` that says which picture is which
(`char|name|file`, `view|name|angle|file`, `shot||thing|file|object`, `scene|n|file`, …).

| Folder | Script | Pictures |
|---|---|---|
| `aakhiri_chai/` | `app/src/main/assets/aakhiri_chai_screenplay.txt` | the owner's pictures of आखिरी चाय (processed copies) |
| `babu_ke_liye/` | `app/src/main/assets/babu_ke_liye_screenplay.txt` | the owner's pictures of बाबू के लिए (processed copies) |
| `aakhiri_jodi/` | `tools/testdata/aakhiri_jodi/aakhiri_jodi_screenplay.txt` | the owner's 43 uploads for आखिरी जोड़ी, keyed and split |
| `aakhiri_chitthi/` | `tools/testdata/aakhiri_chitthi/aakhiri_chitthi_screenplay.txt` | stand-ins only: बाबू's old man and girl, mapped to रघुवीर जी and परी; the owner has not given pictures for this film yet |

Render one, from the repo root:

    java -cp <classes> com.tarun.kahani.jvm.MakeFilm <script> tools/testdata/render_assets/<folder> out.mp4 640 24 <stills dir>
