# Crochet Quest (Stage 2)

A cozy, offline-first Android game that grows alongside your real crochet hobby.
Built and tested entirely with GitHub Actions. No Android Studio needed.

## What's in the app

**Stage 1:** onboarding and cat naming, Home with level / XP / yarn balls, a pixel-art cat,
session logging, settings.

**Stage 2 (new):**
- **Projects:** add, edit and delete (with confirmation) real crochet projects. Only a name is
  required. Categories, optional target date, hook size, yarn notes, stitch notes, a progress
  slider, a progress history, and a finished summary. Planned / In progress / Completed.
- **Quests:** 11 optional quests in three groups (Cozy, Creative, Project Milestones) with
  Available / In progress / Ready to claim / Claimed states and an explicit **Claim reward** button.
- **Achievements:** 9 badges in the Collection tab, locked ones shown as muted silhouettes,
  unlocked ones with their unlock date.
- **Home:** a featured quest card and a summary of your ongoing projects.

Still to come in Stage 3: wardrobe and shop, cat accessories, more pixel art, polish, UI tests.

## Reward rules (tunable in `domain/game/RewardConfig.kt` and `domain/quests/QuestModels.kt`)

| Action | XP | Yarn balls |
|---|---:|---:|
| Log a crochet session (first 3 per day) | 10 | 5 |
| Record a stitch for the first time | 15 | 10 |
| Meaningful project progress update | 5 | 2 |
| Finish a project (once per project, ever) | 30 | 20 |
| Quests | see Quest Board | see Quest Board |

- **Meaningful update:** progress must pass your best earlier rewarded progress by 10 points.
  Sliding back and forth never earns twice.
- **Reopening a finished project** never pays the finish reward again.
- **Quests** are completed by real activity (a session, a project, a stitch). A completed quest
  must be claimed. A claim works exactly once. Daily quests reset each day, weekly quests each
  Monday, milestones are one-time. A reward you didn't claim yet stays claimable.
- Nothing is ever deducted for inactivity.

## 1. Put the project on GitHub

1. Sign in at github.com, then **New repository** (or reuse your existing one).
2. To update an existing repo, upload the new files over the old ones and commit.
   For a fresh repo, click **uploading an existing file**, and drag in **everything inside** the
   unzipped `CrochetQuest` folder (not the folder itself).
3. Check that the repo root shows `app`, `gradle`, `gradlew`, `build.gradle.kts` and a `.github`
   folder. **If `.github` is missing**, click **Add file, Create new file**, type
   `.github/workflows/android.yml`, paste in that file's contents, and commit.
4. Confirm `gradle/wrapper/gradle-wrapper.jar` is present.

## 2. Run the build

The workflow starts automatically on every push to `main` (or `master`). You can also start it by
hand: **Actions** tab, **Android CI**, **Run workflow**. It runs the unit tests first, then builds
the debug APK. If either fails, no APK is uploaded.

## 3. Download and install

1. **Actions** tab, open the latest run with a green check, scroll to **Artifacts**, and click
   **crochet-quest-debug-apk**. Unzip it to get `app-debug.apk`.
2. Copy `app-debug.apk` to your phone and tap it. Android 12 will ask you to allow installs from
   that app (Chrome, Files, etc.). Allow it, then tap **Install**.
3. If Play Protect warns about an unknown app, choose **Install anyway**.

### Updating from Stage 1: uninstall first

GitHub creates a fresh debug signing key for every build, so Android sees each build as a
different app and refuses to install it over the old one. **Uninstall the old Crochet Quest first**
(this clears its saved progress), then install the new APK. The database itself can upgrade in
place (a 1 to 2 migration is included), so this will only stop being a limitation once the project
uses a fixed signing key (a good Stage 3 task).

## If the build fails

Open the failed run, click the red step, and copy the error text. That is all that is needed to fix it.

## Known limitations (Stage 2)

- Wardrobe, shop and cat accessories arrive in Stage 3 (Collection shows a note for now).
- "Learn a new technique" is a self-reported quest ("I did this"), since the app can't observe it.
- Quest and board data refresh when you act, not at midnight while the app is already open.
- Uses the system default font. UI/database (instrumented) tests arrive in Stage 3. Stage 2 has
  84 JVM unit tests covering levels, rewards, project rules, quest rules and achievements.
