package com.cubearrow.cubelang.compiler.specificcompilers

import com.cubearrow.cubelang.common.Expression
import com.cubearrow.cubelang.compiler.Compiler
import com.cubearrow.cubelang.compiler.CompilerContext
import com.cubearrow.cubelang.compiler.utils.CompilerUtils
import com.cubearrow.cubelang.compiler.utils.CompilerUtils.Companion.checkMatchingTypes
import com.cubearrow.cubelang.compiler.utils.CompilerUtils.Companion.getASMPointerLength
import com.cubearrow.cubelang.compiler.utils.CompilerUtils.Companion.getRegister
import com.cubearrow.cubelang.compiler.utils.CommonErrorMessages
import com.cubearrow.cubelang.common.NormalType
import com.cubearrow.cubelang.common.definitions.Function
import com.cubearrow.cubelang.compiler.utils.TypeUtils
import kotlin.math.max

/**
 * The compiler needed for calling functions in the source code.
 *
 * The arguments use the appropriate registers and the returned result is stored in the 'ax' register.
 */
class CallCompiler(var context: CompilerContext) : SpecificCompiler<Expression.Call> {
    var registerIndex = 0
    override fun accept(expression: Expression.Call): String {
        if (expression.callee is Expression.VarCall) {
            val varCall = expression.callee as Expression.VarCall
            val function = context.getFunction(varCall.varName.substring, expression.arguments.size)
            if (function != null) {
                context.argumentIndex = 0
                val args = getFunctionCallArguments(expression, function)
                return "${args}call ${varCall.varName.substring}"
            }
            CommonErrorMessages.xNotFound("called function", varCall.varName, context)
        }
        return ""
    }

    private fun getFunctionCallArguments(call: Expression.Call, function: Function): String {
        var args = ""
        val laterArgs: MutableMap<Int, Expression> = HashMap()
        for (i in call.arguments.indices) {
            val argumentExpression = call.arguments[i]
            if (argumentExpression !is Expression.Call) {
                laterArgs[i] = argumentExpression
                continue
            }
            args += getSingleArgument(function, i, argumentExpression)
        }

        for (entry in laterArgs) {
            val i = entry.key
            val argumentExpression = call.arguments[i]
            args += getSingleArgument(function, i, argumentExpression)
        }
        return args
    }

    private fun getSingleArgument(function: Function, argumentIndex: Int, argumentExpression: Expression): String {
        val expectedArgumentType = function.args[function.args.keys.elementAt(argumentIndex)] ?: error("Unreachable")

        if(expectedArgumentType is NormalType && !Compiler.PRIMARY_TYPES.contains(expectedArgumentType.typeName))
            return moveStruct(expectedArgumentType, argumentExpression)
        val moveInformation = context.moveExpressionToX(argumentExpression)
        checkMatchingTypes(expectedArgumentType, moveInformation.type, -1, -1, context)
        for (i in CompilerUtils.splitLengthIntoRegisterLengths(TypeUtils.getLength(moveInformation.type)))
        if(TypeUtils.getRawLength(moveInformation.type) < 4 && argumentExpression !is Expression.Literal){
            return "${moveInformation.before}\n" +
                    "movsx ${getRegister(Compiler.ARGUMENT_INDEXES[registerIndex++]!!, 4)}, ${getASMPointerLength(TypeUtils.getRawLength(moveInformation.type))} ${moveInformation.pointer}\n"
        }
        return moveInformation.moveTo(getRegister(Compiler.ARGUMENT_INDEXES[registerIndex++]!!, max(4, TypeUtils.getRawLength(moveInformation.type))))
    }

    private fun moveStruct(expectedArgumentType: NormalType, expression: Expression): String {
        if(expression !is Expression.VarCall) {
            context.error(-1, -1, "Expected a variable call when passing structs.")
            return ""
        }
        val variable = context.getVariable(expression.varName.substring)!!
        val sizes = CompilerUtils.splitLengthIntoRegisterLengths(TypeUtils.getLength(expectedArgumentType))
        var indexRemoved = 0
        var resultingString = ""
        for (size in sizes) {
            for (times in 0 until size.second) {
                resultingString += "mov ${getRegister(Compiler.ARGUMENT_INDEXES[registerIndex++]!!, size.first)}, ${getASMPointerLength(size.first)} [rbp - ${variable.index - indexRemoved}]\n"
                indexRemoved += size.first
            }
        }
        return resultingString
    }
}