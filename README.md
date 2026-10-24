# Crochet Quest

A cozy, offline-first Android game that grows alongside your real crochet hobby. A pixel-art cat
companion, optional quests, achievements, a project tracker and a little wardrobe of accessories,
all stored privately on your phone. Built, tested and packaged entirely with GitHub Actions.
**No Android Studio needed.**

## What's in the app

- **Home:** level, XP bar and yarn balance, your cat (tap it to open the wardrobe), a featured
  quest card, your ongoing projects, and one-tap **Log Crochet Session**.
- **Quests:** 11 optional quests (Cozy, Creative, Project Milestones) with explicit **Claim reward**
  buttons. No deadlines, no penalties, nothing is ever lost for being away.
- **Projects:** add, edit and delete (with confirmation) real crochet projects. Only a name is
  required. Category, optional target date, hook size, yarn and stitch notes, a progress slider
  with history, and a finished summary.
- **Collection:** 9 achievements (locked ones are muted silhouettes) and your accessory inventory.
- **Wardrobe:** 7 accessories to buy with yarn balls. Preview, buy, equip, take off. One item per
  slot (head, face, neck, body), and the cat changes immediately.
- **Settings:** rename your cat, gentle animations on/off, reset game progress (with confirmation),
  privacy note, version.
- A short level-up celebration, an idle bob and blink (both off when you turn animations off or
  Android's "remove animations" is on), screen-reader descriptions, and layouts that scroll for
  large fonts and small screens.

## Reward rules

Tunable in `domain/game/RewardConfig.kt` and `domain/quests/QuestModels.kt`.

| Action | XP | Yarn balls |
|---|---:|---:|
| Log a crochet session (first 3 per day) | 10 | 5 |
| Record a stitch for the first time | 15 | 10 |
| Meaningful project progress update | 5 | 2 |
| Finish a project (once per project, ever) | 30 | 20 |
| Quests | see Quest Board | see Quest Board |

- Start with 10 yarn balls. The cheapest accessory (bell collar, 15) is one session away.
- **Meaningful update:** progress must pass your best earlier rewarded progress by 10 points.
  Sliding back and forth never earns twice.
- **Reopening a finished project** never pays the finish reward again.
- **Claiming a quest** works exactly once, even on double taps. An unclaimed reward stays claimable.
- **Buying an accessory** can never charge twice, and spending never lowers XP or your level.
- Levels: 100 / 250 / 450 / 700 / 1000 XP and onward.

## 1. Put the project on GitHub

1. Sign in at github.com, then **New repository** (or reuse your existing one). A **private**
   repository is recommended.
2. Upload the files. For an existing repo, upload over the old files and commit. For a fresh repo,
   click **uploading an existing file** and drag in **everything inside** the unzipped
   `CrochetQuest` folder (not the folder itself).
3. Check that the repo root shows `app`, `gradle`, `gradlew`, `build.gradle.kts`, `licenses` and a
   `.github` folder. **If `.github` is missing** (some computers hide dot-folders), use **Add file,
   Create new file**, type `.github/workflows/android.yml`, paste in that file's contents and commit.
   Do the same for `create-signing-key.yml` and `instrumented-tests.yml` if you want them.
4. Confirm `gradle/wrapper/gradle-wrapper.jar` is present.

## 2. Build the APK

The **Android CI** workflow runs automatically on every push to `main` (or `master`). You can also
start it: **Actions** tab, **Android CI**, **Run workflow**. It runs the unit tests first, then
builds the debug APK. If either fails, no APK is uploaded.

## 3. Download and install

1. **Actions** tab, open the latest run with a green check, scroll to **Artifacts**, click
   **crochet-quest-debug-apk**, and unzip it to get `app-debug.apk`.
2. Copy `app-debug.apk` to your Android phone and tap it. Android 12 asks you to allow installs
   from that app (Chrome, Files, and so on). Allow it, then tap **Install**.
3. If Play Protect warns about an unknown app, choose **Install anyway**.

This is a **debug** build, fine for personal use and testing.

## Keep your progress between updates (optional, recommended)

By default GitHub creates a brand-new debug signing key for every build, so Android sees each build
as a different app and won't install it over the old one. You would have to uninstall first, which
erases your progress. To avoid that, create **one** permanent key and reuse it:

1. Make sure the repository is **private**.
2. **Actions** tab, **Create signing key (one-time, optional)**, **Run workflow**.
3. Open the finished run and download the **signing-secrets** artifact. Unzip it to get
   `signing-secrets.txt` with four lines: `NAME=value`.
4. **Settings, Secrets and variables, Actions, New repository secret.** Create four secrets, using
   each `NAME` and its value exactly: `SIGNING_KEYSTORE_BASE64`, `SIGNING_STORE_PASSWORD`,
   `SIGNING_KEY_ALIAS`, `SIGNING_KEY_PASSWORD`.
5. Delete the **signing-secrets** artifact from the run page, and keep a safe copy of the text file.
   If you lose the key, future builds won't install over old ones.
6. Run **Android CI** again. From this build onward, every new build installs over the previous one
   and keeps your data. (Switching from an unsigned-key build to this key once still needs an
   uninstall, because the key itself changed.)

The database also upgrades safely between app versions (migrations 1-2 and 2-3 are included).

## UI tests on an emulator (optional)

The **Instrumented tests** workflow boots an Android emulator and runs five end-to-end flows:
onboarding, adding a project, completing and claiming a quest, buying and equipping an accessory,
and keeping progress after the app is recreated and reopened. Start it from the **Actions** tab. It
is manual and slower, so it never holds up your APK builds. The 99 fast unit tests (levels, rewards,
projects, quests, achievements, shop, reset) run on every build.

## If a build fails

Open the failed run, click the red step, and copy the error text. That is all that is needed to fix it.
The first run downloads many dependencies and takes several minutes.

## Implemented features, and known limitations

All Stage 1 to 3 features above are implemented. Honest notes:

- **No sound effects.** The spec made the sound toggle conditional on having sounds, so there is no
  toggle. Animations have a toggle.
- **Quests refresh when you act**, not at midnight while the app sits open.
- **"Learn a new technique"** is self-reported ("I did this"), since the app can't observe it.
- **The art is original, bundled and small:** the cat and accessories are drawn in code as
  pixel-art layers (crisp, no image files). The font is Nunito (SIL Open Font License, see
  `licenses/`).
- Only a debug build is produced. A Play Store release would need a properly signed release build.
- Photos, export and backup are not included. Data lives only on the device (backups are disabled).
