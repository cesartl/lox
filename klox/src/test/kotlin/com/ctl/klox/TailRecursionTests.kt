package com.ctl.klox

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

class TailRecursionTests {

    private val case1 = """
var x = 0;
var y = 0;
var z = 0;        
fun foo(acc, n){
  x = x + 1;
  if(n == 0){
    y = y + 1;
    return acc;
  }
  z = z + 1;
  return foo(2 * acc, n-1);
}
var result = foo(1, 10);
        """.trimIndent()

    private val case2 = """
var x = 0;
var y = 0;
var z = 0;
fun foo(acc, n){
  x = x + 1;
  if(n == 0){
    y = y + 1;
    return acc;
  }else{
    z = z + 1;
    return foo(2 * acc, n-1);
  }
}
var result = foo(1, 10);
        """.trimIndent()

    private val case3 = """
var x = 0;
var y = 0;
var z = 0;
fun foo(acc, n){
  x = x + 1;
  if(n > 0){
    z = z + 1;
    return foo(2 * acc, n-1);
  }else{
    y = y + 1;
    return acc;
  }
}
var result = foo(1, 10);
        """.trimIndent()

    private val case4 = """
var x = 0;
var y = 0;
var z = 0;        
fun foo(acc, n){
  x = x + 1;
  if(n == 0){
    y = y + 1;
    return acc;
  }
  z = z + 1;
  foo(2 * acc, n-1);
}
var result = foo(1, <iter>);
        """.trimIndent()





    @Test
    fun case1() {
        val expected = mapOf("x" to 11.0, "y" to 1.0, "z" to 10.0, "result" to 1024.0)
        checkCase(expected, case1)
    }

    @Test
    fun case2() {
        val expected = mapOf("x" to 11.0, "y" to 1.0, "z" to 10.0, "result" to 1024.0)
        checkCase(expected, case2)
    }

    @Test
    fun case3() {
        val expected = mapOf("x" to 11.0, "y" to 1.0, "z" to 10.0, "result" to 1024.0)
        checkCase(expected, case3)
    }

    @Test
    fun case4() {
        val expected = mapOf("x" to 11.0, "y" to 1.0, "z" to 10.0)
        checkCase(expected, case4.replace("<iter>", "10"))
    }

    @Test
    fun case4High() {
        val expected = mapOf("x" to 2001.0, "y" to 1.0, "z" to 2000.0)
        val source = case4.replace("<iter>", "2000")
        assertThrows(StackOverflowError::class.java) {
            assertResult(source, expected)
        }
        assertResult(source.replace("fun foo", "tailrec fun foo"), expected)
    }

    private fun checkCase(expected: Map<String, Double>, source: String) {
        assertResult(source, expected)
        assertResult(source.replace("fun foo", "tailrec fun foo"), expected)
    }

    private fun assertResult(source: String, expected: Map<String, Double>) {
        val env = Lox.debug(source)
        expected.forEach { (key, value) ->
            assertEquals(value, env[key], "Variable $key")
        }
    }
}