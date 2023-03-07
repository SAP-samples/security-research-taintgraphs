package com.thecout.cpg

import com.thecout.cpg.GraphManagement.DB.BoltDB
import com.thecout.cpg.GraphManagement.DB.CypherRepository
import com.thecout.cpg.GraphManagement.Model.Edge
import com.thecout.cpg.GraphManagement.Model.Node
import com.thecout.cpg.Passes.Queries.NodeSearchQuery
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

class DBTest {
    @Test
    fun connection() {
        assertDoesNotThrow {
            val db = BoltDB("bolt://localhost:7687", "", "")
            db.close()
        }
    }

    @Test
    fun addNode() {
        val node = Node()
        val db = BoltDB("bolt://localhost:7687", "", "")
        val repository = CypherRepository(db)
        repository.addNode(node)
        db.close()
    }

    @Test
    fun addEdge() {
        val nodeA = Node()
        val nodeB = Node()
        val db = BoltDB("bolt://localhost:7687", "", "")
        val repository = CypherRepository(db)
        repository.addNode(nodeA)
        repository.addNode(nodeB)
        repository.addEdge(nodeA, nodeB, "CFG")
        db.close()
    }

    @Test
    fun addBatchEdge() {
        val nodeA = Node()
        val nodeB = Node()
        val db = BoltDB("bolt://localhost:7687", "", "")
        val repository = CypherRepository(db)
        repository.addNode(nodeA)
        repository.addNode(nodeB)
        val edge = Edge(nodeA, nodeB, "CFG", "true")
        db.close()
    }

    @Test
    fun searchNode() {
        val node = Node()
        val db = BoltDB("bolt://localhost:7687", "", "")
        val repository = CypherRepository(db)
        val searchQuery = NodeSearchQuery(node)
        val nodeResults = repository.searchNode(searchQuery)
        for (nodeResult in nodeResults) {
            println(node)
        }
        db.close()
    }
}