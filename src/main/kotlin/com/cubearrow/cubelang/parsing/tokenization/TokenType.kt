package com.cubearrow.cubelang.parsing.tokenization

import com.cubearrow.cubelang.bnf.BnfRule

/**
 * The separate tokens in enum form. The same ones can be found in the grammar file.
 */
enum class TokenType {
    IDENTIFIER, IF, RETURN, FUN, WHILE, BRCKTL, BRCKTR, SEMICOLON, ADD, SUB, EXP, DIV, MULT, MOD, EQEQ, EXCLEQ, AND_GATE, OR_GATE, NUMBER, DOUBLE,CHAR_LITERAL, INT_TYPE, DOUBLE_TYPE, INT, EQUALS, NOT_FOUND;

    companion object {
        /**
         * Parses a Token from a string using a [TokenGrammar] object to know what RegEx to match.
         *
         * @param string       The string to parse the token from
         * @param tokenGrammar The lexification grammar in [TokenGrammar]. This contains the regexes that the string has to match.
         * @return Returns the valid Token, returns Token.NOT_FOUND if nothing matches.
         */
        fun fromString(string: String, tokenGrammar: TokenGrammar): TokenType {
            val rules: MutableList<BnfRule?> = tokenGrammar.bnfParser.rules
            // Iterate over the grammar entries in order to see which RegEx key matches the string
            for (rule in rules) {
                if(rule == null){
                    continue
                }
                if (string.matches(rule.toRegex())) {
                    return valueOf(rule.name.toUpperCase())
                }
            }
            return NOT_FOUND
        }
    }
}