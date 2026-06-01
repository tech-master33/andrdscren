# andrdscren Roadmap

This document describes what we plan to build next, why each item matters,
and what state each one is in. It is updated as things change.

If you want to work on something listed here, open an issue or discussion first
so we can coordinate and avoid duplicated effort.

If something important to you is missing, open a feature request at
github.com/tech-master33/andrdscren/issues — the issue templates will ask you
the right questions.

---

## How to read this document

Each item has a status:

- **Planned** — we intend to build this but have not started
- **In progress** — actively being worked on
- **Needs help** — no one is currently assigned, good place to contribute
- **Done** — shipped and in the nightly build

Items are roughly ordered by priority within each section.

---

## Navigation

### Adjustable speech rate shortcut — Needs help

**What it is:** A quick way to change how fast the screen reader speaks
without going into the system settings menu.

**Why it matters:** The only way to change speech rate right now is to navigate
to Settings → Accessibility → Text-to-speech output → Speech rate, which takes
many swipes. Users who want faster speech for reading long documents and slower
speech for unfamiliar apps have no quick way to switch.

**Proposed approach:** Hold volume-up and volume-down at the same time to cycle
through three preset rates — slow (0.7x), normal (1.0x), and fast (1.5x).
The current rate should be announced after switching.

**Where to start:** The `AccessibilityService` key event handler in the main service class.

---

### Swipe gesture customisation — Planned

**What it is:** Let users reassign what each swipe direction does.

**Why it matters:** The default gesture mappings work well for many people
but not everyone. A user with limited finger mobility may need to swap
two-finger and three-finger gestures. A user who reads a lot may want
a swipe to jump by paragraph instead of by element.

**Proposed approach:** A settings screen listing each gesture and a dropdown
to choose its action. Settings stored in SharedPreferences and read by the
AccessibilityService at startup.

---

### Jump navigation by element type — Planned

**What it is:** A gesture that jumps to the next element of a specific type —
next heading, next button, next link, next form field — rather than moving
through every element in order.

**Why it matters:** On a long page or complex screen, navigating element by element
to find a specific control is slow. Jumping by type lets experienced screen reader
users move through a screen the way sighted users scan with their eyes.

**Proposed approach:** A two-finger swipe down cycles through element types.
A two-finger swipe right jumps to the next element of the currently selected type.
Announce the selected type after switching ("Navigating by headings").

---

### Reading cursor — continuous reading — Planned

**What it is:** A "read from here" command that reads every element on screen
from the current focus point to the end, without stopping.

**Why it matters:** Reading a long page by swiping element by element is slow and tiring.
Continuous reading lets users listen hands-free.

**Proposed approach:** A three-finger tap starts continuous reading from the focused element.
Any touch or swipe stops it. Each element is focused in turn so the user can resume from
wherever they stopped.

---

### Keyboard and switch access compatibility — Needs help

**What it is:** Ensure andrdscren works correctly alongside Android's Switch Access
service and with Bluetooth keyboards.

**Why it matters:** Some blind users also have motor disabilities and cannot use a
touchscreen. They use switch devices or keyboards. The screen reader must not intercept
key events that Switch Access needs, and must announce focus changes triggered by
keyboard navigation.

**Where to start:** Test andrdscren with Switch Access enabled simultaneously.
File any focus announcement or key event conflicts as issues.

---

## Announcements

### Notification announcements — In progress

**What it is:** When a new notification arrives, the screen reader announces it aloud
without the user having to open the notification shade.

**Why it matters:** Blind users cannot see notification badges or the visual notification
shade. They discover notifications only when they deliberately navigate there.
Spoken announcements make notification delivery immediate, the same as for sighted users.

**Current state:** Basic notification listener is implemented. Work is ongoing to make
announcements interruptible, respect Do Not Disturb mode, and allow per-app configuration.

---

### Punctuation verbosity control — Needs help

**What it is:** Let users choose how much punctuation the screen reader reads aloud.

**Why it matters:** By default the screen reader reads all punctuation — "comma",
"period", "exclamation mark" — which slows down reading significantly.
Experienced screen reader users typically want punctuation read only when navigating
character by character, not when reading full sentences.

**Proposed approach:** A setting with three levels — None, Some (sentence-ending only),
All. Implemented in the text processing layer before the TTS call.

---

### Custom announcements per app — Planned

**What it is:** Let users write simple rules that change what the screen reader
announces in a specific app.

**Why it matters:** Some apps have poor accessibility labels — announcing "image" or
"button" with no other information. Users who know what a specific unlabelled button
does should be able to give it a better label without waiting for the app developer to
fix it.

**Proposed approach:** A rules file in SharedPreferences — each rule maps a resource ID
and package name to a replacement content description. Applied in the AccessibilityEvent
handler before the TTS call.

---

### Reading mode — Planned

**What it is:** A mode optimised for reading long-form text — articles, emails,
documents — where the screen reader announces the current sentence rather than
the current focusable element.

**Why it matters:** In standard navigation mode, a long article is split into many
small elements (each link, each heading, each paragraph). Reading mode skips structural
navigation and reads continuous text, the way a human would read aloud.

---

## Braille support

### Braille display — read mode — Planned

**What it is:** Connect a Bluetooth Braille display and read the currently focused
element's text on it in real time.

**Why it matters:** Some blind users are Deafblind — they cannot use audio.
For them, a Braille display is the only way to read a phone screen.
Without this, BAOSP is not usable at all for Deafblind people.

**Proposed approach:** Use the Android Bluetooth HID profile to communicate
with BrailleBack-compatible displays. Send the focused element's text as Grade 1
Braille. Start with read-only mode before adding Braille keyboard input.

**Dependencies:** Requires Android 12 or above for the BT HID APIs.
This is a significant feature — if you want to lead this work, open a discussion first.

---

### Braille display — input mode — Long term

**What it is:** Type using the Braille keyboard on a connected display and have the
input sent to the focused text field.

**Why it matters:** Braille keyboard input is the preferred text entry method for
many Deafblind and experienced Braille users. It is significantly faster than using
the on-screen keyboard by touch.

**Status:** Not started. Depends on Braille display read mode being completed first.

---

## Language support

### Right-to-left layout support — Needs help

**What it is:** Ensure the screen reader works correctly in apps that use right-to-left
(RTL) text layout — Arabic, Hebrew, Farsi, and others.

**Why it matters:** Swipe navigation in andrdscren currently follows left-to-right
reading order. In an RTL app, this means the user moves through elements in the wrong
direction. RTL users get a broken experience.

**Proposed approach:** Detect the app's layout direction from its `WindowInfo`.
Reverse the swipe direction mapping when the active window uses RTL layout.

---

### Multilingual content detection — Planned

**What it is:** When the focused element contains text in a language different from
the system language, switch the TTS engine to the matching voice automatically.

**Why it matters:** If a user's phone is set to English but they receive a message
in Spanish, the English voice reads the Spanish words with English pronunciation —
which is often incomprehensible. Auto-switching the voice makes multilingual content
understandable.

**Dependencies:** Requires aotts (or another TTS engine) to have the needed language
voices installed. Uses Android's `TextClassifier` or a lightweight language detection
library to identify the language before sending text to TTS.

---

## How priorities are set

Items move up the list when:

1. More users report being blocked by the missing feature
2. A contributor volunteers to lead the work
3. A dependency (another item on this list) is completed

Items are not added to this roadmap just because they are technically interesting.
Every item here has a stated impact on blind or disabled users. If you propose a feature,
the most important thing you can say is who it helps and how.
