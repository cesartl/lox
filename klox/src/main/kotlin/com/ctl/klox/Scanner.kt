package com.ctl.klox

import com.ctl.klox.TokenType.*

class Scanner(private val source: String) {

    private var start = 0
    private var current = 0
    private var line = 0
    private val tokens = mutableListOf<Token>()

    fun scanTokens(): List<Token> {

        while (!isAtEnd()) {
            // We are at the beginning of the next lexeme.
            start = current
            scanToken()
        }
        tokens.add(Token(EOF, "", null, line))
        return tokens
    }

    private fun scanToken() {
        when (val c = advance()) {
            '(' -> addToken(LEFT_PAREN)
            ')' -> addToken(RIGHT_PAREN)
            '{' -> addToken(LEFT_BRACE)
            '}' -> addToken(RIGHT_BRACE)
            ',' -> addToken(COMMA)
            '.' -> addToken(DOT)
            '-' -> addToken(MINUS)
            '+' -> addToken(PLUS)
            ';' -> addToken(SEMICOLON)
            '*' -> addToken(STAR)
            '!' -> addToken(if (match('=')) BANG_EQUAL else BANG)
            '=' -> addToken(if (match('=')) EQUAL_EQUAL else EQUAL)
            '<' -> addToken(if (match('=')) LESS_EQUAL else LESS)
            '>' -> addToken(if (match('=')) GREATER_EQUAL else GREATER)
            '/' -> {
                if (match('/')) {
                    while (peek() != '\n') advance()
                } else {
                    addToken(SLASH)
                }
            }
            ' ', '\r', '\t' -> {
            }
            '\n' -> line++
            '"' -> string()
            else -> {
                when {
                    isDigit(c) -> {
                        number()
                    }
                    isAlpha(c) -> {
                        identifier()
                    }
                    else -> {
                        error(line, "Unexpected character: $c")
                    }
                }
            }
        }
    }

    private fun identifier() {
        while (isAlphaNumeric(peek())) advance()
        val text = source.substring(start, current)
        keyWords[text]?.let { addToken(it) } ?: addToken(IDENTIFIER)
    }

    private fun number() {
        while (isDigit(peek())) advance()

        if (peek() == '.' && isDigit(peekNext())) {
            advance()
        }

        while (isDigit(peek())) advance()
        addToken(NUMBER, source.substring(start, current).toDouble())
    }

    private fun string() {
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') line++
            advance()
        }
        if (isAtEnd()) {
            error(line, "Unterminated string.")
            return
        }
        // the closing "
        advance()
        val value = source.substring(start + 1, current - 1)
        addToken(STRING, value)
    }

    private fun isDigit(c: Char): Boolean {
        return c in '0'..'9'
    }

    private fun isAlpha(c: Char): Boolean {
        return c in 'a'..'z' || c in 'A'..'Z' || c == '_'
    }

    private fun isAlphaNumeric(c: Char): Boolean = isAlpha(c) || isDigit(c)


    private fun peek(): Char {
        if (isAtEnd()) return '\n'
        return source[current]
    }

    private fun peekNext(): Char {
        if (isAtEnd()) return '\n'
        return source[current + 1]
    }

    private fun match(expected: Char): Boolean {
        if (isAtEnd()) return false
        if (source[current] != expected) return false
        current++
        return true
    }

    private fun error(line: Int, txt: String) {
        Lox.error(line, txt)
    }

    private fun addToken(type: TokenType, literal: Any? = null) {
        val text = source.substring(start, current)
        tokens.add(Token(type, text, literal, line))
    }

    private fun advance(): Char = source[current++]

    private fun isAtEnd(): Boolean = current >= source.length

    companion object {
        val keyWords: Map<String, TokenType> = mapOf(
            "and" to AND,
            "class" to CLASS,
            "else" to ELSE,
            "false" to FALSE,
            "for" to FOR,
            "fun" to FUN,
            "if" to IF,
            "nil" to NIL,
            "or" to OR,
            "print" to PRINT,
            "return" to RETURN,
            "super" to RETURN,
            "this" to THIS,
            "true" to TRUE,
            "var" to VAR,
            "while" to WHILE,
            "tailrec" to TAILREC
        )
    }
}