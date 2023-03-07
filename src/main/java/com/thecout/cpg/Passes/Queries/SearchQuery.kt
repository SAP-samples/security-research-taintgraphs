package com.thecout.cpg.Passes.Queries


abstract class SearchQuery(
    operator: Operator = Operator.EQUAL,
    conjunction: Conjunction = Conjunction.AND
) : Query {
    abstract fun getQuery(binding: String): String
}

