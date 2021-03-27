package com.ctl.klox.ast

import java.lang.StringBuilder

class AstRpn {
    fun printRpn(expr: Expr): String = when (expr) {
        is Expr.Binary -> rpn(expr.operator.lexeme, expr.left, expr.right)
        is Expr.Grouping -> printRpn(expr.expression)
        is Expr.Literal -> expr.value?.toString() ?: "nil"
        is Expr.Unary -> rpn(expr.operator.lexeme, expr.right)
        else -> TODO(expr.toString())
    }

    private fun rpn(operation: String, vararg exprs: Expr): String {
        return exprs.fold(StringBuilder("")) { acc, expr ->
            acc.append(printRpn(expr))
            acc.append(" ")
            acc
        }.append(operation).toString()
    }
}