package com.ctl.klox.ast

import com.ctl.klox.Token
import com.ctl.klox.TokenType
import org.junit.jupiter.api.Test

internal class AstPrinterTest {
    @Test
    internal fun printAst() {
        val ast = AstUtils.dummyAst()

        println(AstPrinter().print(ast))
    }
}