# KahaniFilm script format

Every script is written in this format, in Hindi or English. The app's story page shows it under "📝 Script format",
and the reader (ScriptParser.userFormat) reads both languages.

```
शीर्षक: फ़िल्म का नाम               Title: Name of the film

पात्र                               Characters
नाम (उम्र) (विवरण)                  Name (age) (description)

वस्तुएं                              Objects
वस्तु का नाम (विवरण)                Name (description)

स्थान और पृष्ठभूमि                   Places and Backgrounds
जगह का नाम (विवरण)                  Name (description)

कहानी                               Story

दृश्य 1                              Scene 1
(दृश्य का विवरण)                    (description of the scene)
नाम: "संवाद" (बोलने का भाव)          Name: "Dialogue" (how it is said)

समाप्त                              END
```

Rules
- Every description in ( ), every dialogue in " ". No other marks such as * / #.
- The studio asks for pictures only of the characters, objects and places listed before the story, in that order.
- Intangible things are never asked for: wind, water, sky, sunlight, rain, fire, shade, sounds, feelings.
- Camera words in a description are camera directions: wide shot, medium shot, close-up, from above, from below,
  the camera pulls back, the camera moves closer.
- The full guide in both languages: app/src/main/assets/help/script_format.txt.
