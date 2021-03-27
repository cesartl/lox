package com.ctl.klox

import com.ctl.klox.TokenType.*
import com.ctl.klox.ast.Expr

class Parser(private val tokens: List<Token>) {
    private var current = 0

    fun parse(): Expr? {
        return try {
            expression()
        } catch (e: ParseError) {
            null
        }
    }

    private fun expression(): Expr {
        return equality()
    }

    private fun equality(): Expr {
        return binary({ comparison() }, BANG_EQUAL, EQUAL_EQUAL)
    }

    private fun comparison(): Expr {
        return binary({ term() }, GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)
    }

    private fun term(): Expr {
        return binary({ factor() }, MINUS, PLUS)
    }

    private fun factor(): Expr {
        return binary({ unary() }, SLASH, STAR)
    }

    private fun unary(): Expr {
        if (match(BANG, MINUS)) {
            val operator = previous()
            val right = unary()
            return Expr.Unary(operator, right)
        }
        return primary()
    }

    private fun primary(): Expr {
        when {
            match(FALSE) -> return Expr.Literal(false)
            match(TRUE) -> return Expr.Literal(true)
            match(NIL) -> return Expr.Literal(null)
            match(NUMBER, STRING) -> return Expr.Literal(previous().literal)
            match(LEFT_PAREN) -> {
                val expr = expression()
                consume(RIGHT_PAREN, "Expect ')' after expression.")
                return Expr.Grouping(expression())
            }
        }
        throw error(peek(), "Expect expression")
    }

    private fun consume(type: TokenType, message: String): Token {
        if (check(type)) return advance()
        throw error(peek(), message)
    }

    private fun synchronize() {
        advance()
        while (!isAtEnd()) {
            if (previous().type == SEMICOLON) return
            when (peek().type) {
                CLASS, FUN, VAR, FOR, IF, WHILE, PRINT, RETURN -> return
                else -> advance()
            }
        }
    }

    private fun error(token: Token, message: String): ParseError {
        Lox.error(token, message)
        return ParseError()
    }

    private fun binary(operand: () -> Expr, vararg types: TokenType): Expr {
        var expr = operand()
        while (match(*types)) {
            val operator = previous()
            val right = operand()
            expr = Expr.Binary(expr, operator, right)
        }
        return expr
    }

    private fun match(vararg types: TokenType): Boolean {
        types.forEach { type ->
            if (check(type)) {
                advance()
                return true
            }
        }
        return false
    }

    private fun check(type: TokenType): Boolean {
        if (isAtEnd()) {
            return false
        }
        return peek().type == type
    }

    private fun peek(): Token {
        return tokens[current]
    }

    private fun isAtEnd(): Boolean {
        return peek().type == EOF
    }

    private fun advance(): Token {
        if (!isAtEnd()) current++
        return previous()
    }

    private fun previous(): Token {
        return tokens[current - 1]
    }
}

private class ParseError() : RuntimeException()