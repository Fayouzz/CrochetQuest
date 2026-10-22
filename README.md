# Crochet Quest (Stage 1)

A cozy, offline-first Android game that grows alongside your real crochet hobby.
Built and tested entirely with GitHub Actions. No Android Studio needed.

**Stage 1 includes:** onboarding and cat naming, Home screen with level / XP / yarn balls,
a pixel-art cat, "Log Crochet Session" with rewards, optional "new stitch" bonus, settings
(rename cat, gentle-animations toggle, privacy, version), and the four-tab navigation shell.
Quests, Projects and Collection are friendly "coming soon" screens until Stages 2 and 3.

## Reward rules (tunable in `domain/game/RewardConfig.kt`)

| Action | XP | Yarn balls |
|---|---:|---:|
| Log a crochet session (first 3 per day) | 10 | 5 |
| Record a stitch for the first time | 15 | 10 |

Extra sessions in a day are still saved, they just do not earn more. You start with 10 yarn balls.
Levels: 100 / 250 / 450 / 700 / 1000 XP, and so on. Nothing is ever deducted for inactivity.

## 1. Put the project on GitHub

1. Sign in at github.com, then **New repository**. Name it `crochet-quest`. Leave it empty
   (no README, no .gitignore). Click **Create repository**.
2. Click **uploading an existing file**, and drag in **everything inside** the unzipped
   `CrochetQuest` folder (not the folder itself). Commit to the `main` branch.
3. Check that the repo root shows `app`, `gradle`, `gradlew`, `build.gradle.kts` and a
   `.github` folder. **If `.github` is missing** (some computers hide dot-folders when dragging):
   click **Add file, Create new file**, type `.github/workflows/android.yml` as the name,
   paste the contents of that file, and commit.
4. Confirm `gradle/wrapper/gradle-wrapper.jar` is present. The build cannot run without it.

## 2. Run the build

- The workflow starts automatically on every push to `main` (or `master`).
- Or run it by hand: **Actions** tab, **Android CI**, **Run workflow**.
- If the Actions tab asks you to enable workflows, click the green button.

The workflow runs the unit tests first, then builds the debug APK. If either fails, no APK is
uploaded.

## 3. Download the APK

1. **Actions** tab, then click the latest run with a green check.
2. Scroll to **Artifacts** at the bottom and click **crochet-quest-debug-apk**.
   GitHub gives you a `.zip`. Unzip it to get `app-debug.apk`.
3. Artifacts are kept for 14 days. Re-run the workflow for a fresh one.

## 4. Install on an Android 12 phone

1. Get `app-debug.apk` onto the phone (download it there, or send it via Drive / USB / email).
2. Tap the file. Android 12 will ask you to allow installs from that app (Chrome, Files, etc.).
   Switch on **Allow from this source** and go back.
3. Tap **Install**. If Play Protect warns about an unknown app, choose **Install anyway**.
   This is normal for a debug build that did not come from the Play Store.

This is a **debug** build, fine for testing and personal use. A broader release would need a
properly signed release build (not included yet).

## If the build fails

Open the failed run, click the red step, and copy the error text. That is all that is needed to fix it.
The first run downloads many dependencies and takes several minutes.

## Known limitations (Stage 1)

- Quests, Projects and Collection tabs are placeholders until Stages 2 and 3.
- The cat has no accessories yet (wardrobe arrives in Stage 3).
- Uses the system default font; a rounded font is a possible Stage 3 polish item.
- UI and database tests (instrumented) arrive in Stage 3. Stage 1 has JVM unit tests for the
  level curve and all reward, dedup and spending rules.
