package com.cubearrow.cubelang.compiler.specificcompilers

import com.cubearrow.cubelang.compiler.Compiler
import com.cubearrow.cubelang.compiler.CompilerContext
import com.cubearrow.cubelang.compiler.utils.CompilerUtils.Companion.getTokenFromArrayGet
import com.cubearrow.cubelang.common.ArrayType
import com.cubearrow.cubelang.common.Expression
import com.cubearrow.cubelang.compiler.utils.CommonErrorMessages
import com.cubearrow.cubelang.common.PointerType
import com.cubearrow.cubelang.common.Type
import com.cubearrow.cubelang.compiler.utils.TypeUtils

/**
 *  Compiles getting from an array or a pointer by moving the value to rax.
 *
 * @param context The needed [CompilerContext].
 */

class ArrayGetCompiler(val context: CompilerContext) : SpecificCompiler<Expression.ArrayGet> {
    override fun accept(expression: Expression.ArrayGet): String {
        val variable = context.getVariableFromArrayGet(expression)
        if(variable == null){
            CommonErrorMessages.xNotFound("requested array-variable", getTokenFromArrayGet(expression), context)
            return ""
        }
        if(variable.type is ArrayType) {
            return getArrayType(expression, variable, variable.type as ArrayType)
        } else if(variable.type is PointerType && expression.inBrackets is Expression.Literal){
            return getPointerType(expression, variable.type as PointerType)
        }
        context.error(-1, -1, "Unable to compile the requested type of array access. This may be changed in the future.")
        return ""
    }

    private fun getPointerType(expression: Expression.ArrayGet, type: PointerType): String {
        val firstTriple = context.moveExpressionToX(expression.expression)
        context.operationResultType = type.subtype
        return firstTriple.moveTo("rax") + "&[rax+${context.evaluate(expression.inBrackets)}]"
    }

    private fun getArrayType(
        expression: Expression.ArrayGet,
        variable: Compiler.LocalVariable,
        type: ArrayType
    ): String {
        context.operationResultType = type.subType
        if (expression.expression is Expression.VarCall && expression.inBrackets is Expression.Literal) {
            val index = variable.index - getIndex(expression, variable.type, TypeUtils.getRawLength(variable.type))
            return " &[rbp - $index]"
        } else if (expression.expression is Expression.VarCall) {
            val triple = context.moveExpressionToX(expression.inBrackets)
            return """movsx rbx, ${triple.pointer}
                            |&[rbp-${variable.index}+rbx*${TypeUtils.getRawLength(variable.type)}]
                        """.trimMargin()
        }
        TODO()
    }

    /**
     * Returns the index of the requested array element by using recursion.
     *
     * @param expression The [Expression.ArrayGet] or [Expression.VarCall] to get the index from.
     * @param type The current type of the expression
     * @param rawLength The length of the register being used.
     */
    private fun getIndex(expression: Expression, type: Type, rawLength: Int): Int {
        if (expression is Expression.ArrayGet) {
            if (expression.inBrackets is Expression.Literal) {
                val literalValue = (expression.inBrackets as Expression.Literal).value
                if (literalValue is Int) {
                    return getIndex(expression.expression, (type as ArrayType).subType, rawLength) * type.count + literalValue * rawLength
                }
            }
        } else if (expression is Expression.VarCall) {
            return 0
        }
        return -1
    }

}