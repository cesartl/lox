package com.ctl.klox.ast

import java.lang.StringBuilder

class AstPrinter {
    fun print(expr: Expr): String = when (expr) {
        is Expr.Binary -> parenthesize(expr.operator.lexeme, expr.left, expr.right)
        is Expr.Grouping -> parenthesize("group", expr.expression)
        is Expr.Literal -> expr.value?.toString() ?: "nil"
        is Expr.Unary -> parenthesize(expr.operator.lexeme, expr.right)
        else -> TODO(expr.toString())
    }

    private fun parenthesize(name: String, vararg exprs: Expr): String {
        val builder = exprs.fold(StringBuilder("(").append(name)) { acc, expr ->
            acc.append(" ")
            acc.append(print(expr))
            acc
        }
        builder.append(")")
        return builder.toString()
    }
}