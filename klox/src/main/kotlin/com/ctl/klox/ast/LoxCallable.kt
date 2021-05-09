package com.ctl.klox.ast

interface LoxCallable {
    fun call(interpreter: JvmInterpreter, arguments: List<Any?>): Any?
    fun arity(): Int
}

data class LoxFunction(
    val declaration: Stmt.Function,
    val closure: JvmEnvironment,
    val isInitializer: Boolean = false
) : LoxCallable {

    fun bind(loxInstance: LoxInstance): LoxFunction {
        val environment = JvmEnvironment(closure)
        environment.define("this", loxInstance)
        return copy(closure = environment)
    }

    override fun call(interpreter: JvmInterpreter, arguments: List<Any?>): Any? {
        val environment = JvmEnvironment(closure)
        declaration.params.zip(arguments).forEach { (token, arg) ->
            environment.define(token.lexeme, arg)
        }
        try {
            interpreter.executeBlock(declaration.body, environment)
        } catch (returnValue: Return) {
            if(isInitializer){
                return closure.getAt(0, "this")
            }
            return returnValue.value
        }
        if (isInitializer) {
            return closure.getAt(0, "this")
        }
        return null
    }

    override fun arity(): Int = declaration.params.size
    override fun toString(): String {
        return "<fn ${declaration.name.lexeme}>"
    }

}