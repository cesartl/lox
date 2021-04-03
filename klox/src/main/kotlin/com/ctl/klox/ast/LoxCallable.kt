package com.ctl.klox.ast

interface LoxCallable {
    fun call(interpreter: JvmInterpreter, arguments: List<Any?>): Any?
    fun arity(): Int
}