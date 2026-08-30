package com.example

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.HistoryDao
import com.example.data.HistoryEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class InputMode { KAS, FIAT }

data class KaspaState(
    val kaspaAmount: String = "0",
    val fiatAmount: String = "0",
    val activeInput: InputMode = InputMode.KAS,
    val selectedFiat: String = "usd",
    val prices: Map<String, Double> = mapOf(
        "usd" to 0.0276,
        "eur" to 0.0238,
        "gbp" to 0.0204,
        "jpy" to 4.41,
        "cad" to 0.0383,
        "aud" to 0.0385,
        "chf" to 0.0223,
        "cny" to 0.186
    ),
    val isLoading: Boolean = false,
    val error: String? = null,
    val lastUpdated: Long = 0L,
    val activePriceSource: String = "Decentralized Network",
    val isOffline: Boolean = false,
    val mathExpression: String = "",
    val mathResult: String = "",
    val lastFinalizedExpression: String = "",
    val isFinalized: Boolean = false
)

class KaspaViewModel(private val historyDao: HistoryDao, private val prefs: android.content.SharedPreferences) : ViewModel() {
    private val api = KaspaApi.create()

    private val _state = MutableStateFlow(KaspaState())
    val state: StateFlow<KaspaState> = _state.asStateFlow()

    val history: StateFlow<List<HistoryEntity>> = historyDao.getAllHistory()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        // Load cached prices from persistent storage immediately
        val cachedPricesJson = prefs.getString("cached_prices", null)
        val cachedSource = prefs.getString("cached_price_source", "Decentralized Cache") ?: "Decentralized Cache"
        val cachedTimestamp = prefs.getLong("cached_price_timestamp", 0L)
        if (cachedPricesJson != null) {
            try {
                val json = org.json.JSONObject(cachedPricesJson)
                val map = mutableMapOf<String, Double>()
                val keys = json.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    map[key] = json.getDouble(key)
                }
                if (map.isNotEmpty()) {
                    _state.update { it.copy(prices = map, activePriceSource = cachedSource, lastUpdated = cachedTimestamp) }
                    recalculate()
                }
            } catch (e: Exception) {
                // Ignore parse errors
            }
        }

        viewModelScope.launch {
            while (true) {
                fetchPrices()
                kotlinx.coroutines.delay(15000)
            }
        }
    }

    val supportedFiats = listOf(
        "usd", "eur", "gbp", "jpy", "aud", "cad", "chf", "cny", "hkd", "nzd", // Majors
        "aed", "ars", "bdt", "bhd", "bmd", "brl", "clp", "czk", "dkk", "huf", // Others A-H
        "idr", "ils", "inr", "krw", "kwd", "lkr", "mmk", "mxn", "myr", "ngn", // Others I-N
        "nok", "php", "pkr", "pln", "rub", "sar", "sek", "sgd", "thb", "try", // Others N-T
        "twd", "uah", "vef", "vnd", "zar"                                     // Others T-Z
    )

    fun fetchPrices() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            var fetchedKaspaUsdPrice: Double? = null
            var sourceLabel = "Kaspa Network"

            // --- STEP 1: Multi-Tier KAS/USD Price Fetching Pipeline ---
            // Tier 1: Official Kaspa Node / Indexer API
            try {
                val resp = api.getOfficialKaspaPrice()
                if (resp.price > 0.0) {
                    fetchedKaspaUsdPrice = resp.price
                    sourceLabel = "Kaspa Network Node"
                }
            } catch (e: Exception) {
                // Proceed to Tier 2
            }

            // Tier 2: MEXC Global Spot Market (High-volume KAS hub)
            if (fetchedKaspaUsdPrice == null) {
                try {
                    val mexcResp = api.getMexcKaspaPrice()
                    val p = mexcResp.price?.toDoubleOrNull()
                    if (p != null && p > 0.0) {
                        fetchedKaspaUsdPrice = p
                        sourceLabel = "MEXC Liquidity Node"
                    }
                } catch (e: Exception) {
                    // Proceed to Tier 3
                }
            }

            // Tier 3: Gate.io Global Spot Market
            if (fetchedKaspaUsdPrice == null) {
                try {
                    val gateResp = api.getGateKaspaPrice()
                    val p = gateResp.firstOrNull()?.last?.toDoubleOrNull()
                    if (p != null && p > 0.0) {
                        fetchedKaspaUsdPrice = p
                        sourceLabel = "Gate.io Market Node"
                    }
                } catch (e: Exception) {
                    // Proceed to Tier 4
                }
            }

            // Tier 4: CoinPaprika Open Decentralized Crypto Index
            if (fetchedKaspaUsdPrice == null) {
                try {
                    val paprikaResp = api.getCoinPaprikaKaspaPrice()
                    val p = paprikaResp.quotes?.USD?.price
                    if (p != null && p > 0.0) {
                        fetchedKaspaUsdPrice = p
                        sourceLabel = "CoinPaprika Feed"
                    }
                } catch (e: Exception) {
                    // Proceed to Tier 5
                }
            }

            // Tier 5: CryptoCompare Aggregator
            if (fetchedKaspaUsdPrice == null) {
                try {
                    val ccResp = api.getCryptoComparePrice()
                    val p = ccResp.USD
                    if (p != null && p > 0.0) {
                        fetchedKaspaUsdPrice = p
                        sourceLabel = "CryptoCompare Feed"
                    }
                } catch (e: Exception) {
                    // Proceed to Tier 6
                }
            }

            // Tier 6: CoinGecko USD Price
            if (fetchedKaspaUsdPrice == null) {
                try {
                    val cgResp = api.getKaspaPrice("usd")
                    val p = cgResp["kaspa"]?.get("usd")
                    if (p != null && p > 0.0) {
                        fetchedKaspaUsdPrice = p
                        sourceLabel = "CoinGecko Index"
                    }
                } catch (e: Exception) {
                    // All live KAS/USD nodes failed
                }
            }

            // --- STEP 2: Multi-Tier Global Fiat Rates Pipeline ---
            var fiatRatesMap: Map<String, Double>? = null

            if (fetchedKaspaUsdPrice != null) {
                // Fiat Tier 1: Open ER-API (open exchange rate feed)
                try {
                    val erResp = api.getFiatRatesOpenEr()
                    if (!erResp.rates.isNullOrEmpty()) {
                        fiatRatesMap = erResp.rates
                    }
                } catch (e: Exception) {
                    // Proceed to Fiat Tier 2
                }

                // Fiat Tier 2: ExchangeRate-API v4 mirror
                if (fiatRatesMap == null) {
                    try {
                        val exResp = api.getFiatRatesExchangeRateApi()
                        if (!exResp.rates.isNullOrEmpty()) {
                            fiatRatesMap = exResp.rates
                        }
                    } catch (e: Exception) {
                        // Proceed to Fiat Tier 3
                    }
                }

                // Fiat Tier 3: Decentralized jsDelivr Edge CDN Currency API
                if (fiatRatesMap == null) {
                    try {
                        val jsdResp = api.getFiatRatesJsDelivr()
                        if (!jsdResp.usd.isNullOrEmpty()) {
                            fiatRatesMap = jsdResp.usd
                        }
                    } catch (e: Exception) {
                        // Proceed to Fiat Tier 4
                    }
                }

                // Fiat Tier 4: Cloudflare Pages Edge CDN Currency API
                if (fiatRatesMap == null) {
                    try {
                        val cfResp = api.getFiatRatesCloudflare()
                        if (!cfResp.usd.isNullOrEmpty()) {
                            fiatRatesMap = cfResp.usd
                        }
                    } catch (e: Exception) {
                        // All fiat mirrors failed
                    }
                }
            }

            // --- STEP 3: Composite Multi-Rate Assembly & Cache Protection ---
            val calculatedPrices = mutableMapOf<String, Double>()

            if (fetchedKaspaUsdPrice != null && fiatRatesMap != null) {
                for (fiat in supportedFiats) {
                    val rate = fiatRatesMap[fiat.uppercase()]
                        ?: fiatRatesMap[fiat.lowercase()]
                        ?: if (fiat.equals("usd", ignoreCase = true)) 1.0 else null
                    if (rate != null) {
                        calculatedPrices[fiat] = fetchedKaspaUsdPrice * rate
                    }
                }
            }

            // Direct Multi-Fiat Fallback (CoinGecko all-in-one query if cross-rates unavailable)
            if (calculatedPrices.isEmpty()) {
                try {
                    val multiCgResp = api.getKaspaPrice(supportedFiats.joinToString(","))
                    val cgMap = multiCgResp["kaspa"]
                    if (!cgMap.isNullOrEmpty()) {
                        calculatedPrices.putAll(cgMap)
                        sourceLabel = "CoinGecko Multi-Feed"
                    }
                } catch (e: Exception) {
                    // Fallback to offline stored state
                }
            }

            if (calculatedPrices.isNotEmpty()) {
                // Success with one of the decentralized/redundant tiers
                val now = System.currentTimeMillis()
                try {
                    val json = org.json.JSONObject()
                    for ((k, v) in calculatedPrices) {
                        json.put(k, v)
                    }
                    prefs.edit()
                        .putString("cached_prices", json.toString())
                        .putString("cached_price_source", sourceLabel)
                        .putLong("cached_price_timestamp", now)
                        .apply()
                } catch (e: Exception) {}

                _state.update {
                    it.copy(
                        prices = calculatedPrices,
                        isLoading = false,
                        error = null,
                        lastUpdated = now,
                        activePriceSource = sourceLabel,
                        isOffline = false
                    )
                }
                recalculate()
            } else {
                // If completely disconnected, retain existing cached prices & indicate offline resilience
                _state.update {
                    it.copy(
                        isLoading = false,
                        activePriceSource = if (it.lastUpdated > 0L) "Cached Rates (${it.activePriceSource})" else "Baseline Offline Rates",
                        isOffline = true
                    )
                }
                recalculate()
            }
        }
    }

    fun onSelectedFiatChanged(fiat: String) {
        _state.update { it.copy(selectedFiat = fiat, activeInput = InputMode.KAS) }
        recalculate()
    }
    
    fun setActiveInput(mode: InputMode) {
        // Input always stays anchored to Kaspa zone
        val initialExpr = if (_state.value.kaspaAmount == "0" || _state.value.kaspaAmount == "0.00") "" else _state.value.kaspaAmount
        _state.update { it.copy(activeInput = InputMode.KAS, mathExpression = initialExpr, mathResult = "") }
    }
    
    fun onKeypadPress(key: String) {
        val st = _state.value
        val currentExpr = st.mathExpression
        var newExpr = currentExpr
        var shouldEvaluateFinal = false
        var nextFinalized = st.isFinalized
        var nextLastFinalizedExpr = st.lastFinalizedExpression

        val isDigitOrDot = key in listOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", ".", "•")
        val isBinaryOperator = key in listOf("+", "-", "×", "÷", "^", "%")

        when (key) {
            "AC" -> {
                saveToHistory()
                newExpr = ""
                nextFinalized = false
                nextLastFinalizedExpr = ""
            }
            "DEL" -> {
                if (nextFinalized) {
                    nextFinalized = false
                    nextLastFinalizedExpr = ""
                }
                if (currentExpr.isNotEmpty()) {
                    if (currentExpr.endsWith("sqrt(")) {
                        newExpr = currentExpr.dropLast(5)
                    } else {
                        newExpr = currentExpr.dropLast(1)
                    }
                }
            }
            "±" -> {
                if (nextFinalized) {
                    nextFinalized = false
                    nextLastFinalizedExpr = ""
                }
                if (currentExpr.startsWith("-(") && currentExpr.endsWith(")")) {
                    newExpr = currentExpr.substring(2, currentExpr.length - 1)
                } else if (currentExpr.startsWith("-")) {
                    newExpr = currentExpr.substring(1)
                } else if (currentExpr.isNotEmpty()) {
                    newExpr = "-($currentExpr)"
                } else {
                    newExpr = "-"
                }
            }
            "=" -> {
                if (currentExpr.isNotEmpty()) {
                    shouldEvaluateFinal = true
                }
            }
            " " -> {
                // Ignore empty keys
            }
            else -> {
                val keyToAppend = when (key) {
                    "•" -> "."
                    else -> key
                }

                if (nextFinalized) {
                    if (isDigitOrDot || key == "π" || key == "√" || key == "(") {
                        // Start fresh calculation when typing a new number/function after equals
                        newExpr = if (keyToAppend == "√") "√" else keyToAppend
                    } else {
                        // Continue/chain calculation on previous result with operator
                        newExpr = currentExpr + keyToAppend
                    }
                    nextFinalized = false
                    nextLastFinalizedExpr = ""
                } else {
                    if (keyToAppend == ".") {
                        val lastOpIndex = currentExpr.lastIndexOfAny(charArrayOf('+', '-', '×', '÷', '^', '%', '(', ')'))
                        val currentNumberSegment = if (lastOpIndex >= 0) currentExpr.substring(lastOpIndex + 1) else currentExpr
                        if (currentNumberSegment.contains(".")) {
                            // Already has a dot in this number token, ignore duplicate dot
                            return
                        }
                        if (currentNumberSegment.isEmpty() || currentExpr.isEmpty() || currentExpr.last() in listOf('+', '-', '×', '÷', '^', '%', '(')) {
                            newExpr = currentExpr + "0."
                        } else {
                            newExpr = currentExpr + "."
                        }
                    } else if (isBinaryOperator && currentExpr.isNotEmpty()) {
                        val lastChar = currentExpr.last().toString()
                        if (lastChar in listOf("+", "-", "×", "÷", "^")) {
                            // If pressing minus after multiply or divide, allow unary minus (e.g. 5 × -)
                            if (keyToAppend == "-" && (lastChar == "×" || lastChar == "÷")) {
                                newExpr = currentExpr + keyToAppend
                            } else {
                                // Replace trailing operator with the new operator
                                newExpr = currentExpr.dropLast(1) + keyToAppend
                            }
                        } else {
                            newExpr = currentExpr + keyToAppend
                        }
                    } else {
                        newExpr = currentExpr + keyToAppend
                    }
                }
            }
        }

        val resultVal = evaluateMath(newExpr)
        val resultStr = resultVal?.let { formatResult(it) } ?: ""

        if (shouldEvaluateFinal) {
            if (resultVal != null) {
                nextLastFinalizedExpr = currentExpr
                newExpr = resultStr
                nextFinalized = true
            }
        }

        _state.update { 
            it.copy(
                mathExpression = newExpr,
                mathResult = resultStr,
                lastFinalizedExpression = nextLastFinalizedExpr,
                isFinalized = nextFinalized
            ) 
        }

        // Automatically update the active input and convert if valid
        if (resultVal != null) {
            if (st.activeInput == InputMode.KAS) {
                _state.update { it.copy(kaspaAmount = resultStr) }
            } else {
                _state.update { it.copy(fiatAmount = resultStr) }
            }
            recalculate()
            if (shouldEvaluateFinal) {
                saveToHistory()
            }
        } else if (newExpr.isEmpty()) {
             if (st.activeInput == InputMode.KAS) {
                _state.update { it.copy(kaspaAmount = "0") }
            } else {
                _state.update { it.copy(fiatAmount = "0") }
            }
            recalculate()
        }
    }

    private fun evaluateMath(expr: String): Double? {
        return MathEvaluator.evaluate(expr)
    }

    private fun formatResult(value: Double): String {
        if (value.isNaN() || value.isInfinite()) return ""
        return try {
            val bd = java.math.BigDecimal.valueOf(value)
                .setScale(10, java.math.RoundingMode.HALF_UP)
                .stripTrailingZeros()
            val plain = bd.toPlainString()
            if (plain == "-0") "0" else plain
        } catch (e: Exception) {
            if (value == value.toLong().toDouble()) {
                value.toLong().toString()
            } else {
                String.format(java.util.Locale.US, "%.6f", value).trimEnd('0').trimEnd('.')
            }
        }
    }

    private fun formatConversionValue(value: Double, isFiat: Boolean): String {
        if (value == 0.0) return "0"
        if (value.isNaN() || value.isInfinite()) return "0"
        
        val absVal = Math.abs(value)
        val formatted = when {
            absVal >= 1000.0 -> String.format(java.util.Locale.US, "%.2f", value)
            absVal >= 1.0 -> String.format(java.util.Locale.US, if (isFiat) "%.4f" else "%.4f", value)
            absVal >= 0.0001 -> String.format(java.util.Locale.US, "%.4f", value)
            else -> String.format(java.util.Locale.US, "%.6f", value)
        }
        return if (formatted.contains(".")) {
            val trimmed = formatted.trimEnd('0').trimEnd('.')
            if (trimmed.isEmpty()) "0" else trimmed
        } else {
            formatted
        }
    }

    private fun recalculate() {
        val st = _state.value
        val price = st.prices[st.selectedFiat] ?: 0.0
        if (price <= 0.0) return

        if (st.activeInput == InputMode.KAS) {
            val kas = st.kaspaAmount.replace(",", ".").toDoubleOrNull() ?: 0.0
            val fiat = kas * price
            _state.update { it.copy(fiatAmount = formatConversionValue(fiat, isFiat = true)) }
        } else {
            val fiat = st.fiatAmount.replace(",", ".").toDoubleOrNull() ?: 0.0
            val kas = fiat / price
            _state.update { it.copy(kaspaAmount = formatConversionValue(kas, isFiat = false)) }
        }
    }

    private var lastSavedKaspa = ""

    fun saveToHistory() {
        val st = _state.value
        val kas = st.kaspaAmount.toDoubleOrNull() ?: 0.0
        val fiat = st.fiatAmount.toDoubleOrNull() ?: 0.0
        if (kas > 0 && fiat > 0 && st.kaspaAmount != lastSavedKaspa) {
            lastSavedKaspa = st.kaspaAmount
            viewModelScope.launch {
                historyDao.insert(
                    HistoryEntity(
                        kaspaAmount = st.kaspaAmount,
                        fiatAmount = st.fiatAmount,
                        fiatCurrency = st.selectedFiat.uppercase(),
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    fun clearHistory() {
        lastSavedKaspa = ""
        viewModelScope.launch {
            historyDao.clearAll()
        }
    }

    fun restoreFromHistory(item: HistoryEntity) {
        val fiat = item.fiatCurrency.lowercase()
        if (fiat in supportedFiats) {
            _state.update {
                it.copy(
                    selectedFiat = fiat,
                    kaspaAmount = item.kaspaAmount,
                    fiatAmount = item.fiatAmount,
                    mathExpression = item.kaspaAmount,
                    mathResult = "",
                    lastFinalizedExpression = "",
                    isFinalized = true
                )
            }
            recalculate()
        }
    }

    companion object {
        fun provideFactory(context: android.content.Context, historyDao: HistoryDao): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val prefs = context.getSharedPreferences("kaspa_prefs", android.content.Context.MODE_PRIVATE)
                return KaspaViewModel(historyDao, prefs) as T
            }
        }
    }
}
