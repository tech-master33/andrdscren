# Contributing to andrdscren

Thank you for contributing to andrdscren, the screen reader built for BAOSP.
This guide is written to work well with screen readers and keyboard-only navigation.
Every step is numbered and linear — no visual layout is assumed.

---

## Ways to contribute

You do not need to write code to contribute. Here are all the ways you can help:

1. Report a bug — describe something that does not work as expected
2. Request a feature — describe something that would help blind users
3. Improve documentation — fix unclear steps or add missing information
4. Test builds — install nightly APKs and report screen reader behaviour
5. Translate — help translate in-app strings to other languages
6. Write code — fix bugs or add features to the accessibility service

---

## Claiming an issue

If you want to work on an existing issue — especially one tagged `needs help` or `good first issue` — follow these steps first.

1. Open the issue you want to work on
2. Read the full description, including the "Where to start" section if there is one
3. Leave a comment saying you would like to work on it — for example: "I'd like to take this on"
4. Wait for a maintainer to reply — we will assign the issue to you and answer any questions
5. Once assigned, follow the steps in this guide to fork, branch, make your change, and open a pull request

This step matters because it avoids two people doing the same work at the same time.
If you have been assigned an issue and decide you cannot continue, leave a comment to let us know.
We will unassign it so someone else can pick it up.

---

## Before you start

You need:

1. A GitHub account — create one free at github.com/join
2. Git installed on your computer — download at git-scm.com
3. Android Studio or a text editor such as VS Code
4. Java 17 — download at adoptium.net
5. Android SDK — installed automatically by Android Studio

---

## Step 1 — Fork the repository

Forking makes a personal copy of the code under your own GitHub account.

1. Open github.com/tech-master33/andrdscren
2. Activate the Fork button near the top of the page
3. On the next screen, activate Create fork
4. GitHub takes you to your copy at github.com/YOUR-USERNAME/andrdscren

---

## Step 2 — Clone your fork to your computer

Open a terminal and run these commands one at a time.
Replace YOUR-USERNAME with your actual GitHub username.

```bash
git clone https://github.com/YOUR-USERNAME/andrdscren.git
cd andrdscren
git remote add upstream https://github.com/tech-master33/andrdscren.git
```

Running `git remote -v` should now show both `origin` (your fork) and `upstream` (the main repo).

---

## Step 3 — Create a branch for your change

Never commit directly to `main`. Create a new branch first.

```bash
git checkout -b your-branch-name
```

Name the branch something descriptive. Examples:

- `fix/tts-crash-on-startup`
- `feature/add-swipe-gestures`
- `docs/update-readme`
- `a11y/improve-focus-order`

---

## Step 4 — Make your changes

Open the project in Android Studio or your editor.
The project uses Kotlin. Key directories:

- `app/src/main/java/` — all Kotlin source code
- `app/src/main/res/` — XML resources, strings, and layouts
- `app/src/main/AndroidManifest.xml` — app permissions and service declarations

### Accessibility rules for this project

Every change must follow these rules:

1. All UI elements must have a content description so the screen reader can announce them
2. Do not use color alone to communicate information — also use text or shape
3. Focus order must be logical when navigating by swipe or keyboard
4. Touch targets must be at least 48dp wide and 48dp tall
5. All strings shown to the user must be in `strings.xml` so they can be translated
6. Test every change with the screen reader turned on before submitting

---

## Step 5 — Build and test locally

```bash
chmod +x gradlew
./gradlew assembleDebug
```

The APK will be at:

```
app/build/outputs/apk/debug/app-debug.apk
```

To install it on a connected Android device:

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Manual testing checklist

Go through each item before submitting your pull request:

- The app builds without errors or warnings
- The screen reader announces every button and label correctly
- All interactive elements are reachable by swiping through the screen
- No element is skipped or announced in a confusing order
- Rotating the screen does not crash the app or lose focus
- The accessibility service starts correctly after enabling it in Settings

---

## Step 6 — Commit your changes

Write a commit message that clearly explains what changed and why.

```bash
git add .
git commit -m "fix: announce button state changes to screen reader

Previously the toggle buttons did not announce their new state when
activated. Added StateDescription to fix this."
```

Commit message format:

```
type: short summary in plain English

Longer explanation if needed. Explain the problem, not just the fix.
```

Types: `fix`, `feature`, `docs`, `refactor`, `a11y`, `test`

---

## Step 7 — Push and open a pull request

```bash
git push origin your-branch-name
```

Then:

1. Open github.com/YOUR-USERNAME/andrdscren
2. GitHub shows a bar saying your branch was recently pushed
3. Activate Compare and pull request
4. Fill in the title: one sentence describing the change
5. Fill in the description: what problem does this solve, how did you test it
6. Activate Create pull request

---

## Reporting a bug or requesting a feature

You do not need to know how to code to do this. It is one of the most valuable contributions.

1. Open github.com/tech-master33/andrdscren/issues
2. Activate New issue
3. Choose Bug report or Feature request
4. Fill in the title with one short sentence describing the problem or request
5. In the body, include:
   - What you were trying to do
   - What happened instead of what you expected
   - Whether it happens every time or only sometimes
   - Your Android version and device model
   - Whether the problem also happens with TalkBack or is specific to andrdscren

---

## Code review process

After you open a pull request:

1. A maintainer will read your changes and may ask questions in the comments
2. Reply to comments — activate the Resolve conversation button once you have addressed the point
3. If changes are requested, push new commits to the same branch — the pull request updates automatically
4. Once approved, a maintainer will merge your pull request

Most pull requests receive a first response within a few days.
If you have not heard back after a week, add a comment to the pull request to ask for an update.

---

## Community and questions

- Discussions: github.com/tech-master33/andrdscren/discussions
- Issues: github.com/tech-master33/andrdscren/issues
- TTS engine: github.com/tech-master33/aotts
- Launcher: github.com/tech-master33/aoler
- BAOSP main project: github.com/tech-master33/baosp

Open a discussion if you have a question. Describe what you are trying to do and where you are stuck.
You do not need to have a solution — questions about how things work are welcome.
