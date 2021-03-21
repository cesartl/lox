package com.ctl.klox

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
    fun runFile(path: String) {
        val bytes = Files.readAllBytes(Paths.get(path))
        run(String(bytes, Charset.defaultCharset()))
        if (hadError) {
            exitProcess(65)
        }
    }

    fun runPrompt() {
        val reader = InputStreamReader(System.`in`).buffered()
        while (true) {
            println("> ")
            val line: String = reader.readLine() ?: break
            run(line)
            hadError = false
        }
    }

    private fun run(source: String) {
        println("running : $source")
        val scanner = Scanner(source)
        scanner.scanTokens().forEach {
            println(it)
        }
    }

    fun error(line: Int, message: String) {
        report(line, "", message)
    }

    private var hadError = false

    private fun report(line: Int, where: String, message: String) {
        System.err.println("[line $line] Error $where: $message")
        hadError = true
    }
}



