# Releasing Workout Planner to GitHub Releases

This project is configured with automated GitHub Actions CI/CD to build, test, sign, and publish release APKs directly to GitHub Releases.

---

## 🚀 How to Publish a Release

### Method 1: Git Tag (Recommended)

Whenever you are ready to publish a new release:

```bash
# 1. Create a version tag (e.g. v1.0.0)
git tag v1.0.0

# 2. Push the tag to GitHub
git push origin v1.0.0
```

GitHub Actions will automatically:
1. Run all unit tests (`./gradlew test`).
2. Build the signed release APK (`./gradlew assembleRelease`).
3. Compute SHA-256 checksums (`WorkoutPlanner-v1.0.0.apk.sha256`).
4. Generate release notes from git commits.
5. Create a GitHub Release and attach the APK and checksum file.

---

### Method 2: Manual Trigger via GitHub UI

1. Go to your GitHub repository: `https://github.com/mawusikpodo/Workout-Planner`
2. Navigate to the **Actions** tab.
3. Select **Build & Publish Release APK** in the left sidebar.
4. Click **Run workflow**:
   - Provide a release tag (e.g. `v1.0.0`).
   - (Optional) Toggle draft or pre-release.
5. Click **Run workflow**.

---

## 🔑 Keystore & Signing Configuration

The release configuration supports two modes:

### 1. Default Mode (Zero Setup Needed)
If no custom keystore is configured, Gradle automatically signs the release APK using a fallback keystore. This ensures the APK is valid and directly installable when downloaded by users from GitHub Releases without requiring manual key generation.

### 2. Custom Production Signing (Recommended for Official Releases)
To sign releases with your own private key:

1. **Generate a keystore**:
   Run the interactive helper script:
   ```bash
   ./scripts/create_release_keystore.sh
   ```
   This generates `release.keystore` and outputs the exact values needed for GitHub Secrets.

2. **Add GitHub Secrets**:
   Go to your repository on GitHub:
   **Settings** → **Secrets and variables** → **Actions** → **New repository secret**:

   | Secret Name | Value |
   |-------------|-------|
   | `KEYSTORE_BASE64` | Base64-encoded content of your `.keystore` file |
   | `KEYSTORE_PASSWORD` | Keystore password |
   | `KEY_ALIAS` | Key alias (e.g. `workoutplanner`) |
   | `KEY_PASSWORD` | Key password |

---

## 💻 Local Release Build

To build and verify the release APK locally on your machine:

```bash
# Run tests
./gradlew test

# Build release APK
./gradlew assembleRelease
```

The output will be generated at:
```
app/build/outputs/apk/release/app-release.apk
```

To verify the signature on the generated APK:
```bash
/home/kafui/Android/Sdk/build-tools/36.0.0/apksigner verify -v app/build/outputs/apk/release/app-release.apk
```
