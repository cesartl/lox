package com.ctl.klox.ast

import com.ctl.klox.Token
import com.ctl.klox.TokenType

data class TailCallFunction(
    val tailCall: TailCall,
    val condition: Expr?,
    val terminationCalls: List<Stmt>,
    val initialCalls: List<Stmt>
) {
    fun isFor(functionName: String): Boolean {
        val callee = tailCall.call.callee
        return callee is Expr.Variable && callee.name.lexeme == functionName
    }
}

data class TailCall(val call: Expr.Call, val callStmt: Stmt, val preCalls: List<Stmt> = listOf())

object TailRecOptimizer {

    fun extractTailCallFunction(body: List<Stmt>): TailCallFunction? {
        if (body.isEmpty()) {
            return null
        }
        val initial = body.dropLast(1)
        return when (val last = body.last()) {
            is Stmt.If -> {
                val thenTailCall = extractTailCall(last.thenBranch)
                if (thenTailCall != null) {
                    TailCallFunction(
                        tailCall = thenTailCall,
                        condition = last.condition,
                        terminationCalls = last.elseBranch?.let { extractStmts(it) } ?: listOf(),
                        initialCalls = initial
                    )
                } else {
                    val elseTailCall = last.elseBranch?.let { extractTailCall(it) }
                    if (elseTailCall != null) {
                        TailCallFunction(
                            tailCall = elseTailCall,
                            condition = Expr.Unary(Token(TokenType.BANG, "!", null, 0, ), last.condition),
                            terminationCalls = extractStmts(last.thenBranch),
                            initialCalls = initial
                        )
                    } else {
                        null
                    }
                }
            }
            is Stmt.Return, is Stmt.Expression ->{
                extractTailCall(last)?.let { tailCall ->
                    TailCallFunction(
                        tailCall = tailCall,
                        condition = null,
                        terminationCalls = listOf(),
                        initialCalls = initial
                    )
                }
            }
            else -> null
        }
    }

    private fun extractTailCall(stmt: Stmt): TailCall? {
        return when (stmt) {
            is Stmt.Expression -> {
                val expression = stmt.expression
                if (expression is Expr.Call) {
                    TailCall(expression, stmt)
                } else {
                    null
                }
            }
            is Stmt.Return -> {
                val value = stmt.value
                if (value is Expr.Call) {
                    TailCall(value, stmt)
                } else {
                    null
                }
            }
            is Stmt.Block -> {
                val call = stmt.statements.lastOrNull()?.let { extractTailCall(it) }
                call?.copy(callStmt = stmt, preCalls = stmt.statements.dropLast(1) + call.preCalls)
            }
            else -> null
        }
    }

    private fun extractStmts(stmt: Stmt): List<Stmt> = when (stmt) {
        is Stmt.Block -> stmt.statements
        else -> listOf(stmt)
    }
}