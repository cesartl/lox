package com.ctl.klox.ast

import com.ctl.klox.Token

class JvmEnvironment(val enclosing: JvmEnvironment? = null) {
    private val values = mutableMapOf<String, Any?>()

    fun define(name: String, value: Any? = Special.UNDEFINED) {
        values[name] = value
    }

    fun get(name: Token): Any? {
        if (values.contains(name.lexeme)) {
            val value = values[name.lexeme]
            if (value == Special.UNDEFINED) {
                throw RuntimeError(name, "Variable '${name.lexeme}' is not initialized.")
            }
            return value
        }
//        if (enclosing != null) {
//            return enclosing.get(name)
//        }
        throw RuntimeError(name, "Undefined variable '${name.lexeme}[l${name.line}]'.")
    }

    fun getAt(distance: Int, name: String): Any? {
        return ancestor(distance).values[name]
    }

    fun assign(name: Token, value: Any?) {
        when {
            values.containsKey(name.lexeme) -> {
                values[name.lexeme] = value
            }
//            enclosing != null -> {
//                enclosing.assign(name, value)
//            }
            else -> {
                throw RuntimeError(name, "Undefined variable '${name.lexeme}'.")
            }
        }
    }

    fun assignAt(distance: Int, name: Token, value: Any?){
        ancestor(distance).values.put(name.lexeme, value)
    }

    private fun ancestor(distance: Int): JvmEnvironment {
        return generateSequence(this) { it.enclosing }.drop(distance).first()
    }

    fun bindings(): Map<String, Any?> {
        val previous = enclosing?.let { bindings() } ?: mapOf()
        return values + previous
    }

    companion object {
        enum class Special {
            UNDEFINED
        }
    }
}