package com.thecout.cpg

import com.thecout.cpg.GraphManagement.DB.BoltDB
import com.thecout.cpg.GraphManagement.DB.CypherRepository
import com.thecout.cpg.GraphManagement.Model.Edge
import com.thecout.cpg.GraphManagement.Model.Node
import com.thecout.cpg.Passes.Services.FindCSinks
import com.thecout.cpg.Passes.Services.FindCSources
import com.thecout.cpg.Passes.Services.GetPaths
import org.junit.jupiter.api.Test

class PathTest {
    @Test
    fun pathTest() {
        val db = BoltDB("bolt://localhost:7687", "", "")
        val repository = CypherRepository(db, reset = false)
        val sources = FindCSources().run(repository).map { it.id }
        val sinks = FindCSinks().run(repository).map { it.id }
        val paths = GetPaths(sources, listOf(), sinks).run(repository)
        val nodes: MutableSet<Node> = mutableSetOf()
        val edges: MutableSet<Edge> = mutableSetOf()
        for (p in paths) {
            nodes.addAll(p.nodes)
            edges.addAll(p.edges)
        }
       print("digraph g { \n ${nodes.joinToString("\n")} \n ${edges.joinToString("\n")}} \n }")
    }
}