package com.thecout.cpg

import com.thecout.cpg.GraphManagement.DB.BoltDB
import com.thecout.cpg.GraphManagement.DB.CypherRepository
import com.thecout.cpg.GraphManagement.DB.Repository
import com.thecout.cpg.GraphManagement.Model.Node
import com.thecout.cpg.Passes.Queries.BenchmarkPath
import com.thecout.cpg.Util.Timing
import org.junit.jupiter.api.Test

class BenchmarkTest {

    fun createErdosRenyiGraph(repository: Repository, m: Int, p: Float) {
        val timing = Timing()

        timing.start()

        listOf(0..m).flatten().parallelStream().forEach { i ->
            repository.addNode(Node(id = i.toLong(), nameAST = "Test"))
        }
        repository.createIndex()
        timing.stop()
        println("added $m nodes in ${timing.getTime()}ms")

        timing.start()
        var k = 0
        listOf(0..m).flatten().parallelStream().forEach { i ->
            for (j in 0..m) {
                if (i != j && Math.random() < p) {
                    repository.addEdge(
                        Node(id = i.toLong(), nameAST = "Test"),
                        Node(id = j.toLong(), nameAST = "Test"),
                        type = "DFG"
                    )
                    k++
                }
            }
        }
        timing.stop()

        println("added $k edges in ${timing.getTime()}ms")
    }

    fun runMemgraphPath(m: Int, p: Float, depth: Int) {
        val repository = CypherRepository(BoltDB("bolt://localhost:7687", "", ""))
        createErdosRenyiGraph(repository, m, p)
        repository.nodeQuery(BenchmarkPath(depth, 10))
    }

    @Test
    fun benchmarkNodeCreationMemgraphTest() {
        val repository = CypherRepository(BoltDB("bolt://localhost:7687", "", ""))
        val ms = listOf(10, 100, 1000, 10000, 100000, 1000000)
        for (m in ms) {
            for (i in 0 until m) {
                repository.addNode(Node())
            }

        }
    }

    @Test
    fun benchmarkPathTraversalTest() {
        val m = listOf(10, 100, 1000, 10000)
        val p = listOf(0.1f)
        val depth = listOf(10)
        for (i in m) {
            for (j in p) {
                for (k in depth) {
                    runMemgraphPath(i, j, k)
                }
            }
        }
    }
}