package com.thecout.cpg

import com.thecout.cpg.GraphManagement.DB.BoltDB
import com.thecout.cpg.GraphManagement.DB.CypherRepository
import com.thecout.cpg.Passes.Services.FindCSinks
import com.thecout.cpg.Passes.Services.FindCSources
import org.junit.jupiter.api.Test

class FindSinksSourceTest {
    @Test
    fun testfindSinksSources() {
        val db = BoltDB("bolt://localhost:7687", "", "")
        val repository = CypherRepository(db, reset = false)
        println("Sources")
        FindCSources().run(repository).forEach(::println)

        println("Sinks")
        FindCSinks().run(repository).forEach(::println)
    }
}