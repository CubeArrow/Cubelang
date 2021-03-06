package com.cubearrow.cubelang.common

/**
 * The Type used to define arrays.
 *
 * @param subType The type of which an array is defined.
 * @param count The amount of arrays elements.
 */
class ArrayType(var subType: Type, var count: Int) : Type {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true


        if(other !is ArrayType) return false

        if (subType != other.subType) return false
        if (count != other.count) return false

        return true
    }

    override fun hashCode(): Int {
        var result = subType.hashCode()
        result = 31 * result + count.hashCode()
        return result
    }

    override fun toString(): String {
        return "[$subType : $count]"
    }
}

/**
 * The [Type] used to define Pointers to other types.
 *
 * @param subtype The type that is being pointed to.
 */
class PointerType(var subtype: Type): Type {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if(other !is PointerType || subtype != other.subtype) return false
        return true
    }

    override fun hashCode(): Int {
        return subtype.hashCode()
    }

    override fun toString(): String {
        return "$subtype*"
    }
}

class NormalType(var typeName: String) : Type {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if(other !is NormalType) return false

        if(typeName == "any" || other.typeName == "any") return true
        if (typeName != other.typeName) return false

        return true
    }

    override fun hashCode(): Int {
        return typeName.hashCode()
    }

    override fun toString(): String {
        return typeName
    }
}
interface Type {
    companion object {
        fun getType(type: Type?, value: Any?): Type {
            var valueToCompare = value
            if (value is Expression.Literal) valueToCompare = value.value
            return type ?: when (valueToCompare) {
                is Int -> NormalType("i32")
                is Double -> NormalType("double")
                is String -> NormalType("string")
                is Char -> NormalType("char")
                //is ClassInstance -> valueToCompare.className
                null -> NormalType("any")
                else -> NormalType("any")
            }
        }
    }
}