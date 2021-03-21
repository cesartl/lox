package com.ctl.klox.ast

import com.ctl.klox.Token
import com.ctl.klox.TokenType

class AstUtils {
    companion object {
        fun dummyAst(): Expr = Expr.Binary(
            Expr.Unary(Token(TokenType.MINUS, "-", null, 1), Expr.Literal(123)),
            Token(TokenType.STAR, "*", null, 1),
            Expr.Grouping(Expr.Literal(45.67))
        )

        fun arith(): Expr = Expr.Binary(
            Expr.Binary(Expr.Literal(1), Token(TokenType.PLUS, "+", null, 1), Expr.Literal(2)),
            Token(TokenType.STAR, "*", null, 1),
            Expr.Binary(Expr.Literal(4), Token(TokenType.MINUS, "-", null, 1), Expr.Literal(3)),
        )
    }
}