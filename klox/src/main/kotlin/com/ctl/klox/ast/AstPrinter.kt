package com.ctl.klox.ast

import java.lang.StringBuilder

class AstPrinter {
    fun printExpr(expr: Expr): String = when (expr) {
        is Expr.Binary -> parenthesize(expr.operator.lexeme, expr.left, expr.right)
        is Expr.Grouping -> parenthesize("group", expr.expression)
        is Expr.Literal -> expr.value?.toString() ?: "nil"
        is Expr.Unary -> parenthesize(expr.operator.lexeme, expr.right)
        is Expr.Assign -> "${expr.name.lexeme} = ${printExpr(expr.value)}"
        is Expr.Call -> "${printExpr(expr.callee)}(${
            expr.arguments.joinToString(",") {
                printExpr(
                    it
                )
            }
        })"
        is Expr.Logical -> printExpr(expr.left) + expr.operator.lexeme + printExpr(expr.right)
        is Expr.Variable -> "${expr.name.lexeme}[l${expr.name.line}]"
    }

    fun printStmts(stmts: List<Stmt>, indent: String = ""): String {
        val builder = StringBuilder()
        stmts.forEach {
            builder.append(indent + printStmt(it, indent))
            builder.append("\n")
        }
        return builder.toString()
    }

    fun printStmt(stmt: Stmt, indent: String): String = when (stmt) {
        is Stmt.Block -> "{\n${printStmts(stmt.statements, indent + "\t")}$indent}"
        is Stmt.Expression -> printExpr(stmt.expression)
        is Stmt.Function -> "${stmt.name.lexeme}(${stmt.params.joinToString(",") { it.lexeme }}) ${
            printStmts(
                stmt.body,
                indent + "\t"
            )
        }"
        is Stmt.If -> "if(${printExpr(stmt.condition)}) ${printStmt(stmt.thenBranch, indent)} else ${
            stmt.elseBranch?.let { printStmt(it, indent) }
        }"
        is Stmt.Print -> "print ${printExpr(stmt.expression)}"
        is Stmt.Return -> "return ${stmt.value?.let { printExpr(it) }}"
        is Stmt.Var -> "var ${stmt.name.lexeme}${stmt.initializer?.let { "=" + printExpr(it) }}"
        is Stmt.While -> "while(${printExpr(stmt.condition)}) ${printStmt(stmt.body, indent)}"
    }

    private fun parenthesize(name: String, vararg exprs: Expr): String {
        val builder = exprs.fold(StringBuilder("(").append(name)) { acc, expr ->
            acc.append(" ")
            acc.append(printExpr(expr))
            acc
        }
        builder.append(")")
        return builder.toString()
    }
}