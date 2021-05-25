package com.ctl.klox.ast

data class LoxClass(val name: String, val superclass: LoxClass?, val methods: MutableMap<String, LoxFunction>) : LoxCallable{
    override fun call(interpreter: JvmInterpreter, arguments: List<Any?>): Any {
        val instance = LoxInstance(this)
        findMethod("init")?.bind(instance)?.call(interpreter, arguments)
        return instance
    }

    fun findMethod(name: String): LoxFunction? {
        return methods[name] ?: superclass?.findMethod(name)
    }

    override fun arity(): Int {
        return findMethod("init")?.arity() ?: 0
    }

    override fun toString(): String {
        return name
    }
}