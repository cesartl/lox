package com.ctl.klox.ast

import org.junit.jupiter.api.Test

internal class AstPrinterTest {
    @Test
    internal fun printAst() {
        val ast = AstUtils.dummyAst()

        println(AstPrinter().printExpr(ast))
    }
}