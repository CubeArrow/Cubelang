package com.cubearrow.cubelang.compiler.specificcompilers

import com.cubearrow.cubelang.common.Expression
import com.cubearrow.cubelang.compiler.CompilerContext

/**
 * Compiles defining a function by using a frame pointer.
 *
 * The [FunctionDefinitionCompiler.accept] function is not pure and has multiple side effects.
 *
 * @param context The needed [CompilerContext].
 */
class FunctionDefinitionCompiler(var context: CompilerContext) : SpecificCompiler<Expression.FunctionDefinition> {
    override fun accept(expression: Expression.FunctionDefinition): String {
        context.separateReturnSegment = false

        val arguments = initiateArgumentVariables(expression)

        return """${expression.name.substring}:
            |push rbp
            |mov rbp, rsp
            |sub rsp, ${context.stackIndex.removeLast()}
            |$arguments
            |${if (context.separateReturnSegment) ".L${context.lIndex}:" else ""}
            |leave
            |ret
        """.trimMargin()
    }

    private fun initiateArgumentVariables(expression: Expression.FunctionDefinition): String {
        context.stackIndex.add(0)
        context.variables.add(HashMap())
        context.currentReturnType = expression.type
        context.argumentIndex = 0
        val statements = expression.args.fold("") { acc, it -> acc + context.evaluate(it) } + context.evaluate(expression.body)
        context.variables.removeLast()
        context.currentReturnType = null
        return statements
    }
}