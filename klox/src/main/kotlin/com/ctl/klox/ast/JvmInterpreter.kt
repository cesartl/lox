package com.ctl.klox.ast

import com.ctl.klox.Lox
import com.ctl.klox.Token
import com.ctl.klox.TokenType.*
import java.util.*

class JvmInterpreter() {

    fun interpret(statements: List<Stmt>) {
        try {
            statements.forEach {
                execute(it)
            }
        } catch (e: RuntimeError) {
            Lox.runtimeError(e)
        }
    }

    private fun execute(stmt: Stmt) {
        when (stmt) {
            is Stmt.Expression -> {
                evaluate(stmt.expression)
            }
            is Stmt.Print -> {
                val value = evaluate(stmt.expression)
                println(stringify(value))
            }
        }

    }

    private fun evaluate(expr: Expr): Any? {
        return when (expr) {
            is Expr.Literal -> expr.value
            is Expr.Grouping -> evaluate(expr.expression)
            is Expr.Unary -> {
                val right = evaluate(expr.right)
                when (expr.operator.type) {
                    MINUS -> {
                        checkNumberOperand(expr.operator, right)
                        -(right as Double)
                    }
                    BANG -> !isTruthy(right)
                    else -> null
                }
            }
            is Expr.Binary -> {
                val left = evaluate(expr.left)
                val right = evaluate(expr.right)
                when (expr.operator.type) {
                    MINUS -> {
                        checkNumberOperands(expr.operator, left, right)
                        (left as Double) - (right as Double)
                    }
                    SLASH -> {
                        checkNumberOperands(expr.operator, left, right)
                        (left as Double) / (right as Double)
                    }
                    STAR -> {
                        checkNumberOperands(expr.operator, left, right)
                        (left as Double) * (right as Double)
                    }
                    PLUS -> {
                        if (left is String || right is String) {
                            stringify(left) + stringify(right)
                        } else {
                            checkNumberOperands(expr.operator, left, right)
                            (left as Double) + (right as Double)
                        }
                    }
                    GREATER -> {
                        checkNumberOperands(expr.operator, left, right)
                        (left as Double) > (right as Double)
                    }
                    GREATER_EQUAL -> {
                        checkNumberOperands(expr.operator, left, right)
                        (left as Double) >= (right as Double)
                    }
                    LESS -> {
                        checkNumberOperands(expr.operator, left, right)
                        (left as Double) < (right as Double)
                    }
                    LESS_EQUAL -> {
                        checkNumberOperands(expr.operator, left, right)
                        (left as Double) <= (right as Double)
                    }
                    BANG_EQUAL -> !isEqual(left, right)
                    EQUAL_EQUAL -> isEqual(left, right)
                    else -> null
                }
            }
        }
    }

    private fun stringify(any: Any?): String {
        return when (any) {
            null -> "nil"
            is Double -> {
                val text = any.toString()
                if (text.endsWith(".0")) {
                    text.substring(0, text.length - 2)
                } else {
                    text
                }
            }
            else -> any.toString()
        }
    }

    private fun checkNumberOperand(operator: Token, operand: Any?) {
        if (operand !is Double) {
            throw RuntimeError(operator, "Operand must be a number.")
        }
    }

    private fun checkNumberOperands(operator: Token, left: Any?, right: Any?) {
        if (!(left is Number && right is Number)) {
            throw RuntimeError(operator, "Operands must be numbers.")
        }
    }

    private fun isEqual(a: Any?, b: Any?): Boolean {
        return Objects.equals(a, b)
    }

    private fun isTruthy(value: Any?): Boolean {
        return when (value) {
            null -> false
            is Boolean -> value
            else -> true
        }
    }

}

class RuntimeError(val token: Token, message: String) : RuntimeException(message)