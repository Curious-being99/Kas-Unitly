# GPT & Application Security Policy

This document outlines the security posture, threat model, and validation logic implemented in the **Kaspa Calculator** (KAS Calculator) to ensure tamper-proof, crash-resistant, and secure performance.

---

## 🔒 1. Threat Model & Risk Profile

The Kaspa Calculator executes all mathematical calculations and data caching locally on the client device. However, because it interfaces with external cryptocurrency APIs (CoinGecko) and handles input parsing dynamically, it maintains standard security controls against several attack vectors:

| Threat Vector | Description | Implemented Countermeasure |
| :--- | :--- | :--- |
| **Parsing Denial of Service (DoS)** | Maliciously nested math expressions (e.g., deep recursion) causing stack overflow. | Balanced recursion controls, stack limit handling, and robust `try-catch` boundaries inside `eval()`. |
| **API Spoofing / Hijacking** | Intercepting pricing responses to manipulate fiat conversion displays. | Enforcement of strict **HTTPS** protocol in Retrofit, system-level trusted CA certificates. |
| **Local Cache Tampering** | Modifying saved pricing data to display inflated/deflated rates. | Fallback structural JSON validation; raw pricing arrays are verified before recalculating. |
| **Command Injection** | Injecting operating system commands through the keypad parser. | Pure mathematical AST evaluation; no execution of shell or system environments. |

---

## 🧠 2. Math Parser Security

A common vector in standard calculation engines is arbitrary string evaluation (such as JS `eval` or dynamic bytecode generation). 

* **AST / Recursive Descent Architecture:** The calculator employs a **custom Recursive Descent Parser** built strictly for mathematical expressions. It does not parse or execute shell, Kotlin, or JVM commands.
* **Input Sanitization:** The keyboard keypad only permits registered characters: `[0-9, ., +, -, *, /, %, ^, √, (, )]`. Any unapproved external strings are automatically stripped.
* **Stack Overflow & Crash Boundaries:** Any nested mathematical loop or recursive structure that fails evaluation is safely caught by the try-catch wrapper in `evaluateMath`, immediately returning `null` rather than propagating standard JVM crashes:
```kotlin
private fun evaluateMath(expr: String): Double? {
    if (expr.isEmpty()) return null
    var cleanExpr = expr.replace("×", "*").replace("÷", "/")
    return try {
        eval(cleanExpr)
    } catch (e: Exception) {
        null // Graceful fallback
    }
}
```

---

## 🌐 3. GPT-Prompt Security & LLM Integrity

If this application or its backend is exposed to or driven by an LLM / GPT model (e.g., voice assistants, auto-calculators), follow these strict safety protocols:

1. **System Prompt Hardening:** Ensure the system instructions specify that the model should **never** output executable code segments directly to the calculator console.
2. **Formula Integrity:** When requesting a conversion from the GPT assistant, the payload must be pre-parsed locally before being sent to the calculation engine to prevent prompt injection.
3. **No Direct Executables:** Do not pass unfiltered user feedback into live variables without converting them into validated double numbers (`toDoubleOrNull()`).

---

## 🔑 4. API Key & Endpoint Management

* **Zero Hardcoded Credentials:** The Coingecko price-tracking API is a public endpoint and requires no API keys.
* **Upgrading to Pro Tier:** If upgrading to CoinGecko Pro or another paid index requiring a header token:
  * Do **NOT** hardcode the API key in `KaspaApi.kt` or `build.gradle.kts`.
  * Store it in the **Secrets Panel** (which populates the `.env` configuration file).
  * Access it securely via `BuildConfig` using the Android Secrets Gradle Plugin.

---

## 💾 5. Data Privacy & Local Storage (Room)

* **Local History Containment:** All calculation history items are stored on the local device's SQLite database via Android's **Room Database Library**. No transaction history, numeric results, or conversion logs are sent to remote servers.
* **Total Deletion Control:** Calculation history is only deleted when explicitly requested by the user tapping the "Delete" icon. The `clearAll()` transaction fully purges local history tables.
