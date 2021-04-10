package com.ctl.klox.ast

interface LoxCallable {
    fun call(interpreter: JvmInterpreter, arguments: List<Any?>): Any?
    fun arity(): Int
}

data class LoxFunction(val declaration: Stmt.Function, val closure: JvmEnvironment) : LoxCallable {
    override fun call(interpreter: JvmInterpreter, arguments: List<Any?>): Any? {
        val environment = JvmEnvironment(closure)
        declaration.params.zip(arguments).forEach { (token, arg) ->
            environment.define(token.lexeme, arg)
        }
        try {
            interpreter.executeBlock(declaration.body, environment)
        } catch (returnValue: Return) {
            return returnValue.value
        }
        return null
    }

    override fun arity(): Int = declaration.params.size
    override fun toString(): String {
        return "<fn ${declaration.name.lexeme}>"
    }


}