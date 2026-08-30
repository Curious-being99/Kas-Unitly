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
        // Load cached prices
        val cachedPricesJson = prefs.getString("cached_prices", null)
        if (cachedPricesJson != null) {
            try {
                val json = org.json.JSONObject(cachedPricesJson)
                val map = mutableMapOf<String, Double>()
                val keys = json.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    map[key] = json.getDouble(key)
                }
                _state.update { it.copy(prices = map) }
                recalculate()
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
            try {
                // Dual strategy: Try official direct + open fiat exchange rates API first (Highly reliable, no rate limits)
                val kaspaPrices = try {
                    val officialPriceResponse = api.getOfficialKaspaPrice()
                    val fiatRatesResponse = api.getFiatRates()
                    val priceUsd = officialPriceResponse.price
                    val rates = fiatRatesResponse.rates
                    
                    val map = mutableMapOf<String, Double>()
                    for (fiat in supportedFiats) {
                        val rate = rates[fiat.uppercase()] ?: rates[fiat.lowercase()]
                        if (rate != null) {
                            map[fiat] = priceUsd * rate
                        }
                    }
                    if (map.isNotEmpty()) {
                        map
                    } else {
                        throw Exception("Empty direct rate map")
                    }
                } catch (directEx: Exception) {
                    // Fallback to CoinGecko (Legacy option)
                    try {
                        val response = api.getKaspaPrice(supportedFiats.joinToString(","))
                        response["kaspa"] ?: emptyMap()
                    } catch (cgEx: Exception) {
                        throw Exception("Both direct and fallback price fetches failed: ${cgEx.localizedMessage}")
                    }
                }
                
                try {
                    val json = org.json.JSONObject()
                    for ((k, v) in kaspaPrices) {
                        json.put(k, v)
                    }
                    prefs.edit().putString("cached_prices", json.toString()).apply()
                } catch (e: Exception) {}

                _state.update { it.copy(prices = kaspaPrices, isLoading = false, lastUpdated = System.currentTimeMillis()) }
                recalculate()
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.localizedMessage) }
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
                    if (isBinaryOperator && currentExpr.isNotEmpty()) {
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
