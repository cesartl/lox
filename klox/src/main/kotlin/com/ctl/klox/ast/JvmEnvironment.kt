package com.ctl.klox.ast

import com.ctl.klox.Token

class JvmEnvironment(private val enclosing: JvmEnvironment? = null) {
    private val values = mutableMapOf<String, Any?>()

    fun define(name: String, value: Any?) {
        values[name] = value
    }

    fun get(name: Token): Any? {
        if (values.contains(name.lexeme)) {
            return values[name.lexeme]
        }
        if (enclosing != null) {
            return enclosing.get(name)
        }
        throw RuntimeError(name, "Undefined variable '${name.lexeme}'.")
    }

    fun assign(name: Token, value: Any?) {
        when {
            values.containsKey(name.lexeme) -> {
                values[name.lexeme] = value
            }
            enclosing != null -> {
                enclosing.assign(name, value)
            }
            else -> {
                throw RuntimeError(name, "Undefined variable '${name.lexeme}'.")
            }
        }
    }
}