package com.ctl.klox.ast

import com.ctl.klox.Lox
import com.ctl.klox.Token
import com.ctl.klox.TokenType.*
import java.time.Instant
import java.util.*

class JvmInterpreter {

    private val locals = mutableMapOf<Expr, Int>()
    private val globals = JvmEnvironment()
    private var environment = globals

    init {
        globals.define("clock", object : LoxCallable {
            override fun call(interpreter: JvmInterpreter, arguments: List<Any?>): Any {
                return Instant.now().epochSecond.toDouble()
            }

            override fun arity(): Int = 0
        })
    }

    fun debug(statements: List<Stmt>): Map<String, Any?> {
        interpret(statements)
        return environment.bindings()
    }

    fun interpret(statements: List<Stmt>) {
        try {
            statements.forEach {
                execute(it)
            }
        } catch (e: RuntimeError) {
            Lox.runtimeError(e)
        }
    }

    fun execute(stmt: Stmt) {
        when (stmt) {
            is Stmt.Expression -> {
                evaluate(stmt.expression)
            }
            is Stmt.Print -> {
                val value = evaluate(stmt.expression)
                println(stringify(value))
            }
            is Stmt.Var -> {
                stmt.initializer?.let {
                    environment.define(stmt.name.lexeme, evaluate(it))
                } ?: environment.define(stmt.name.lexeme)
            }
            is Stmt.Block -> executeBlock(stmt.statements, JvmEnvironment(environment))
            is Stmt.If -> {
                if (isTruthy(evaluate(stmt.condition))) {
                    execute(stmt.thenBranch)
                } else if (stmt.elseBranch != null) {
                    execute(stmt.elseBranch)
                }
            }
            is Stmt.While -> {
                while (isTruthy(evaluate(stmt.condition))) {
                    execute(stmt.body)
                }
            }
            is Stmt.Function -> {
                val function = LoxFunction(stmt, environment)
                environment.define(stmt.name.lexeme, function)
            }
            is Stmt.Return -> {
                throw Return(stmt.value?.let { evaluate(it) })
            }
            is Stmt.Class -> {
                val superclass = stmt.superclass?.let {
                    val superclass = evaluate(stmt.superclass)
                    if (superclass !is LoxClass) {
                        throw RuntimeError(it.name, "Superclass must be a class.")
                    }
                    superclass
                }
                environment.define(stmt.name.lexeme, null)

                if (superclass != null) {
                    environment = JvmEnvironment(environment)
                    environment.define("super", superclass)
                }

                val methods = stmt.methods.fold(mutableMapOf<String, LoxFunction>()) { acc, f ->
                    val isInitializer = f.name.lexeme == "init"
                    acc[f.name.lexeme] = LoxFunction(f, environment, isInitializer)
                    acc
                }
                val klass = LoxClass(stmt.name.lexeme, superclass, methods)
                if(superclass != null){
                    environment = environment.enclosing!!
                }
                environment.assign(stmt.name, klass)
            }
        }

    }

    fun evaluate(expr: Expr): Any? {
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
            is Expr.Variable -> lookupVariable(expr.name, expr)
            is Expr.Assign -> {
                val value = evaluate(expr.value)
                locals[expr]?.let { distance ->
                    environment.assignAt(distance, expr.name, value)
                } ?: globals.assign(expr.name, value)
                return value
            }
            is Expr.Logical -> {
                val left = evaluate(expr.left)
                return when (expr.operator.type) {
                    OR -> if (isTruthy(left)) left else evaluate(expr.right)
                    AND -> if (!isTruthy(left)) left else evaluate(expr.right)
                    else -> null
                }
            }
            is Expr.Call -> {
                val callee = evaluate(expr.callee)
                val arguments = expr.arguments.map { evaluate(it) }
                return when (callee) {
                    is LoxCallable -> {
                        if (arguments.size != callee.arity()) {
                            throw RuntimeError(
                                expr.paren,
                                "Expected ${callee.arity()} arguments but got ${arguments.size}."
                            )
                        }
                        callee.call(this, arguments)
                    }
                    else -> throw RuntimeError(
                        expr.paren,
                        "Can only call function and classes: ${expr.callee}:${callee?.javaClass}"
                    )
                }
            }
            is Expr.Get -> {
                when (val obj = evaluate(expr.targetObject)) {
                    is LoxInstance -> obj.get(expr.name)
                    else -> throw RuntimeError(expr.name, "Only instances have properties.")
                }
            }
            is Expr.Set -> {
                when (val obj = evaluate(expr.targetObject)) {
                    is LoxInstance -> obj.set(expr.name, evaluate(expr.value))
                    else -> throw RuntimeError(expr.name, "Only instances have properties.")
                }
            }
            is Expr.This -> lookupVariable(expr.keyword, expr)
            is Expr.Super -> {
                val distance = locals[expr] ?: throw RuntimeError(expr.keyword, "Could not resolve super")
                val superclass = environment.getAt(distance, "super") as LoxClass
                val instance = environment.getAt(distance - 1, "this") as LoxInstance
                val method = superclass.findMethod(expr.method.lexeme)
                    ?: throw RuntimeError(
                        expr.keyword,
                        "Could not find method ${expr.method.lexeme} on superclass ${superclass.name}"
                    )
                return method.bind(instance)
            }
        }
    }

    private fun lookupVariable(name: Token, expr: Expr): Any? {
        return locals[expr]?.let { distance ->
            environment.getAt(distance, name.lexeme)
        } ?: globals.get(name)
    }

    fun executeBlock(statements: List<Stmt>, environment: JvmEnvironment) {
        val previous = this.environment
        try {
            this.environment = environment
            statements.forEach { execute(it) }
        } finally {
            this.environment = previous
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

    fun resolve(expr: Expr, depth: Int) {
        locals[expr] = depth
    }

}

class RuntimeError(val token: Token, message: String) : RuntimeException(message)
data class Return(val value: Any?) : RuntimeException(null, null, false, false)