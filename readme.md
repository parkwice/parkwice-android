# 📱 Parkwise Android - CLI Workflow Cheatsheet

Since this project is being developed in VS Code without Android Studio, all building, running, and debugging is handled via the Gradle Wrapper (`./gradlew`) and Android Debug Bridge (`adb`).

**Note:** Run all `./gradlew` commands from the root directory of this project.

---

## 🧹 1. Cleaning & Maintenance
*Use these when the project acts weird, after changing branches, or to free up space.*

| Command | Description |
|---------|-------------|
| `./gradlew clean` | Deletes the `build/` directories. Fixes most weird caching issues. |
| `./gradlew --stop` | Stops the background Gradle daemon if it gets stuck or consumes too much RAM. |
| `rm -rf ~/.gradle/caches/` | (Mac/Linux) Nukes the global Gradle cache. Use only as a last resort for severe sync issues. |

---

## 🛠️ 2. Building APKs (For Testing)
*These commands compile the code and generate `.apk` files you can share or test.*

| Command | Description |
|---------|-------------|
| `./gradlew assembleDebug` | Builds the Debug APK. (Find it in `app/build/outputs/apk/debug/`) |
| `./gradlew assembleRelease` | Builds the Release APK (Requires signing config). |
| `./gradlew installDebug` | Builds the Debug APK and **installs it directly** to a connected phone/emulator. |

---

## 🚀 3. Building App Bundles (For Google Play Store)
*Google Play requires `.aab` (Android App Bundle) files instead of APKs.*

| Command | Description |
|---------|-------------|
| `./gradlew bundleRelease` | Builds the optimized Release AAB. (Find it in `app/build/outputs/bundle/release/`) |

---

## 🏃‍♂️ 4. Running & Device Management (ADB)
*Interact with your physical phone or emulator via the command line.*

| Command | Description |
|---------|-------------|
| `adb devices` | Lists all connected Android phones or running emulators. |
| `adb install path/to/app.apk` | Manually installs a specific APK file to your phone. |
| `adb shell am start -n com.mintech.parkwiseapp/.MainActivity` | Launches the app on your phone via the terminal. |
| `adb uninstall com.mintech.parkwiseapp` | Completely uninstalls the app from your phone. |

---

## 🐛 5. Logging & Debugging
*How to see what's crashing without the Android Studio Logcat window.*

| Command | Description |
|---------|-------------|
| `pidcat com.mintech.parkwiseapp` | **(Recommended)** Beautiful, color-coded log stream just for this app. |
| `adb logcat \| grep com.mintech.parkwiseapp` | Standard native log stream filtered for this app. |
| `adb logcat -c` | Clears the log history on the phone (fixes cluttered terminals). |

---

## 📊 6. Code Quality & Profiling
*Check for errors and measure build performance.*

| Command | Description |
|---------|-------------|
| `./gradlew lint` | Scans the codebase for bugs, deprecated code, and bad practices. |
| `./gradlew profile` | Profiles the build process and generates an HTML report showing exactly what is making the build slow (Find it in `build/reports/profile/`). |
| `./gradlew build --dry-run` | Checks if the build *would* succeed without actually taking the time to compile everything. |

---

### 💡 Pro-Tip for VS Code
Open a split terminal (`Cmd + \`):
1. Use the **left** terminal for running `./gradlew installDebug`.
2. Keep the **right** terminal permanently running `pidcat com.mintech.parkwiseapp` to catch crashes instantly.



Ultimate one liner:
./gradlew installDebug && adb shell am start -n com.mintech.parkwiseapp/com.mintech.parkwiseapp.MainActivity