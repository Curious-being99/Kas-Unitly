package com.example

import net.objecthunter.exp4j.ExpressionBuilder
import net.objecthunter.exp4j.operator.Operator

object MathEvaluator {
    // Custom modulo operator: A % B = remainder of A / B (e.g. 1000 % 2 = 0, 10 % 3 = 1)
    private val moduloOperator = object : Operator("%", 2, true, Operator.PRECEDENCE_MULTIPLICATION) {
        override fun apply(vararg args: Double): Double {
            if (args[1] == 0.0) throw ArithmeticException("Division by zero")
            return args[0] % args[1]
        }
    }

    fun evaluate(expression: String): Double? {
        if (expression.isBlank()) return null
        val prepared = preprocessExpression(expression)
        if (prepared.isBlank()) return null
        
        return try {
            val exp = ExpressionBuilder(prepared)
                .operator(moduloOperator)
                .build()
            val result = exp.evaluate()
            if (result.isNaN() || result.isInfinite()) null else result
        } catch (e: Exception) {
            null
        }
    }

    fun preprocessExpression(raw: String): String {
        var expr = raw.trim()
        if (expr.isEmpty()) return ""

        // 1. Replace UI symbols with standard math tokens
        expr = expr
            .replace("×", "*")
            .replace("÷", "/")
            .replace(":", "/")
            .replace("\\", "/")
            .replace("•", ".")
            .replace("−", "-")
            .replace("–", "-")
            .replace("—", "-")
            .replace("π", "pi")
            .replace("∆", "")
            .replace("<", "")
            .replace(">", "")
            .replace(" ", "")

        // 2. Square root: replace √number with sqrt(number) and √ with sqrt
        expr = expr.replace(Regex("√([0-9]+(?:\\.[0-9]+)?)"), "sqrt($1)")
        expr = expr.replace("√", "sqrt")

        // 3. Implicit multiplication
        expr = expr.replace(Regex("(\\d+)\\("), "$1*(")
        expr = expr.replace(Regex("\\)(\\d+)"), ")*$1")
        expr = expr.replace(Regex("\\)\\("), ")*(")
        expr = expr.replace(Regex("(\\d+)pi"), "$1*pi")
        expr = expr.replace(Regex("(\\d+)sqrt"), "$1*sqrt")
        expr = expr.replace(Regex("\\)pi"), ")*pi")
        expr = expr.replace(Regex("\\)sqrt"), ")*sqrt")

        // 4. Percentage calculations when used with + and -:
        // e.g. 100 + 20% => (100 + (100 * (20 / 100)))
        // e.g. 100 - 20% => (100 - (100 * (20 / 100)))
        expr = expr.replace(Regex("([0-9.]+|pi)\\+([0-9.]+)%($|[+\\-*/^)])")) {
            val a = it.groupValues[1]
            val b = it.groupValues[2]
            val next = it.groupValues[3]
            "($a+($a*($b/100)))$next"
        }
        expr = expr.replace(Regex("([0-9.]+|pi)-([0-9.]+)%($|[+\\-*/^)])")) {
            val a = it.groupValues[1]
            val b = it.groupValues[2]
            val next = it.groupValues[3]
            "($a-($a*($b/100)))$next"
        }

        // 5. Standalone or postfix percentage: e.g. 50% or 100 * 20% (when % is followed by end of string or an operator)
        // Note: When % is directly followed by a number/expression without another operator (e.g. 1000%2), it acts as binary modulo
        expr = expr.replace(Regex("([0-9.]+|pi|sqrt\\([^)]*\\)|\\))%($|[+\\-*/^)])")) {
            val operand = it.groupValues[1]
            val next = it.groupValues[2]
            "($operand/100)$next"
        }

        // 6. Unary minus after operators: e.g. 5 * -3 => 5 * (-3), 10 / -2 => 10 / (-2)
        expr = expr.replace(Regex("([+\\-*/^])-([0-9.]+|pi|sqrt\\([^)]*\\))"), "$1(-$2)")

        // 7. Clean trailing operators for live typing (e.g. "5+", "10/", "8-", "3*", "1000%")
        while (expr.isNotEmpty() && (expr.endsWith("+") || expr.endsWith("-") || expr.endsWith("*") ||
                    expr.endsWith("/") || expr.endsWith("^") || expr.endsWith("%") || expr.endsWith(".") ||
                    expr.endsWith("(") || expr.endsWith("sqrt"))) {
            if (expr.endsWith("sqrt")) {
                expr = expr.substring(0, expr.length - 4)
            } else {
                expr = expr.substring(0, expr.length - 1)
            }
        }

        if (expr.isEmpty()) return ""

        // 8. Auto-close open parentheses for live evaluation
        val openCount = expr.count { it == '(' }
        val closeCount = expr.count { it == ')' }
        if (openCount > closeCount) {
            expr += ")".repeat(openCount - closeCount)
        }

        return expr
    }
}
