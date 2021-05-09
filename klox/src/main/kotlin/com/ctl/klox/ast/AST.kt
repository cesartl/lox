package com.ctl.klox.ast

import com.ctl.klox.Token

sealed class Expr {
    data class Assign(val name: Token, val value: Expr) : Expr()
    data class Binary(val left: Expr, val operator: Token, val right: Expr) : Expr()
    data class Call(val callee: Expr, val paren: Token, val arguments: List<Expr>) : Expr()
    data class Get(val targetObject: Expr, val name: Token) : Expr()
    data class Grouping(val expression: Expr) : Expr()
    data class Literal(val value: Any?) : Expr()
    data class Logical(val left: Expr, val operator: Token, val right: Expr) : Expr()
    data class Set(val targetObject: Expr, val name: Token, val value: Expr) : Expr()
    data class This( val keyword: Token) : Expr()
    data class Unary(val operator: Token, val right: Expr) : Expr()
    data class Variable(val name: Token) : Expr()
}

sealed class Stmt {
    data class Block(val statements: List<Stmt>) : Stmt()
    data class Class(val name: Token, val methods: List<Function>) : Stmt()
    data class Expression(val expression: Expr) : Stmt()
    data class Function(val name: Token, val params: List<Token>, val body: List<Stmt>) : Stmt()
    data class If(val condition: Expr, val thenBranch: Stmt, val elseBranch: Stmt?) : Stmt()
    data class Print(val expression: Expr) : Stmt()
    data class Return(val keyword: Token, val value: Expr?) : Stmt()
    data class Var(val name: Token, val initializer: Expr?) : Stmt()
    data class While(val condition: Expr, val body: Stmt) : Stmt()
}

fun Expr.lineOffset(offset: Int): Expr = when (this) {
    is Expr.Assign -> this.copy(name = this.name.lineOffset(offset), value = this.value.lineOffset(offset))
    is Expr.Binary -> this.copy(left.lineOffset(offset), operator.lineOffset(offset), right.lineOffset(offset))
    is Expr.Call -> this.copy(
        callee.lineOffset(offset),
        paren.lineOffset(offset),
        arguments.map { it.lineOffset(offset) })
    is Expr.Get -> this.copy(name = name.lineOffset(offset), targetObject = targetObject.lineOffset(offset))
    is Expr.Grouping -> this.copy(expression.lineOffset(offset))
    is Expr.Literal -> this
    is Expr.Logical -> this.copy(left.lineOffset(offset), operator.lineOffset(offset), right.lineOffset(offset))
    is Expr.Set -> this.copy(
        name = name.lineOffset(offset),
        targetObject = targetObject.lineOffset(offset),
        value = value.lineOffset(offset)
    )
    is Expr.Unary -> this.copy(operator.lineOffset(offset), right.lineOffset(offset))
    is Expr.Variable -> this.copy(name.lineOffset(offset))
    is Expr.This -> this.copy(keyword = keyword.lineOffset(offset))
}

fun Stmt.lineOffset(offset: Int): Stmt = when(this){
    is Stmt.Block -> this.copy(statements.map { it.lineOffset(offset) })
    is Stmt.Expression -> this.copy(expression.lineOffset(offset))
    is Stmt.Function -> this.copy(
        name.lineOffset(offset),
        params.map { it.lineOffset(offset) },
        body.map { it.lineOffset(offset) })
    is Stmt.If -> this.copy(condition.lineOffset(offset), thenBranch.lineOffset(offset), elseBranch?.lineOffset(offset))
    is Stmt.Print -> this.copy(expression.lineOffset(offset))
    is Stmt.Return -> this.copy(keyword.lineOffset(offset), value?.lineOffset(offset))
    is Stmt.Var -> this.copy(name.lineOffset(offset), initializer?.lineOffset(offset))
    is Stmt.While -> this.copy(condition.lineOffset(offset), body.lineOffset(offset))
    is Stmt.Class -> this.copy(methods = methods.map { it.lineOffset(offset) as Stmt.Function })
}