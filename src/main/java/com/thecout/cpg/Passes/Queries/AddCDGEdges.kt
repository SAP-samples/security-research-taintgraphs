package com.thecout.cpg.Passes.Queries

class AddCDGEdges(val fileNames: List<String> = emptyList()) : Query {
    override fun getQuery(): String {
        var queryModifier = ""
        if (fileNames.isNotEmpty()) {
            val fileNameString = fileNames.map { "\"$it\"" }.joinToString(separator = ",") { it }
            queryModifier = "AND (n.file in [$fileNameString] OR o.file in [$fileNameString])"
        }
        return "MATCH(o)-[:CFG]->(n)<-[:AST]-(m)" +
                " WHERE labels(o)[0] in [\"IfStatement\", \"ForStatement\", \"ForEachStatement\", \"GotoStatement\", \"SwitchStatement\"," +
                "\"ContinueStatement\", \"CaseStatement\", \"CatchClause\", \"DoStatement\", \"TryStatement\", \"WhileStatement\"]" +
                " $queryModifier MERGE (o)-[:CDG]->(n) MERGE (o)-[:CDG]->(m);"
    }
}
