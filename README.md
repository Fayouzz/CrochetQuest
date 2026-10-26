# Crochet Quest

A cozy, offline-first Android game that grows alongside your real crochet hobby. A pixel-art cat
companion, optional quests, achievements, a project tracker, a yarn stash, a decorate-able room and
a wardrobe, all stored privately on your phone. Built, tested and packaged entirely with GitHub
Actions. **No Android Studio needed.**

## What's in the app

- **Home:** level, XP and yarn balance. **Pet your cat** (tap it for hearts and a purr, no rewards,
  just comfort). It naps at night and does a happy hop when you log a session or finish a project.
  Log a session by hand (15, 30 or 60 minutes) or use the **real session timer**, which keeps running
  even if you close the app.
- **Quests:** 11 optional quests with explicit **Claim reward** buttons. No deadlines, no penalties.
  Daily and weekly quests roll over at midnight even while the app is open.
- **Projects:** add, edit, delete (with confirmation), progress slider and history, finished summary,
  and a **share card** for finished projects.
- **Yarn stash:** track yarn colors and how many skeins you have, and link colors to projects.
- **Room:** a cozy pixel room with a rug, lamp, window, plants and shelves to buy with yarn balls.
  Items unlock as you level, and free decorations (bunting, a picture, star lights) simply appear.
  The window shows stars at night and the lamps glow.
- **Collection:** a **trophy shelf** where each finished project becomes a pixel keepsake, 9
  achievements, and your accessory inventory.
- **Wardrobe:** 7 accessories for the cat.
- **Stats:** gentle totals (time crocheted, projects finished this year) and a **Year in yarn** recap.
- **Settings:** rename your cat, **sounds and haptics** toggle, gentle animations toggle, **theme**
  (match my phone / light / cozy night), **backup and restore** (JSON or CSV), reset progress.

## Reward rules

Tunable in `domain/game/RewardConfig.kt` and `domain/quests/QuestModels.kt`.

| Action | XP | Yarn balls |
|---|---:|---:|
| Log a crochet session (first 3 per day) | 10 | 5 |
| Record a stitch for the first time | 15 | 10 |
| Meaningful project progress update | 5 | 2 |
| Finish a project (once per project, ever) | 30 | 20 |
| Quests | see Quest Board | see Quest Board |

Petting the cat, the timer, the stash, the stats and sharing never give rewards on their own. Nothing
is ever deducted for inactivity.

## 1. Put the project on GitHub

1. Sign in at github.com, then **New repository** (or reuse your existing one). A **private**
   repository is recommended.
2. Upload the files over the old ones (or, for a fresh repo, drag in **everything inside** the
   unzipped `CrochetQuest` folder, not the folder itself) and commit.
3. Check the repo root shows `app`, `gradle`, `gradlew`, `build.gradle.kts`, `licenses` and a
   `.github` folder. **If `.github` is missing**, use **Add file, Create new file**, type
   `.github/workflows/android.yml`, paste in that file's contents and commit. Do the same for the
   other two workflow files if you want them.
4. Confirm `gradle/wrapper/gradle-wrapper.jar` is present.

## 2. Build the APK

The **Android CI** workflow runs on every push to `main` (or `master`), or start it from the
**Actions** tab, **Android CI**, **Run workflow**. It runs the unit tests first, then builds the
debug APK. If either fails, no APK is uploaded.

## 3. Download and install

1. **Actions** tab, open the latest green run, scroll to **Artifacts**, click
   **crochet-quest-debug-apk** and unzip it to get `app-debug.apk`.
2. Copy it to your Android phone and tap it. Allow installs from that app when asked, then **Install**.
   If Play Protect warns about an unknown app, choose **Install anyway**.

This is a **debug** build, fine for personal use.

### Updating

Updating keeps your progress **only if every build is signed with the same key**. Set that up once
(next section). Without it, GitHub signs each build with a new key, and Android refuses to install
over the old one, so you would have to uninstall first. **Before uninstalling, use Settings, Backup
and restore, Save JSON** and keep the file safe. After reinstalling, **Restore from a backup file**
gets everything back.

## Keep your progress between updates (optional, recommended)

1. Make sure the repository is **private**.
2. **Actions** tab, **Create signing key (one-time, optional)**, **Run workflow**.
3. Download the **signing-secrets** artifact from that run and unzip it to get `signing-secrets.txt`
   (four lines of `NAME=value`).
4. **Settings, Secrets and variables, Actions, New repository secret.** Create four secrets, each
   `NAME` with its value: `SIGNING_KEYSTORE_BASE64`, `SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS`,
   `SIGNING_KEY_PASSWORD`.
5. Delete the artifact from the run page and keep a safe copy of the text file. If you lose the key,
   future builds won't install over old ones.
6. Run **Android CI** again. From that build on, new builds install over the previous one. (The
   very first switch to this key still needs one uninstall, so **back up first**.)

The database upgrades safely between versions (migrations 1 to 4 are included).

## Backup and restore

**Settings, Backup and restore.**

- **Save JSON** is the complete save. **Save CSV** holds the same data in a spreadsheet-friendly
  file (one file, with a section per table). Both restore to exactly the same game.
- You pick where the file goes with Android's file picker, so the app needs no storage permission.
- **Restore from a backup file** reads and checks the file first, shows what's inside, and only then
  asks you to confirm. Restoring **replaces everything** in the app. A damaged or wrong file is
  refused with a message and changes nothing.

## UI tests on an emulator (optional)

The **Instrumented tests** workflow runs nine end-to-end flows on an emulator (onboarding, adding a
project, claiming a quest, buying and equipping, restart persistence, the timer, the stash, settings
and the room). It is manual and slower, so it never holds up your APK builds. The 150+ fast unit tests
run on every build.

## If a build fails

Open the failed run, click the red step and copy the error text. That is all that is needed to fix it.
The first run downloads many dependencies and takes several minutes.

## Notes and known limitations

- **Sounds are synthesized** (a soft purr, a click and a chime) and bundled as tiny files. They are
  gentle by design; say if you'd like them tuned. Everything is silent when the toggle is off.
- **Haptics** use Android's standard light feedback and follow the same toggle.
- **Quests refresh at midnight, 7 am and 8 pm** (the ticker sleeps in between, there is no polling).
  The cat naps from 8 pm to 7 am.
- **The room window and lamps** follow the clock, not the theme. The theme is your choice.
- "Learn a new technique" is self-reported ("I did this"), since the app can't observe it.
- **Share cards** are drawn on the phone and given to Android's share sheet. Nothing is uploaded.
- Fonts: Nunito (SIL Open Font License, see `licenses/`).
- Only a debug build is produced. A Play Store release would need a properly signed release build.
- Project photos are not included.
