package com.thecout.cpg.CPGParsing.SymbolicExecution

import kotlin.math.min

class BoxedType(var id: String, var lower: BoxedValue, var upper: BoxedValue) {
    companion object {
        fun mergeBoxedValue(first: BoxedValue, second: BoxedValue): BoxedValue {
            var result: BoxedValue = BoxedValue.Unchecked
            if (first !is BoxedValue.Unchecked && second is BoxedValue.Unchecked) {
                result = first
            } else if (first is BoxedValue.Unchecked && second !is BoxedValue.Unchecked) {
                result = second
            } else if (first is BoxedValue.Numerical && second is BoxedValue.Numerical) {
                result = BoxedValue.Numerical(min(first.value, second.value))
            } else if (first is BoxedValue.Identifier && second is BoxedValue.Identifier) {
                result = BoxedValue.Identifier(first.value + "|" + second.value)
            } else if (first is BoxedValue.Identifier && second is BoxedValue.Numerical) {
                result = BoxedValue.Identifier(first.value + "|" + second.value)
            } else if (first is BoxedValue.Numerical && second is BoxedValue.Identifier) {
                result = BoxedValue.Identifier(first.value.toString() + "|" + second.value)
            }
            return result
        }
    }

    override fun toString(): String {
        return "BoxedType(id=$id, lower=$lower, upper=$upper)"
    }

    fun merge(other: BoxedType): BoxedType {
        if (id != other.id) {
            throw IllegalArgumentException("Cannot merge two BoxedTypes with different ids")
        }
        return BoxedType(id, mergeBoxedValue(this.lower, other.lower), mergeBoxedValue(this.upper, other.upper))
    }
}