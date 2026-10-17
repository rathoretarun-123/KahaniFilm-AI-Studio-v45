# KahaniFilm script format

Every script is written in this format, in Hindi or English. The app's story page shows it under "📝 Script format",
and the reader (ScriptParser.userFormat) reads both languages.

```
शीर्षक: फ़िल्म का नाम               Title: Name of the film

पात्र                               Characters
1. नाम (उम्र)                       1. Name (age)
विवरण                               description

वस्तुएं                              Objects
1. वस्तु का नाम                     1. Name
विवरण                               description

स्थान                               Places
1. जगह का नाम                       1. Name
विवरण                               description

कहानी                               Story

दृश्य 1                              Scene 1
(दृश्य का विवरण)                    (description of the scene)
नाम: "संवाद" (बोलने का भाव)          Name: "Dialogue" (how it is said)

समाप्त                              END
```

Rules
- Every description in ( ), every dialogue in " ". No other marks such as * / #.
- The lists are numbered; the line under a name is its description. `नाम (उम्र) (विवरण)` on one line, unnumbered or
  numbered, reads the same. The user's own sample: tools/testdata/aakhiri_chitthi/aakhiri_chitthi_numbered.txt.
- The reader takes only what the script says: every name as written (a place keeps its whole name, "गांव का पुराना
  डाकघर - बाहरी हिस्सा"), each place once, and a scene is where its people are — a place seen behind or come from does
  not move it.
- The studio asks for pictures only of the characters, objects and places listed before the story, in that order.
- Intangible things are never asked for: wind, water, sky, sunlight, rain, fire, shade, sounds, feelings.
- Camera words in a description are camera directions: wide shot, medium shot, close-up, from above, from below,
  the camera pulls back, the camera moves closer.
- The full guide in both languages: app/src/main/assets/help/script_format.txt.
