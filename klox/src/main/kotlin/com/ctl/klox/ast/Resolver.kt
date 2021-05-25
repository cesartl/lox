package com.ctl.klox.ast

import com.ctl.klox.Lox
import com.ctl.klox.Token
import java.util.*

class Resolver(private val interpreter: JvmInterpreter) {

    private val scopes = Stack<MutableMap<String, Boolean>>()
    var currentFunction = FunctionType.NONE
    var currentClass = ClassType.NONE

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
                if (currentFunction == FunctionType.NONE) {
                    Lox.error(stmt.keyword, "Can't return from top-level code.")
                }
                stmt.value?.let {
                    if (currentFunction == FunctionType.INITIALIZER) {
                        Lox.error(stmt.keyword, "Can't return a value from an initializer.")
                    }
                    resolve(it)
                }
            }
            is Stmt.While -> {
                resolve(stmt.condition)
                resolve(stmt.body)
            }
            is Stmt.Class -> {
                val enclosingClass = currentClass
                currentClass = ClassType.CLASS
                declare(stmt.name)
                define(stmt.name)

                stmt.superclass?.let { superclass ->
                    if (superclass.name.lexeme == stmt.name.lexeme) {
                        Lox.error(stmt.superclass.name, "A class can't inherit from itself")
                    }
                    currentClass = ClassType.SUBCLASS
                    this.resolve(superclass)
                    beginScope()
                    scopes.peek()["super"] = true
                }

                beginScope()
                scopes.peek()["this"] = true


                stmt.methods.forEach { method ->
                    var declaration = FunctionType.METHOD
                    if (method.name.lexeme == "init") {
                        declaration = FunctionType.INITIALIZER
                    }
                    resolveFunction(method, declaration)
                }

                endScope()

                if (stmt.superclass != null) {
                    endScope()
                }

                currentClass = enclosingClass
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
            is Expr.Literal -> {
            }
            is Expr.Logical -> {
                resolve(expr.left)
                resolve(expr.right)
            }
            is Expr.Unary -> resolve(expr.right)
            is Expr.Get -> resolve(expr.targetObject)
            is Expr.Set -> {
                resolve(expr.value)
                resolve(expr.targetObject)
            }
            is Expr.This -> {
                if (currentClass != ClassType.CLASS) {
                    Lox.error(
                        expr.keyword,
                        "Can't use 'this' outside of a class."
                    )
                    return
                }
                resolveLocal(expr, expr.keyword)
            }
            is Expr.Super -> {
                when (currentClass) {
                    ClassType.NONE -> Lox.error(
                        expr.keyword,
                        "Can't use 'super' outside of a class."
                    )
                    ClassType.CLASS -> Lox.error(
                        expr.keyword,
                        "Can't use 'super' in a class with no superclass."
                    )
                    ClassType.SUBCLASS -> {
                    }
                }
                resolveLocal(expr, expr.keyword)
            }
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
            if (scope.containsKey(name.lexeme)) {
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

enum class FunctionType {
    NONE, FUNCTION, METHOD, INITIALIZER
}

enum class ClassType {
    NONE, CLASS, SUBCLASS
}
