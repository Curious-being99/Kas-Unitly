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

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](./LICENSE) file for details.
