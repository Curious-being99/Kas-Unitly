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
    val prices: Map<String, Double> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val lastUpdated: Long = 0L,
    val mathExpression: String = "",
    val mathResult: String = ""
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
                val response = api.getKaspaPrice(supportedFiats.joinToString(","))
                val kaspaPrices = response["kaspa"] ?: emptyMap()
                
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
        _state.update { it.copy(selectedFiat = fiat) }
        recalculate()
    }
    
    fun setActiveInput(mode: InputMode) {
        if (_state.value.activeInput == mode) return
        val currentAmount = if (mode == InputMode.KAS) _state.value.kaspaAmount else _state.value.fiatAmount
        val initialExpr = if (currentAmount == "0" || currentAmount == "0.00") "" else currentAmount
        _state.update { it.copy(activeInput = mode, mathExpression = initialExpr, mathResult = "") }
    }
    
    fun onKeypadPress(key: String) {
        val st = _state.value
        val currentExpr = st.mathExpression
        var newExpr = currentExpr
        var shouldEvaluateFinal = false

        when (key) {
            "AC" -> {
                saveToHistory()
                newExpr = ""
            }
            "DEL" -> {
                if (currentExpr.isNotEmpty()) {
                    newExpr = currentExpr.dropLast(1)
                }
            }
            "=" -> {
                shouldEvaluateFinal = true
            }
            " " -> {
                // Ignore empty keys
            }
            else -> {
                newExpr = currentExpr + key
            }
        }

        val resultVal = evaluateMath(newExpr)
        val resultStr = resultVal?.let { formatResult(it) } ?: ""

        if (shouldEvaluateFinal) {
            if (resultVal != null) {
                newExpr = resultStr
            }
        }

        _state.update { it.copy(mathExpression = newExpr, mathResult = resultStr) }

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
        if (expr.isEmpty()) return null
        
        var cleanExpr = expr.replace("×", "*")
            .replace("÷", "/")
            .replace("π", Math.PI.toString())
            .replace("•", ".")
            .replace(":", "/") // Assuming : is used for division if typed
            .replace("\\", "/")
        
        return try {
            eval(cleanExpr)
        } catch (e: Exception) {
            null
        }
    }

    private fun eval(str: String): Double {
        return object : Any() {
            var pos = -1
            var ch = 0
            fun nextChar() {
                ch = if (++pos < str.length) str[pos].code else -1
            }

            fun eat(charToEat: Int): Boolean {
                while (ch == ' '.code) nextChar()
                if (ch == charToEat) {
                    nextChar()
                    return true
                }
                return false
            }

            fun parse(): Double {
                nextChar()
                val x = parseExpression()
                if (pos < str.length) throw RuntimeException("Unexpected: " + ch.toChar())
                return x
            }

            fun parseExpression(): Double {
                var x = parseTerm()
                while (true) {
                    if (eat('+'.code)) x += parseTerm()
                    else if (eat('-'.code)) x -= parseTerm()
                    else return x
                }
            }

            fun parseTerm(): Double {
                var x = parseFactor()
                while (true) {
                    if (eat('*'.code)) x *= parseFactor()
                    else if (eat('/'.code)) x /= parseFactor()
                    else if (eat('%'.code)) x %= parseFactor()
                    else return x
                }
            }

            fun parseFactor(): Double {
                if (eat('+'.code)) return parseFactor()
                if (eat('-'.code)) return -parseFactor()
                var x = 0.0
                val startPos = pos
                if (eat('('.code)) {
                    x = parseExpression()
                    eat(')'.code)
                } else if ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code || ch == 'E'.code) {
                    while ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code || ch == 'E'.code) nextChar()
                    x = str.substring(startPos, pos).toDouble()
                } else if (eat('√'.code)) {
                    x = Math.sqrt(parseFactor())
                } else {
                    throw RuntimeException("Unexpected: " + ch.toChar())
                }
                
                if (eat('^'.code)) x = Math.pow(x, parseFactor())
                
                return x
            }
        }.parse()
    }

    private fun formatResult(value: Double): String {
        if (value.isNaN() || value.isInfinite()) return ""
        if (value == value.toLong().toDouble()) {
            return value.toLong().toString()
        }
        val formatted = String.format(java.util.Locale.US, "%.6f", value)
        return if (formatted.contains(".")) {
            formatted.trimEnd('0').trimEnd('.')
        } else {
            formatted
        }
    }

    private fun recalculate() {
        val st = _state.value
        val price = st.prices[st.selectedFiat] ?: 0.0
        if (price == 0.0) return

        if (st.activeInput == InputMode.KAS) {
            val kas = st.kaspaAmount.replace(",", ".").toDoubleOrNull() ?: 0.0
            val fiat = kas * price
            _state.update { it.copy(fiatAmount = String.format(java.util.Locale.US, "%.2f", fiat)) }
        } else {
            val fiat = st.fiatAmount.replace(",", ".").toDoubleOrNull() ?: 0.0
            val kas = fiat / price
            _state.update { it.copy(kaspaAmount = String.format(java.util.Locale.US, "%.4f", kas)) }
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
