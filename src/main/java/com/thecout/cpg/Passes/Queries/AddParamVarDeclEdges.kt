package com.thecout.cpg.Passes.Queries

class AddParamVarDeclEdges(val fileNames: List<String> = emptyList()) : Query {
    override fun getQuery(): String {
        var queryModifier = ""
        if (fileNames.isNotEmpty()) {
            val fileNameString = fileNames.map { "\"$it\"" }.joinToString(separator = ",") { it }
            queryModifier = "WHERE n.file in [$fileNameString]"
        }
        return "MATCH(o:FunctionDeclaration)-[:AST]->(n:ParamVariableDeclaration) $queryModifier MERGE (n)-[:DFG]->(o);"
    }
}