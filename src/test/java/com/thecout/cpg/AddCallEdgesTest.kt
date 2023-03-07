package com.thecout.cpg

import com.thecout.cpg.GraphManagement.DB.BoltDB
import com.thecout.cpg.GraphManagement.DB.CypherRepository
import com.thecout.cpg.Passes.Services.AddIDFGEdges
import org.junit.jupiter.api.Test

class AddCallEdgesTest {
    @Test
    fun addIDFGEdgesTest() {
        val db = BoltDB("bolt://localhost:7687", "", "")
        val repository = CypherRepository(db, reset = false)
        AddIDFGEdges().run(repository)
    }
}