package com.ctl.klox

import com.ctl.klox.TokenType.*
import com.ctl.klox.ast.*
import kotlin.random.Random

class Parser(private val tokens: List<Token>) {
    private var current = 0

    fun parse(): List<Stmt> {
        val statements = mutableListOf<Stmt>()
        while (!isAtEnd()) {
            declaration()?.let { statements.add(it) }
        }
        return statements
    }

    private fun declaration(): Stmt? {
        return try {
            when {
                match(TAILREC) -> {
                    consume(FUN, "Expected 'fun' after tailrec.")
                    function("function", true)
                }
                match(CLASS) -> classDeclaration()
                match(FUN) -> function("function")
                match(VAR) -> varDeclaration()
                else -> statement()
            }
        } catch (e: ParseError) {
            synchronize()
            null
        }
    }

    private fun classDeclaration(): Stmt.Class {
        val name = consume(IDENTIFIER, "Expect class name.")
        consume(LEFT_BRACE, "Expect '{' before class body.")

        val methods = mutableListOf<Stmt.Function>()
        while(!check(RIGHT_BRACE) && !isAtEnd()){
            methods.add(function("method"))
        }
        consume(RIGHT_BRACE, "Expect '}' after class body.")
        return Stmt.Class(name, methods)
    }

    private fun function(kind: String, tailrec: Boolean = false): Stmt.Function {
        val name = consume(IDENTIFIER, "Expect $kind name.")
        consume(LEFT_PAREN, "Expect '(' after $kind name.")
        val parameters = mutableListOf<Token>()
        if (!check(RIGHT_PAREN)) {
            do {
                if (parameters.size >= 255) {
                    error(peek(), "Can't have more than 255 parameters.")
                }
                parameters.add(consume(IDENTIFIER, "Expect parameter name."))
            } while (match(COMMA))
        }
        consume(RIGHT_PAREN, "Expect ')' after $kind name.")
        consume(LEFT_BRACE, "Expect '{' before $kind body.")
        val body = block()

        if (tailrec) {
            val tailCallFunction = TailRecOptimizer.extractTailCallFunction(body)
            if (tailCallFunction != null && tailCallFunction.isFor(name.lexeme)) {
                println("Optimizing tail call for function ${name.lexeme}")
                val call = tailCallFunction.tailCall.call
                val assignments = parameters.zip(call.arguments).map { (param, arg) ->
                    Stmt.Expression(Expr.Assign(param, arg))
                }
                val whileBody = Stmt.Block(
                    tailCallFunction.tailCall.preCalls +
                            assignments +
                            tailCallFunction.initialCalls
                )
                val condition = tailCallFunction.condition ?: Expr.Literal(true)
                val whileStmt = Stmt.While(condition, whileBody)
                // Currently needs to randomly modify the lines for the elements inside the while loop
                // this is because this extra closure doesn't normally exist in the recursive form and
                // copying the tokens as it confuses the Resolver.
                val random = Random(System.currentTimeMillis())
                val newBody =
                    (tailCallFunction.initialCalls +
                            listOf(whileStmt.lineOffset(random.nextInt(99, 999))) +
                            tailCallFunction.terminationCalls)
//                println(AstPrinter().printStmts(newBody))
                return Stmt.Function(name, parameters, newBody)
            }
        }
        if (tailrec) {
            error(name, "tailrec function is not tail recursive.")
        }

        return Stmt.Function(name, parameters, body)
    }

    private fun varDeclaration(): Stmt {
        val name = consume(IDENTIFIER, "Expect variable name.")
        var initializer: Expr? = null
        if (match(EQUAL)) {
            initializer = expression()
        }
        consume(SEMICOLON, "Expect ';' after variable declaration.")
        return Stmt.Var(name, initializer)
    }

    private fun statement(): Stmt {
        return when {
            match(PRINT) -> printStatement()
            match(LEFT_BRACE) -> Stmt.Block(block())
            match(IF) -> ifStatement()
            match(RETURN) -> returnStatement()
            match(WHILE) -> whileStatement()
            match(FOR) -> forStatement()
            else -> expressionStatement()
        }
    }

    private fun returnStatement(): Stmt {
        val keyword = previous()
        val value = if (!check(SEMICOLON)) expression() else null
        consume(SEMICOLON, "Expect ';' after return value.")
        return Stmt.Return(keyword, value)
    }

    private fun forStatement(): Stmt {
        consume(LEFT_PAREN, "Expect '(' after 'for'.")
        val initializer = when {
            match(SEMICOLON) -> null
            match(VAR) -> varDeclaration()
            else -> expressionStatement()
        }
        val condition = if (!check(SEMICOLON)) expression() else null
        consume(SEMICOLON, "Expect ';' after loop condition.")
        val increment = if (!check(RIGHT_PAREN)) expression() else null
        consume(RIGHT_PAREN, "Expect ')' after for clauses.")
        var body = statement()
        if (increment != null) {
            body = Stmt.Block(listOf(body, Stmt.Expression(increment)))
        }
        body = Stmt.While(condition ?: Expr.Literal(true), body)
        if (initializer != null) {
            body = Stmt.Block(listOf(initializer, body))
        }
        return body
    }

    private fun whileStatement(): Stmt {
        consume(LEFT_PAREN, "Expect '(' after 'while'.")
        val condition = expression()
        consume(RIGHT_PAREN, "Expect ')' after while condition.")
        val body = statement()
        return Stmt.While(condition, body)
    }

    private fun ifStatement(): Stmt {
        consume(LEFT_PAREN, "Expect '(' after 'if'.")
        val condition = expression()
        consume(RIGHT_PAREN, "Expect ')' after if condition.")
        val thenBranch = statement()
        val elseBranch = if (match(ELSE)) {
            statement()
        } else {
            null
        }
        return Stmt.If(condition, thenBranch, elseBranch)
    }

    private fun block(): List<Stmt> {
        val statements = mutableListOf<Stmt>()
        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            declaration()?.let { statements.add(it) }
        }
        consume(RIGHT_BRACE, "Expect '}' after block.")
        return statements
    }

    private fun expressionStatement(): Stmt {
        val expr = expression()
        consume(SEMICOLON, "Expect ';' after expression")
        return Stmt.Expression(expr)
    }

    private fun printStatement(): Stmt {
        val value = expression()
        consume(SEMICOLON, "Expect ';' after value")
        return Stmt.Print(value)
    }

    private fun expression(): Expr {
        return assignment()
    }

    private fun assignment(): Expr {
        val expr = or()
        if (match(EQUAL)) {
            val equals = previous()
            val value = assignment()
            when (expr) {
                is Expr.Variable -> {
                    val name = expr.name
                    return Expr.Assign(name, value)
                }
                is Expr.Get -> {
                    return Expr.Set(expr.targetObject, expr.name, value)
                }
                else -> error(equals, "Invalid assignment target.")
            }
        }
        return expr
    }

    private fun or(): Expr {
        return logical({ and() }, OR)
    }

    private fun and(): Expr {
        return logical({ equality() }, AND)
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
        return call()
    }

    private fun call(): Expr {
        var expr = primary()
        while (true) {
            expr = if (match(LEFT_PAREN)) {
                finishCall(expr)
            } else if(match(DOT)){
                val name = consume(IDENTIFIER, "Expect property name after '.'")
                Expr.Get(expr, name)
            } else {
                break
            }
        }
        return expr
    }

    private fun finishCall(callee: Expr): Expr {
        val arguments = mutableListOf<Expr>()
        if (!check(RIGHT_PAREN)) {
            do {
                if (arguments.size >= 255) {
                    error(peek(), "Can't have more than 255 arguments.")
                }
                arguments.add(expression())
            } while (match(COMMA))
        }
        val paren = consume(RIGHT_PAREN, "Expect ')' after arguments.")
        return Expr.Call(callee, paren, arguments)
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
                return Expr.Grouping(expr)
            }
            match(THIS) -> return Expr.This(previous())
            match(IDENTIFIER) -> return Expr.Variable(previous())
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
        return manyOperator(operand, { left, operator, right -> Expr.Binary(left, operator, right) }, *types)
    }

    private fun logical(operand: () -> Expr, vararg types: TokenType): Expr {
        return manyOperator(operand, { left, operator, right -> Expr.Logical(left, operator, right) }, *types)
    }

    private fun manyOperator(
        operand: () -> Expr,
        exprConstructor: (Expr, Token, Expr) -> Expr,
        vararg types: TokenType
    ): Expr {
        var expr = operand()
        while (match(*types)) {
            val operator = previous()
            val right = operand()
            expr = exprConstructor(expr, operator, right)
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

private class ParseError : RuntimeException()