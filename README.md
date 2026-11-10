# Church School Ministry – Android APK + iPhone/Android home-screen app

Repo layout (drop these into your `churchschool` GitHub repo, repo root):

```
index.html, manifest.webmanifest, sw.js, offline.html, .nojekyll, icons/   → the installable web app (PWA)
android/                                                                   → Android app source
.github/workflows/build-apk.yml                                            → builds the APK in the cloud
apps-script/doGet-snippet.gs                                               → one-line fix in your Apps Script
```

## 1. Apps Script (needed for the PWA/iPhone version)
Open `apps-script/doGet-snippet.gs`, copy `.setXFrameOptionsMode(...ALLOWALL)` into your `doGet`,
then Deploy → Manage deployments → Edit → New version → Deploy. Access must be "Anyone".

## 2. Publish the web app (iOS + Android install)
1. In the repo, delete the old `index.md` (if present) and upload the files above to the repo root.
2. Settings → Pages → deploy from the `main` branch, `/ (root)`.
3. Open https://noelmuthomi.github.io/churchschool/
   - iPhone: Safari → Share → Add to Home Screen.
   - Android Chrome: menu → Install app / Add to Home screen.

## 3. Build the Android APK (no Android Studio needed)
1. Upload the whole `android/` folder and `.github/` folder to the repo.
2. GitHub → Actions → "Build Android APK" → Run workflow.
3. After ~5 min, download `church-school.apk` from the run's Artifacts or from Releases
   (https://github.com/noelmuthomi/churchschool/releases/latest/download/church-school.apk).
4. On the phone: open the APK → allow "Install unknown apps" for your browser/Files app → Install.

### Stable signing key (do this once, so future updates install over old ones)
```
keytool -genkeypair -v -keystore release.jks -alias churchschool -keyalg RSA -keysize 2048 -validity 10000
base64 -w0 release.jks      # (macOS: base64 -i release.jks)
```
Add repo secrets: KEYSTORE_BASE64 (the base64 output), KEYSTORE_PASSWORD, KEY_ALIAS (`churchschool`), KEY_PASSWORD.
Keep release.jks somewhere safe and never commit it. Without these secrets the workflow builds a debug APK,
which installs fine but can't update over a previous build from a different runner.

## Customising
- App URL: `android/app/build.gradle.kts` (APP_URL) and `index.html` (APP_URL).
- Colours: `#1b2a5c` navy / `#d4a72c` gold in `index.html`, `manifest.webmanifest`, `res/values/colors.xml`.
- Icons: replace `icons/*.png` (192, 512, maskable 512, 180 apple-touch) and the Android
  `res/mipmap-*/ic_launcher.png` + `res/drawable-nodpi/ic_launcher_foreground.png` (432×432, artwork inside the centre ~60%).
- New release: bump `versionCode`/`versionName` in `app/build.gradle.kts`.
