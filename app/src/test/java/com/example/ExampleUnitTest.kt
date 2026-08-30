package com.example

import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun mathEvaluator_standardOperations() {
    // Addition
    assertEquals(30.0, MathEvaluator.evaluate("10 + 20")!!, 0.0001)
    assertEquals(10.0, MathEvaluator.evaluate("5 + 3 + 2")!!, 0.0001)

    // Subtraction
    assertEquals(6.0, MathEvaluator.evaluate("10 - 4")!!, 0.0001)
    assertEquals(12.0, MathEvaluator.evaluate("20 - 5 - 3")!!, 0.0001)

    // Multiplication
    assertEquals(50.0, MathEvaluator.evaluate("10 × 5")!!, 0.0001)
    assertEquals(24.0, MathEvaluator.evaluate("3 × 4 × 2")!!, 0.0001)

    // Division
    assertEquals(5.0, MathEvaluator.evaluate("10 ÷ 2")!!, 0.0001)
    assertEquals(25.0, MathEvaluator.evaluate("100 ÷ 4")!!, 0.0001)
    assertEquals(3.5, MathEvaluator.evaluate("7 ÷ 2")!!, 0.0001)

    // Operator Precedence (BODMAS / PEMDAS)
    assertEquals(14.0, MathEvaluator.evaluate("2 + 3 × 4")!!, 0.0001)
    assertEquals(16.0, MathEvaluator.evaluate("(5 + 3) × 2")!!, 0.0001)
    assertEquals(40.0, MathEvaluator.evaluate("50 - 20 ÷ 2")!!, 0.0001)
    assertEquals(8.0, MathEvaluator.evaluate("2 ^ 3")!!, 0.0001)
    assertEquals(3.0, MathEvaluator.evaluate("√9")!!, 0.0001)
    assertEquals(5.0, MathEvaluator.evaluate("√25")!!, 0.0001)
  }

  @Test
  fun mathEvaluator_negativeNumbers() {
    assertEquals(5.0, MathEvaluator.evaluate("-5 + 10")!!, 0.0001)
    assertEquals(-15.0, MathEvaluator.evaluate("5 × -3")!!, 0.0001)
    assertEquals(-5.0, MathEvaluator.evaluate("10 ÷ -2")!!, 0.0001)
    assertEquals(-15.0, MathEvaluator.evaluate("-10 - 5")!!, 0.0001)
  }

  @Test
  fun mathEvaluator_percentagesAndPi() {
    assertEquals(0.5, MathEvaluator.evaluate("50%")!!, 0.0001)
    assertEquals(20.0, MathEvaluator.evaluate("100 × 20%")!!, 0.0001)
    assertEquals(120.0, MathEvaluator.evaluate("100 + 20%")!!, 0.0001)
    assertEquals(80.0, MathEvaluator.evaluate("100 - 20%")!!, 0.0001)
    assertEquals(Math.PI * 2, MathEvaluator.evaluate("2π")!!, 0.0001)
    assertEquals(Math.PI * 2, MathEvaluator.evaluate("π × 2")!!, 0.0001)

    // Modulo / Remainder operations (e.g. 1000%2, 10%3)
    assertEquals(0.0, MathEvaluator.evaluate("1000%2")!!, 0.0001)
    assertEquals(0.0, MathEvaluator.evaluate("1000 % 2")!!, 0.0001)
    assertEquals(1.0, MathEvaluator.evaluate("10%3")!!, 0.0001)
    assertEquals(2.0, MathEvaluator.evaluate("17 % 5")!!, 0.0001)
    assertEquals(0.0, MathEvaluator.evaluate("1000000 % 100")!!, 0.0001)
  }

  @Test
  fun mathEvaluator_complexExpressions() {
    assertEquals(25.0, MathEvaluator.evaluate("3 + 4 × 5 + (10 ÷ 5)")!!, 0.0001)
    assertEquals(100.0, MathEvaluator.evaluate("(2 + 3) ^ 2 × 4")!!, 0.0001)
    assertEquals(7.0, MathEvaluator.evaluate("√49")!!, 0.0001)
    assertEquals(10.0, MathEvaluator.evaluate("√100")!!, 0.0001)
    assertEquals(13.0, MathEvaluator.evaluate("3 + √100")!!, 0.0001)
    assertEquals(30.0, MathEvaluator.evaluate("3 × √100")!!, 0.0001)
  }

  @Test
  fun mathEvaluator_liveTypingRecovery() {
    // Unclosed parenthesis during typing
    assertEquals(8.0, MathEvaluator.evaluate("(5 + 3")!!, 0.0001)
    // Trailing operators during typing
    assertEquals(5.0, MathEvaluator.evaluate("5 + ")!!, 0.0001)
    assertEquals(10.0, MathEvaluator.evaluate("10 × ")!!, 0.0001)
    assertEquals(10.0, MathEvaluator.evaluate("10 ÷ ")!!, 0.0001)
    assertEquals(10.0, MathEvaluator.evaluate("10 - ")!!, 0.0001)
  }
}
