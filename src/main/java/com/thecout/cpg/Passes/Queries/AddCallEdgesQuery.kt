package com.thecout.cpg.Passes.Queries

class AddCallEdgesQuery(var fileNames: List<String> = emptyList()) : Query {
    override fun getQuery(): String {
        var queryModifier = ""
        if (fileNames.isNotEmpty()) {
            val fileNameString = fileNames.map { "\"$it\"" }.joinToString(separator = ",") { it }
            queryModifier = "WHERE (n.file in [$fileNameString] OR k.file in [$fileNameString])"
        }
        return "MATCH(k:FunctionDeclaration) WHERE (k.lineStart >0 OR k.columnStart > 0 OR k.lineEnd > 0 OR k.columnEnd > 0)" +
                " MATCH(n:CallExpression {nameParseTree: k.nameParseTree})" +
                " WITH n,k" +
                " $queryModifier MERGE (n)-[:ICG]->(k)" +
                " return DISTINCT k as n;"
    }
}