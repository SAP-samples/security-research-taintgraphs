package com.thecout.cpg.GraphManagement.DB

import com.thecout.cpg.GraphManagement.Model.Node
import com.thecout.cpg.GraphManagement.Model.Path
import com.thecout.cpg.Passes.Queries.NodeSearchQuery
import com.thecout.cpg.Passes.Queries.Query

interface Repository {
    fun addNode(node: Node): Node
    fun addEdge(a: Node, b: Node, type: String, branch: String = "")
    fun searchNode(searchQuery: NodeSearchQuery): List<Node>
    fun deleteNode(searchQuery: NodeSearchQuery)
    fun addNodes(nodes: List<Node>)
    fun nodeQuery(query: Query): List<Node>
    fun nodeQuery(query: Query, binding: String): List<Node>
    fun createIndex()
    fun query(query: Query): Any
    fun pathQuery(query: Query, timeOut: Long = 0): List<Path>

}
