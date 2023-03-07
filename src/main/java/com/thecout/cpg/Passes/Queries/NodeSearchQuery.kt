package com.thecout.cpg.Passes.Queries

import com.thecout.cpg.GraphManagement.Model.EMPTY_NUMBER
import com.thecout.cpg.GraphManagement.Model.EMPTY_STRING
import com.thecout.cpg.GraphManagement.Model.Node
import java.util.stream.Collectors

class NodeSearchQuery(
    private val node: Node,
    private val operator: Operator = Operator.EQUAL,
    private val conjunction: Conjunction = Conjunction.AND,
) :
    SearchQuery(operator, conjunction) {

    fun getLabel(): String {
        if (node.nameAST == EMPTY_STRING)
            return ""
        else
            return ":${node.nameAST}"
    }

    fun parseClass(): Map<String, String> {
        val map = HashMap<String, String>()
        for (field in node.javaClass.declaredFields) {
            field.isAccessible = true // You might want to set modifier to public first.
            val value: Any? = field.get(node)
            if (value is String && field.name == "nameAST") {
                continue
            }
            if (value is String && value != EMPTY_STRING) {
                map.put(field.name, "\"" + value.toString() + "\"")
            } else if ((value is Int || value is Long) && value.toString() != EMPTY_NUMBER.toString()) {
                map.put(field.name, value.toString())
            }
        }

        return map
    }

    override fun getQuery(binding: String): String {
        val query = parseClass().toList().stream()
            .map { (key, value) -> "${binding}.$key${operator.value}$value" }
            .collect(Collectors.joining(" ${conjunction.value} "))
        return query.toString()
    }

    override fun getQuery(): String {
        val query = parseClass().toList().stream()
            .map { (key, value) -> "$key${operator.value}$value" }
            .collect(Collectors.joining(" ${conjunction.value} "))
        return query.toString()
    }

}

