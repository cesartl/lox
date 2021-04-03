package com.ctl.klox.ast

import com.ctl.klox.Token

sealed class Expr {
    data class Assign(val name: Token, val value: Expr) : Expr()
    data class Binary(val left: Expr, val operator: Token, val right: Expr) : Expr()
    data class Call(val callee: Expr, val paren: Token, val arguments: List<Expr>) : Expr()
    data class Grouping(val expression: Expr) : Expr()
    data class Literal(val value: Any?) : Expr()
    data class Logical(val left: Expr, val operator: Token, val right: Expr) : Expr()
    data class Unary(val operator: Token, val right: Expr) : Expr()
    data class Variable(val name: Token) : Expr()
}

sealed class Stmt {
    data class Block(val statements: List<Stmt>) : Stmt()
    data class Expression(val expression: Expr) : Stmt()
    data class Function(val name: Token, val params: List<Token>, val body: List<Stmt>): Stmt()
    data class If(val condition: Expr, val thenBranch: Stmt, val elseBranch: Stmt?) : Stmt()
    data class Print(val expression: Expr) : Stmt()
    data class Var(val name: Token, val initializer: Expr?) : Stmt()
    data class While(val condition: Expr, val body: Stmt) : Stmt()
}