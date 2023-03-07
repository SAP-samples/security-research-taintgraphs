package com.thecout.cpg.Passes.Queries

class Path(
    val from: Long,
    val to: List<Long>,
    private val withCFG: Boolean = false,
    private val limit: Int = 10,
    private val pathLength: Int = 1000
) :
    Query {
    override fun getQuery(): String {
        val toQueryString = "[" + to.joinToString(separator = ",") + "]"
        val CFG = if (withCFG) "CFG|" else ""
        return "match path=(n {id: $from})-[:${CFG}CDG|DDG|DFG|IDFG *bfs..$pathLength]->(k) " +
                " WHERE k.id in $toQueryString" +
                " WITH path as p limit $limit" +
                " RETURN p;"
    }
}