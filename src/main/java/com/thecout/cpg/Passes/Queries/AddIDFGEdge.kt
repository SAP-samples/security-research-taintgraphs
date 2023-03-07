package com.thecout.cpg.Passes.Queries

class AddIDFGEdge(val callId: List<Long>, val fileNames: List<String> = emptyList()) : Query {

    override fun getQuery(): String {
        var queryModifier = ""
        if (fileNames.isNotEmpty()) {
            val fileNameString = fileNames.map { "\"$it\"" }.joinToString(separator = ",") { it }
            queryModifier = "(k.file in [$fileNameString] OR m.file in [$fileNameString]) AND"
        }
        return if (callId.size > 2) {
            val callIdString = callId.joinToString(separator = ",")
            "MATCH(o)<-[:AST]-(k)-[:ICG]->(m)-[:AST]->(f:ParamVariableDeclaration) WHERE m.id in [$callIdString] AND $queryModifier o.argumentIndex = f.argumentIndex MERGE (o)-[:IDFG]->(f);"
        } else {
            "MATCH(o)<-[:AST]-(k)-[:ICG]->(m {id: ${callId.first()}})-[:AST]->(f:ParamVariableDeclaration) WHERE $queryModifier o.argumentIndex = f.argumentIndex MERGE (o)-[:IDFG]->(f);"
        }
    }
}