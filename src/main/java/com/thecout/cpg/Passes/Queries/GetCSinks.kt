package com.thecout.cpg.Passes.Queries

class GetCSinks : Query {
    // (functionName, argumentIndex)
    private val sinksWithIndex: List<Pair<String, Long>> = listOf(
        Pair("strcpy", 1),
        Pair("memcpy", 2),
        Pair("memset", 2),
        Pair("strncpy", 1),
        Pair("strncpy", 2),
        Pair("strcat", 1),
        Pair("printf", 0),
        Pair("sprintf", 1),
        Pair("snprintf", 2),
        Pair("malloc", 0),
        Pair("realloc", 1),
        Pair("calloc", 0),
        Pair("calloc", 1),
        Pair("fread", 2),
        Pair("free", 0)
    )
    /**
    override fun getQuery(): String {
        val whereQuery: String = sinksWithIndex
            .map{p -> "(m.nameParseTree = \"${p.first}\" and n.argumentIndex = ${p.second})"}
            .joinToString(" or ")
        return "MATCH (m: CallExpression)-[:DDG]->(n) WHERE ($whereQuery) RETURN n;";
    }
    **/
    override fun getQuery(): String {
        return "MATCH(n:CallExpression)" +
                " WHERE n.nameParseTree in [\"malloc\",\"memcpy\",\"strcpy\",\"printf\",\"memset\",\"wrapper\", \"calloc\", \"realloc\", \"snprintf\", \"sprintf\", \"strcat\", \"free\"]" +
                " return n;"
    }

}