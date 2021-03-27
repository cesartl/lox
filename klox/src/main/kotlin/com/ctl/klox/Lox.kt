package com.ctl.klox

import com.ctl.klox.ast.JvmInterpreter
import com.ctl.klox.ast.RuntimeError
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.system.exitProcess


fun main(args: Array<String>) {
    when {
        args.size > 1 -> {
            println("Usage: klox [script]")
            exitProcess(64)
        }
        args.size == 1 -> {
            Lox.runFile(args[0])
        }
        else -> {
            Lox.runPrompt()
        }
    }
}

object Lox {

    private val interpreter = JvmInterpreter()

    fun runFile(path: String) {
        val bytes = Files.readAllBytes(Paths.get(path))
        run(String(bytes, Charset.defaultCharset()))
        if (hadError) {
            exitProcess(65)
        }
        if (hadRuntimeError) {
            exitProcess(70)
        }
    }

    fun runPrompt() {
        val reader = InputStreamReader(System.`in`).buffered()
        while (true) {
            print("lox> ")
            val line: String = reader.readLine() ?: break
            run(line)
            hadError = false
        }
    }

    private fun run(source: String) {
        val scanner = Scanner(source)
        val parser = Parser(scanner.scanTokens())
        val expression = parser.parse()
        if (hadError) return
        expression?.let { interpreter.interpret(it) }
    }

    fun error(line: Int, message: String) {
        report(line, "", message)
    }

    fun error(token: Token, message: String) {
        if (token.type == TokenType.EOF) {
            report(token.line, "at end", message)
        } else {
            report(token.line, " at '${token.lexeme}'", message)
        }
    }

    fun runtimeError(error: RuntimeError) {
        System.err.println("${error.message}\n[line ${error.token.line}]")
        hadRuntimeError = true
    }

    private var hadError = false
    private var hadRuntimeError = false

    private fun report(line: Int, where: String, message: String) {
        System.err.println("[line $line] Error $where: $message")
        hadError = true
    }
}



