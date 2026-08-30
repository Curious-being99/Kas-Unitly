# Kaspa Calculator (KAS)

A beautifully-designed, high-performance, and resilient **Kaspa (KAS) Calculator** for Android. Built with modern Android development standards using **Kotlin**, **Jetpack Compose (Material 3)**, **Room Database**, **Retrofit**, and lifecycle-aware architecture.

---

## ✨ Key Features

*   **🎬 Typewriter Splash Screen:** A sleek, minimal splash interface that dynamically types out `kas calculator` in a glowing Kaspa Cyan accent before smoothly transitioning to the main layout.
*   **🪙 Live Conversions:** Instantly converts Kaspa (KAS) into USD, EUR, GBP, JPY, and over 40 other world currencies using real-time price feeds.
*   **💾 Smart Local Caching:** Automatically caches the latest successfully fetched currency rates into the device storage (`SharedPreferences`). If the device is offline when opened, the app remains fully functional with the last known prices.
*   **🔄 Lifecycle-Aware Auto-Updates:** Listens to Android system lifecycle events. Whenever you switch away and return to the application, it immediately forces a background refresh to fetch the absolute latest price.
*   **📱 Custom Math Parser Engine:** A safe, robust Recursive Descent Parser that processes advanced math equations, brackets `()`, exponents `^`, and square roots `√` cleanly without risk of runtime crashes.
*   **🗄️ Persistent Calculation History:** Saves all calculation events securely to a local SQLite database via Room. Calculation history remains permanently stored and is only deleted when explicitly wiped by the user.

---

## 🏗️ Technical Architecture

The app conforms to standard **Clean Architecture** and **MVVM (Model-View-ViewModel)** guidelines:

```
├── app
│   └── src
│       └── main
│           ├── java
│           │   └── com
│           │       └── example
│           │           ├── BaseApplication.kt        # App context and database initialization
│           │           ├── MainActivity.kt           # Screen containers, Splash screen, and UI layout
│           │           ├── KaspaApi.kt               # Retrofit networking client and CoinGecko API
│           │           ├── KaspaViewModel.kt         # Live state, caching management, and math evaluation
│           │           └── data
│           │               ├── HistoryEntity.kt      # Database model for calculation items
│           │               ├── HistoryDao.kt         # Local Room Database operations
│           │               └── AppDatabase.kt        # Room database builder
│           └── res
│               └── values
│                   └── strings.xml                   # App-wide string resources
```

---

## ⚙️ Automated CI/CD (GitHub Actions)

A pre-configured GitHub Actions workflow automatically compiles, runs checks, and builds a downloadable **Unsigned Release APK** on every commit or pull request. If configured, it will also build and sign a production **Release APK**.

### **How to find your built APK on GitHub:**
1. Push this project to your GitHub repository.
2. Go to the **Actions** tab on your GitHub repository page.
3. Select the latest **Build Android APK** workflow execution.
4. Scroll down to the **Artifacts** section at the bottom of the page and download `kas-calculator-unsigned-apk` or `kas-calculator-release-apk`.

### **🔑 Setting up Release Signing (GitHub Secrets):**

To build a secure, signed **Release APK** automatically, add the following Secrets to your GitHub Repository (**Settings > Secrets and variables > Actions > New repository secret**):

1. **`RELEASE_KEYSTORE_BASE64`**: The Base64 encoded string of your release keystore (`.jks` / `.keystore`) file.
   * *To generate a keystore:* 
     ```bash
     keytool -genkey -v -keystore my-upload-key.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000
     ```
   * *To convert it to Base64 (Mac/Linux):* 
     ```bash
     base64 -i my-upload-key.jks | tr -d '\n'
     # Or: openssl base64 -A -in my-upload-key.jks
     ```
   * *To convert it to Base64 (Windows PowerShell):*
     ```powershell
     [Convert]::ToBase64String([IO.File]::ReadAllBytes("my-upload-key.jks")) | Out-File -FilePath keystore_base64.txt
     ```
2. **`STORE_PASSWORD`**: The password chosen when creating the keystore.
3. **`KEY_PASSWORD`**: The password chosen for the key alias.

Once these secrets are configured on GitHub, subsequent action runs will automatically generate and attach a signed production-ready **`kas-calculator-release-apk`** artifact!

---

## 🛠️ Build and Development

### Prerequisites
*   JDK 17
*   Android SDK

### Build Commands

To compile and verify the build locally:
```bash
gradle :app:assembleDebug
```

To run standard unit and Robolectric tests:
```bash
gradle :app:testDebugUnitTest
```

---

## 🔒 Security Posture
Refer to [GPT_SECURITY.md](./GPT_SECURITY.md) for details about the mathematical parser security, credentials policy, local database sanitation, and prompt integrity practices.
