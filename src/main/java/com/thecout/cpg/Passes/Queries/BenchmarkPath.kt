package com.thecout.cpg.Passes.Queries

class BenchmarkPath(val pathLength: Int, val numberPath: Int) : Query {
    override fun getQuery(): String {
        return "match path=(o {id: 0})-[:DFG*..$pathLength]-(n) return n LIMIT $numberPath;"
    }
}