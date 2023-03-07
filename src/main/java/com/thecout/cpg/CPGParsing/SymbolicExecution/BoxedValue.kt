package com.thecout.cpg.CPGParsing.SymbolicExecution

sealed class BoxedValue {
    class Numerical(val value: Long) : BoxedValue() {
        override fun toString(): String {
            return value.toString()
        }
    }

    class Identifier(val value: String) : BoxedValue() {
        override fun toString(): String {
            return value
        }
    }

    object Unchecked : BoxedValue() {
        override fun toString(): String {
            return "unchecked"
        }
    }
}