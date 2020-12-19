package com.cubearrow.cubelang.compiler.specificcompilers

import com.cubearrow.cubelang.compiler.Compiler
import com.cubearrow.cubelang.compiler.CompilerContext
import com.cubearrow.cubelang.compiler.CompilerUtils
import com.cubearrow.cubelang.lexer.Token
import com.cubearrow.cubelang.lexer.TokenType
import com.cubearrow.cubelang.parser.Expression
import com.cubearrow.cubelang.utils.ExpressionUtils
import com.cubearrow.cubelang.utils.UsualErrorMessages

class OperationCompiler(var context: CompilerContext) : SpecificCompiler<Expression.Operation> {
    override fun accept(expression: Expression.Operation): String {
        val wasInSub = context.isInSubOperation
        if (!context.isInSubOperation) context.isInSubOperation = true
        context.operationIndex++
        val register = Compiler.OPERATION_REGISTERS[context.operationIndex]!!
        val rightPair = getOperationSide(expression.expression2)
        val rightSide = rightPair.first + "\nmov r$register, rax"
        val rightRegister = CompilerUtils.getRegister("ax", rightPair.second)

        val leftPair = getOperationSide(expression.expression)
        val leftSide = leftPair.first
        val leftRegister = CompilerUtils.getRegister(register, leftPair.second)
        context.operationResultSize = leftPair.second
        context.operationIndex--
        val operator = CompilerUtils.getOperator(expression.operator.substring)
        val result =
            "$rightSide\n$leftSide\n${operator} ${if (operator != "mul" && operator != "div") "$rightRegister," else ""} $leftRegister"
        return saveUsedRegisters(wasInSub, result, expression)
    }

    private fun saveUsedRegisters(wasInSub: Boolean, result: String, expression: Expression): String {
        var result1 = result
        if (!wasInSub) {
            context.isInSubOperation = false
            for (i in 0 until CompilerUtils.getOperationDepth(expression) - 1) {
                result1 = "push r${Compiler.OPERATION_REGISTERS[i]}\n$result1\npop r${Compiler.OPERATION_REGISTERS[i]}"
            }
        }
        return result1
    }

    private fun getOperationSide(side: Expression): Pair<String, Int> {
        val compilerInstance = context.compilerInstance
        val registerSize: Int
        val leftSide = when (side) {
            is Expression.Literal -> {
                val value = side.accept(compilerInstance)
                val length = ExpressionUtils.getType(null, side.any).getRawLength()
                val register = CompilerUtils.getRegister("ax", length)
                registerSize = length
                "mov $register, $value"
            }
            is Expression.VarCall -> {
                val variable = context.variables.last()[side.identifier.substring]
                    ?: error("The variable could not be found")
                val register = CompilerUtils.getRegister("ax", variable.length)

                registerSize = variable.length
                "mov $register, ${side.accept(compilerInstance)}"
            }
            is Expression.Call -> {
                if (side.expression is Expression.VarCall) {
                    val function = context.functions[side.expression.identifier.substring]
                        ?: error("The called function does not exist")
                    registerSize = function.returnType!!.getRawLength()
                    side.accept(compilerInstance)
                } else {
                    TODO()
                }
            }
            is Expression.ArrayGet -> {
                val variable = CompilerUtils.getVariableFromArrayGet(side, context)
                if (variable != null) {
                    registerSize = variable.type.getRawLength()
                    CompilerUtils.moveArrayGetToSth(
                        side,
                        "mov ${
                            CompilerUtils.getRegister(
                                "ax",
                                variable.type.getRawLength()
                            )
                        }, ${CompilerUtils.getASMPointerLength(variable.type.getRawLength())}", context)
                } else {
                    registerSize = 8
                    UsualErrorMessages.xNotFound("variable", Token("", TokenType.IDENTIFIER, -1, -1))
                    ""
                }
            }
            is Expression.Operation, is Expression.Grouping -> {
                val result = side.accept(compilerInstance)
                registerSize = context.operationResultSize
                result
            }
            else -> {
                registerSize = 8
                side.accept(compilerInstance)
            }
        }
        return Pair(leftSide, registerSize)
    }
}