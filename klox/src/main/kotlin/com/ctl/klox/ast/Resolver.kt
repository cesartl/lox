package com.ctl.klox.ast

import com.ctl.klox.Lox
import com.ctl.klox.Token
import java.util.*

class Resolver(private val interpreter: JvmInterpreter) {

    private val scopes = Stack<MutableMap<String, Boolean>>()
    var currentFunction = FunctionType.NONE

    fun resolve(statements: List<Stmt>) {
        statements.forEach { resolve(it) }
    }

    fun resolve(stmt: Stmt) {
        when (stmt) {
            is Stmt.Block -> {
                beginScope()
                resolve(stmt.statements)
                endScope()
            }
            is Stmt.Var -> {
                declare(stmt.name)
                stmt.initializer?.let {
                    resolve(it)
                }
                define(stmt.name)
            }
            is Stmt.Function -> {
                declare(stmt.name)
                define(stmt.name)
                resolveFunction(stmt, FunctionType.FUNCTION)
            }
            is Stmt.Expression -> resolve(stmt.expression)
            is Stmt.If -> {
                resolve(stmt.condition)
                resolve(stmt.thenBranch)
                stmt.elseBranch?.let { resolve(it) }
            }
            is Stmt.Print -> resolve(stmt.expression)
            is Stmt.Return -> {
                if(currentFunction == FunctionType.NONE){
                    Lox.error(stmt.keyword, "Can't return from top-level code.")
                }
                stmt.value?.let { resolve(it) }
            }
            is Stmt.While -> {
                resolve(stmt.condition)
                resolve(stmt.body)
            }
        }
    }

    private fun resolve(expr: Expr) {
        when (expr) {
            is Expr.Variable -> {
                if (scopes.isNotEmpty() && scopes.peek()[expr.name.lexeme] == false) {
                    Lox.error(expr.name, "Can't read local variable in its own initializer.")
                }
                resolveLocal(expr, expr.name)
            }
            is Expr.Assign -> {
                resolve(expr.value)
                resolveLocal(expr, expr.name)
            }
            is Expr.Binary -> {
                resolve(expr.left)
                resolve(expr.right)
            }
            is Expr.Call -> {
                resolve(expr.callee)
                expr.arguments.forEach {
                    resolve(it)
                }
            }
            is Expr.Grouping -> resolve(expr.expression)
            is Expr.Literal -> {}
            is Expr.Logical -> {
                resolve(expr.left)
                resolve(expr.right)
            }
            is Expr.Unary -> resolve(expr.right)
        }
    }

    private fun resolveFunction(function: Stmt.Function, type: FunctionType) {
        val enclosingFunction = currentFunction
        currentFunction = type
        beginScope()
        function.params.forEach {
            declare(it)
            define(it)
        }
        resolve(function.body)
        endScope()
        currentFunction = enclosingFunction
    }

    private fun resolveLocal(expr: Expr, name: Token) {
        for (i in scopes.size - 1 downTo 0) {
            if (scopes[i].containsKey(name.lexeme)) {
                interpreter.resolve(expr, scopes.size - 1 - i)
                return
            }
        }
    }

    private fun declare(name: Token) {
        if (scopes.isNotEmpty()) {
            val scope = scopes.peek()
            if(scope.containsKey(name.lexeme)){
                Lox.error(name, "Already variable with this name in this scope.")
            }
            scope[name.lexeme] = false
        }
    }

    private fun define(name: Token) {
        if (scopes.isNotEmpty()) {
            scopes.peek()[name.lexeme] = true
        }
    }

    private fun beginScope() {
        scopes.push(mutableMapOf())
    }

    private fun endScope() {
        scopes.pop()
    }

}

enum class FunctionType{
    NONE, FUNCTION
}