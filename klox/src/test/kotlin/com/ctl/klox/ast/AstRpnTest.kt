package com.ctl.klox.ast

import org.junit.jupiter.api.Test

internal class AstRpnTest{
    @Test
    internal fun rpn() {
        val ast = AstUtils.arith()
        println(AstRpn().printRpn(ast))
    }
}