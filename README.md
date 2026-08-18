# Lie Detector AI — Android Test v0.2

This is a UI/logic prototype for an Android app that estimates how suspicious a
text statement or a short video response sounds.

## Current demo
- Text analysis screen (heuristic deception-probability score + reasons)
- **Video analysis screen**: record or upload a short clip; the app extracts
  frames, runs Google ML Kit Face Detection (real, on-device face landmarks —
  eye openness, smiling probability, head pose), and combines those signals
  into a heuristic score. Result shown as a big **red "LIE"** or
  **green "TRUE"** card with a suspicion percentage and reasons.
- History placeholder
- English UI

## Important — please read
Both analyzers are **heuristics, not scientifically validated lie detectors**.
There is no reliable way — for a phone app or for a human expert — to determine
whether someone is lying purely from facial expressions, eye contact, or body
language. The video feature uses a *real* face-detection model (ML Kit), but
turning its output into a LIE/TRUE verdict is still a novelty heuristic. Please
don't use this app to make real decisions about someone's honesty, trust, or a
relationship — treat it as a demo/entertainment feature only. The in-app UI
repeats this warning on the video tab and on every result.

## Build
Open this folder in Android Studio and let Gradle sync. Then run the `app` configuration on an Android emulator/device.

Next production steps:
1. Add OCR for screenshots.
2. Add a real LLM backend for semantic analysis.
3. Analyze full conversations and contradictions.
4. Add Georgian-language analysis.
5. Add secure history and privacy controls.

## Build an installable APK via GitHub (no local Android Studio needed)

This repo includes a GitHub Actions workflow (`.github/workflows/build.yml`) that
automatically builds a debug APK every time you push to `main`, or whenever you
trigger it manually.

1. Create a new **public or private** repository on GitHub.
2. Upload/push all the files in this project to that repository (keep the folder
   structure exactly as-is — `app/`, `gradle/`, `gradlew`, etc. all need to stay
   where they are).
3. Go to the repository's **Actions** tab. If prompted, click **"I understand my
   workflows, go ahead and enable them"**.
4. Push a commit to `main` (or open the **Actions** tab → select **Build APK** →
   click **Run workflow**) to trigger a build.
5. Wait for the run to finish (a few minutes). Open the completed run, scroll to
   **Artifacts**, and download **lie-detector-debug-apk** — this is a zip
   containing `app-debug.apk`.
6. Transfer `app-debug.apk` to your Android phone (via a cloud drive, USB, email,
   etc.), open it, and allow "install unknown apps" for that app when prompted.
   The app installs directly since this is a debug build (unsigned/self-signed,
   fine for personal testing — not for the Play Store).

### Uploading without git (if you don't want to use git commands)
On the repository's GitHub page: **Add file → Upload files**, drag the entire
project folder contents in (make sure hidden folders like `.github` come along —
GitHub's web uploader does support this if you drag the folder itself in a
modern browser). Commit directly to `main`.

