package com.thecout.cpg.Passes.Queries

class GetNodesByFileAndLine(val files: List<String>, val lines: List<Int>) : Query {
    override fun getQuery(): String {
        val filesString = files.map { "\"$it\"" }.joinToString(",")
        val linesString = lines.joinToString(",")
        return "MATCH(n) WHERE (n.lineStart in [$linesString] or n.lineEnd in [$linesString]) and n.file in [$filesString] return n;"
    }
}