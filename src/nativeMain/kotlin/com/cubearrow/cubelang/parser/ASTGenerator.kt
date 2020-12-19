package com.cubearrow.cubelang.parser

import com.cubearrow.cubelang.bnf.BnfParser
import com.cubearrow.cubelang.bnf.BnfRule
import com.cubearrow.cubelang.bnf.BnfTerm
import com.cubearrow.cubelang.lexer.Token
import com.cubearrow.cubelang.lexer.TokenGrammar
import com.cubearrow.cubelang.utils.IOUtils.Companion.readAllText
import com.cubearrow.cubelang.utils.IOUtils.Companion.writeAllLines
import kotlin.system.exitProcess


/**
 * A generator for the AST from BNF grammar files
 *
 * An abstract base class is created that contains the specific subclasses with the appropriate arguments.
 *
 * The baseclass also contains a Visitor-Interface which has call functions for every [Expression] type.
 * These also each implement the abstract [Expression.accept] function calling their specific call function.
 * The visitor pattern is used to be able to use functions on the specific [Expression] types without changing the generated class.
 *
 *
 * @param outputDir The directory path to output the Expression class to
 * @param syntaxGrammarFile The path to the bnf syntax grammar file
 * @param tokenGrammarFile The path to the bnf-token grammar file
 */
class ASTGenerator(private val outputDir: String, syntaxGrammarFile: String, tokenGrammarFile: String) {
    private var syntaxGrammarParser: BnfParser

    /**
     * Initialize the class by creating the grammar objects and generating the actual class
     */
    init {
        val tokenGrammar = TokenGrammar(readAllText(tokenGrammarFile))
        this.syntaxGrammarParser = BnfParser(readAllText(syntaxGrammarFile), tokenGrammar.bnfParser)

        generateExpressionClass()
    }


    private fun generateVisitorInterface(): String {
        var visitorClassContent = "    interface ExpressionVisitor<R> {\n"

        syntaxGrammarParser.rules.forEach {
            if (it != null && it.name != "expression") {
                visitorClassContent += "        fun visit${it.name.capitalize()}(${it.name}: ${it.name.capitalize()}): R\n"
            }
        }
        return "$visitorClassContent    }\n"
    }

    private fun generateExpressionClass() {
        var expressionFileContent = generateBaseExpressionClass()
        expressionFileContent += generateSubclasses()
        expressionFileContent += generateVisitorInterface()
        expressionFileContent += "    abstract fun <R> accept(visitor: ExpressionVisitor<R>): R\n"
        expressionFileContent += "}\n"

        writeAllLines(outputDir + "Expression.kt", expressionFileContent)
    }

    private fun generateBaseExpressionClass(): String {
        return """
                package com.cubearrow.cubelang.parser
                
                
                import com.cubearrow.cubelang.compiler.Type
                import com.cubearrow.cubelang.lexer.Token
                
                /**
                 * This class is generated automatically by the [ASTGenerator]
                 **/
                abstract class Expression {
            """.trimIndent() + "\n"
    }

    /**
     * Generates the subclasses from the syntax grammar including implementing [Expression.accept]
     */
    private fun generateSubclasses(): String {
        var expressionFileContent1 = ""
        syntaxGrammarParser.rules.forEach {
            if (it != null && it.name != "expression") {
                val parameters = expressionToParameters(it.expression)
                expressionFileContent1 += """
    class ${it.name.capitalize()} ($parameters) : Expression() {
        override fun <R> accept(visitor: ExpressionVisitor<R>): R {
            return visitor.visit${it.name.capitalize()}(this)
        }
    }
"""
            }
        }
        return expressionFileContent1
    }


    /**
     * Generates the parameters to the constructor of a subclass of [Expression] from a [List] of [BnfTerm]
     */
    private fun expressionToParameters(expression: List<List<BnfTerm?>>?): String {
        var result = ""
        val ruleCount = HashMap<BnfRule, Int>()
        expression?.get(0)?.forEach { term ->
            if (term is BnfRule) {
                handleRuleCount(ruleCount, term)
                result += parseSingleParameter(term, ruleCount)
            }

        }
        // Returns the result while removing ", " from the end
        return result.substring(0, result.length - 2)
    }


    /**
     * Increment the ruleCount for rules that were already used or initialize it as 1
     *
     * The ruleCount is used to avoid duplicate parameter names in the subclasses of [Expression]
     */
    private fun handleRuleCount(ruleCount: HashMap<BnfRule, Int>, term: BnfRule) {
        if (ruleCount.containsKey(term)) {
            ruleCount[term] = ruleCount[term]!! + 1
        } else {
            ruleCount[term] = 1
        }
    }

    /**
     * Parses a single parameter for the constructor for [Expression] subclasses.
     *
     * A [MutableList] is used if the name ends with "Lst"
     */
    private fun parseSingleParameter(term: BnfRule, ruleCount: Map<BnfRule, Int>): String {
        val type: String
        when {
            term.name == "typeNull" -> {
                type = "Type?"
            }
            term.name == "type" -> {
                type = "Type"
            }

            term.name.indexOf("Lst") == term.name.length - 3 -> {
                val ruleName = term.name.substring(0, term.name.length - 3)
                val rule = syntaxGrammarParser.getRuleFromString(ruleName)
                type = "List<${rule?.name?.capitalize() ?: "Token"}>"
            }
            term.name.endsWith("Null") -> {
                val ruleName = term.name.substring(0, term.name.length - 4)
                val rule = syntaxGrammarParser.getRuleFromString(ruleName)
                type = "${rule?.name?.capitalize() ?: "Token"}?"
            }
            term.name == "any" -> {
                type = "Any?"
            }

            else -> {
                val rule = syntaxGrammarParser.getRuleFromString(term.name)
                type = rule?.name?.capitalize() ?: "Token"
            }
        }
        return "val ${term.name}${if(ruleCount[term] != 1)  ruleCount[term] else ""}: ${type}, "
    }
}