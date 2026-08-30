# Kas Unitly

A sleek, modern, and highly responsive calculator and converter designed specifically for the Kaspa (KAS) community. This app combines a powerful mathematical expression calculator with a real-time cryptocurrency converter, allowing you to instantly compute math and convert between Kaspa (KAS) and your local fiat currency simultaneously.

### Key Features
- **Dual-Purpose Display**: Solve complex mathematical equations and immediately see how those calculations translate to Kaspa and fiat values.
- **Real-Time Conversion**: Seamlessly convert between Kaspa (KAS) and multiple global fiat currencies (including USD, EUR, GBP, and more) with up-to-date exchange rates.
- **Kaspa-Centric Calculation**: Keypad computations directly compute Kaspa quantities with instant real-time fiat estimation.
- **Advanced Math Keyboard**: Includes a custom Material 3 styled scientific keypad featuring standard operators, brackets, decimals, and special functions ($\pi$, $\sqrt{}$, power, etc.) with tactile ripple feedback.
- **Beautiful Material 3 Design**: Features a clean, high-contrast user interface with an elegant dark-theme option, designed with balanced negative space to keep all functions perfectly visible on any screen size.

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

## ⚙️ Automated CI/CD & Releases (GitHub Actions)

A pre-configured GitHub Actions workflow automatically compiles, runs checks, and builds a downloadable signed production **Release APK** on every commit or pull request.

### **How to find your built APK on GitHub:**
1. **GitHub Releases (Recommended):** On every push to the default branch (`main` or `master`), the workflow automatically creates a new GitHub Release containing the signed production APK. Simply look at the **Releases** section on the right-hand sidebar of your GitHub repository page!
2. **GitHub Actions Artifacts:** You can also go to the **Actions** tab on your GitHub repository page, click the latest workflow execution, scroll to the bottom, and download the `kas-unitly-release-apk` artifact.

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
