# Kas Unitly

Real-time Kaspa (KAS) converter and precision financial calculator supporting global fiat currencies.

A sleek, modern, and highly responsive Android application designed specifically for the Kaspa (KAS) community and cryptocurrency enthusiasts. Kas Unitly combines a high-precision mathematical expression engine with real-time exchange rates, allowing you to instantly perform arithmetic, calculate fees/percentages, and convert between Kaspa (KAS) and your chosen fiat currency simultaneously.

### Key Features
- **Decentralized & Multi-Node Rate Architecture**: Multi-tiered fallback pipeline across Kaspa Network nodes (`api.kaspa.org`), MEXC Global, Gate.io, CoinPaprika, CryptoCompare, and CoinGecko for KAS price, combined with multi-edge CDN mirrors (Open ER-API, ExchangeRate-API, jsDelivr edge, and Cloudflare Pages edge) for fiat exchange rates so prices are never lost even during network disruptions.
- **Offline Cache Protection**: Automatically persists rates to local encrypted storage with baseline anchors, ensuring 100% calculation uptime offline and in airplane mode.
- **Real-Time KAS & Fiat Conversion**: Seamlessly convert between Kaspa (KAS) and 45+ global fiat currencies (including USD, EUR, GBP, CAD, AUD, JPY, and more) with live rate status indicators and instant manual sync.
- **Precision Math Engine**: Zero-error calculation engine supporting standard arithmetic (`+`, `-`, `×`, `÷`), modulo/remainder (`%`), percentage additions/discounts (`100 + 20% = 120`), exponentiation (`^`), square roots (`√`), constants (`π`), and bracket grouping with strict BODMAS/PEMDAS precedence.
- **Dual-Purpose Live Display**: Enter values in Kaspa or standard numbers and immediately see the live mathematical result alongside real-time fiat equivalent values.
- **Calculation History & Room Database**: Automatically preserves previous calculations and conversions locally using Android Jetpack Room with full history search, replay, and management.
- **Tactile Material 3 Keypad**: Custom 5-column scientific keypad with intuitive color-coded keys, operator overwrite protection, and smooth haptic/ripple feedback.
- **Clean Modern Design**: High-contrast, edge-to-edge layout with full support for dark mode and adaptive sizing for phones, foldables, and tablets.

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
│           │           ├── BaseApplication.kt        # Application context and database setup
│           │           ├── MainActivity.kt           # Jetpack Compose UI, Splash screen, and Keypad
│           │           ├── MathEvaluator.kt          # Robust mathematical evaluation & tokenizer engine
│           │           ├── KaspaApi.kt               # Retrofit networking client for live exchange rates
│           │           ├── KaspaViewModel.kt         # State management, caching, and conversion logic
│           │           └── data
│           │               ├── HistoryEntity.kt      # Database model for calculation items
│           │               ├── HistoryDao.kt         # Room DAO for local history persistence
│           │               └── AppDatabase.kt        # Room database builder
│           └── res
│               └── values
│                   └── strings.xml                   # Application string resources
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
