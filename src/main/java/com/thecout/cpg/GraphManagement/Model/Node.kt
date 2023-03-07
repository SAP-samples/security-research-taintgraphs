package com.thecout.cpg.GraphManagement.Model

const val EMPTY_STRING = "EMPTY_STRING"
const val EMPTY_NUMBER: Long = -1

class Node(
    var id: Long = EMPTY_NUMBER,
    var nameAST: String = EMPTY_STRING,
    var nameParseTree: String = EMPTY_STRING,
    var code: String = EMPTY_STRING,
    var lineStart: Int = EMPTY_NUMBER.toInt(),
    var lineEnd: Int = EMPTY_NUMBER.toInt(),
    var columnStart: Int = EMPTY_NUMBER.toInt(),
    var columnEnd: Int = EMPTY_NUMBER.toInt(),
    var file: String = EMPTY_STRING,
    var argumentIndex: Int = EMPTY_NUMBER.toInt(),
    var upperBound: String = EMPTY_STRING,
    var lowerBound: String = EMPTY_STRING
) {

    override fun toString(): String {
        return "\"$id\" [label = \"$nameAST\", lexeme=\"$nameParseTree\", enclosing=\"$code\", file=\"$file\", location=\"$lineStart:$lineEnd, $columnStart:$columnEnd\", upperBound=\"$upperBound\", lowerBound=\"$lowerBound\"]"
    }

    fun toDot(): String {
        return toString()
    }


    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Node

        if (id != other.id) return false

        return true
    }

}