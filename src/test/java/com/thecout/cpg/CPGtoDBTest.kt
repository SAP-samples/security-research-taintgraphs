package com.thecout.cpg

import com.thecout.cpg.CPGParsing.CPGtoDB
import com.thecout.cpg.GraphManagement.DB.BoltDB
import com.thecout.cpg.GraphManagement.DB.CypherRepository
import com.thecout.cpg.GraphManagement.Model.Node
import com.thecout.cpg.Passes.Queries.NodeSearchQuery
import com.thecout.cpg.Passes.Queries.Operator
import com.thecout.cpg.Passes.Services.AddIDFGEdges
import com.thecout.cpg.RepoManagement.IncrementalGraphUpdater.Companion.parseFile
import de.fraunhofer.aisec.cpg.TranslationResult
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Paths

class CPGtoDBTest {

    fun getTranslationResult(fileString: String): TranslationResult? {
        val topLevel = Paths.get("src").resolve("test").resolve("resources").toAbsolutePath()
        val path = topLevel.resolve(fileString).toAbsolutePath()
        val file = File(path.toString())
        val translationManager = parseFile(file, mutableMapOf(), null, file.parentFile)
        val translationResult = translationManager.analyze().get()
        return translationResult
    }

    @Test
    fun cpgToDBTest() {
        val db = BoltDB("bolt://localhost:7687", "test", "neo4j")
        val repository = CypherRepository(db)
        val files: List<String> = listOf("test1.c", "test2.c")
        for (fileString in files) {
            val cpgtoDB = CPGtoDB(getTranslationResult(fileString)!!.translationUnits, repository)
            cpgtoDB.processGraph()
        }
        AddIDFGEdges().run(repository)

        db.close()
    }

    @Test
    fun deleteTest() {
        val db = BoltDB("bolt://localhost:7687", "", "")
        val repository = CypherRepository(db, reset = false)
        repository.deleteNode(NodeSearchQuery(Node(file = "test2.c")))
    }
}