package com.ctl.klox

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class LoxTest {

    @Test
    fun tailRec() {
        val script = """
            tailrec fun loop(n) {
                if(n > 0) loop(n-1);
            }
            loop(2000);
        """.trimIndent()
        Lox.run(script)
    }
}