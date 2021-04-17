package com.ctl.klox

data class Token(
    val type: TokenType,
    val lexeme: String,
    val literal: Any ?,
    val line: Int
) {
    override fun toString(): String {
        return "$type $lexeme $literal"
    }

    fun lineOffset(offset: Int): Token = copy(line = line + offset)
}