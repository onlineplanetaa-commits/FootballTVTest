# Football TV Test — cloud APK build

This project is prepared for building the Android TV APK in GitHub Actions. No Android Studio is required on your computer.

## Build without installing Android Studio

1. Create or sign in to a GitHub account.
2. Create a new empty repository, for example `FootballTVTest`.
3. Upload **all files and folders from this project ZIP** to the repository. Keep `.github/workflows/build-apk.yml` in exactly that path.
4. Open the repository's **Actions** tab.
5. Select **Build Football TV APK**.
6. Click **Run workflow**.
7. Wait for the green check mark.
8. Open that workflow run and find **Artifacts** at the bottom.
9. Download `FootballTV-debug-apk` and extract `app-debug.apk`.

The workflow installs Java 17, Android SDK 35, build-tools 35.0.0 and Gradle 8.7 automatically in GitHub's cloud runner.

## App

The current build is a test Android TV app with a football match list and a Media3 player. It uses a test video URL, not third-party sports streams.
