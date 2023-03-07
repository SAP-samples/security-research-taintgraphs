package com.thecout.cpg.Passes.Queries

class GetCSources : Query {
    // (functionName, argumentIndex)
    // if index is negative, it is the return value
    private val sourcesWithIndex: List<Pair<String, Long>> = listOf(
        Pair("fgetc", -1),
        Pair("fgetc", -1),
        Pair("poll", 0),
        Pair("ppoll", 0),
        Pair("fopen", -1),
        Pair("fread", 0),
        Pair("fscanf", 2),
        Pair("fscanf", 3),
        Pair("fscanf", 4),
        Pair("fscanf", 5),
        Pair("fscanf", 6),
        Pair("fscanf", 7),
        Pair("fscanf", 8),
        Pair("fscanf", 9),
        Pair("getc", -1),
        Pair("getch", -1),
        Pair("getchar", -1),
        Pair("getche", -1),
        Pair("getenv", -1),
        Pair("gets", -1),
        Pair("poll", -1),
        Pair("read", 0),
        Pair("recv", 1),
        Pair("recvfrom", 1),
        Pair("secure_getenv", -1),
        Pair("scanf", 1),
        Pair("scanf", 2),
        Pair("scanf", 3),
        Pair("scanf", 4),
        Pair("scanf", 5),
        Pair("scanf", 6),
        Pair("scanf", 7),
        Pair("scanf", 8),
        Pair("scanf", 9),
    );

    /**
    override fun getQuery(): String {
    val whereQuery: String = sourcesWithIndex
    .map{p ->
    if (p.second >= 0) {
    "(m.nameParseTree = \"${p.first}\" and n.argumentIndex = ${p.second} and TYPE(e) = \"DDG\")"
    } else {
    "(m.nameParseTree = \"${p.first}\" and TYPE(e) = \"DFG\")"
    }
    }
    .joinToString(" or ")
    return "MATCH (m: CallExpression)-[e:DDG|DFG]->(n) WHERE ($whereQuery) RETURN n;"
    }**/
    override fun getQuery(): String {
        return "MATCH(n:CallExpression) WHERE n.nameParseTree in [\"getchar\",\"fgets\",\"read\",\"fopen\"," +
                "\"scanf\",\"gets\",\"scan\",\"getch\",\"fscanf\",\"getenv\",\"secure_getenv\", \"fread\", \"poll\", \"ppoll\", \"recvfrom\"] return n;"
    }

}
