package com.thecout.cpg

import com.thecout.cpg.CPGParsing.CPGtoDB
import com.thecout.cpg.GraphManagement.DB.BoltDB
import com.thecout.cpg.GraphManagement.DB.CypherRepository
import com.thecout.cpg.GraphManagement.DB.Repository
import com.thecout.cpg.Passes.Services.AddIDFGEdges
import com.thecout.cpg.Passes.Services.FindCSinks
import com.thecout.cpg.Passes.Services.FindCSources
import com.thecout.cpg.Passes.Services.GetPaths
import com.thecout.cpg.RepoManagement.IncrementalGraphUpdater.Companion.parseFile
import org.junit.jupiter.api.Test
import java.nio.file.Paths

class DataflowTest {
    fun getTranslationResult(files: List<String>, repository: Repository) {

        val topLevel = Paths.get("src").resolve("test").resolve("resources").toAbsolutePath()
        val translationManager = parseFile(files.map { topLevel.resolve(it).toAbsolutePath().toFile() }, mutableMapOf())
        CPGtoDB(translationManager.analyze().get().translationUnits, repository).processGraph()

    }

    @Test
    fun len1len2Test() {
        val db = getDB()
        val repository = CypherRepository(db)

        getTranslationResult(listOf("len1len2.c"), repository)
        val (sources, sinks) = getSinkAndSources(repository)
        val paths = GetPaths(sources, listOf(), sinks, withCFG = false).run(repository)
        println("Found ${sinks.size} sinks, ${sources.size} sources ${paths.size} paths")
        assert(paths.isNotEmpty())
        assert(sinks.size == 1) { "Expected 1 sink, found ${sinks.size}" }
        assert(sources.size == 1) { "Expected 1 source, found ${sources.size}" }
    }

    @Test
    fun test1test2() {
        val db = getDB()
        val repository = CypherRepository(db)
        getTranslationResult(listOf("test1.c", "test2.c"), repository)
        val (sources, sinks) = getSinkAndSources(repository)
        AddIDFGEdges().run(repository)
        val paths = GetPaths(sources, listOf(), sinks, withCFG = false).run(repository)
        println("Found ${sinks.size} sinks, ${sources.size} sources ${paths.size} paths")
        assert(paths.isNotEmpty())
        assert(sinks.size == 1) { "Expected 1 sink, found ${sinks.size}" }
        assert(sources.size == 2) { "Expected 2 source, found ${sources.size}" }
    }

    @Test
    fun test1Indirectiontest2() {
        val db = getDB()
        val repository = CypherRepository(db)
        getTranslationResult(listOf("test1Indirection.c", "test2.c"), repository)
        val (sources, sinks) = getSinkAndSources(repository)
        AddIDFGEdges().run(repository)
        val paths = GetPaths(sources, listOf(), sinks, withCFG = false).run(repository)
        println("Found ${sinks.size} sinks, ${sources.size} sources ${paths.size} paths")
        assert(paths.isNotEmpty())
        assert(sinks.size == 1) { "Expected 1 sink, found ${sinks.size}" }
        assert(sources.size == 2) { "Expected 2 source, found ${sources.size}" }
    }

    @Test
    fun test1structtest2() {
        val db = getDB()
        val repository = CypherRepository(db)
        getTranslationResult(listOf("test1struct.c", "test2.c"), repository)
        val (sources, sinks) = getSinkAndSources(repository)
        AddIDFGEdges().run(repository)
        val paths = GetPaths(sources, listOf(), sinks, withCFG = false).run(repository)
        println("Found ${sinks.size} sinks, ${sources.size} sources ${paths.size} paths")
        assert(paths.isNotEmpty())
        assert(sinks.size == 1) { "Expected 1 sink, found ${sinks.size}" }
        assert(sources.size == 2) { "Expected 2 source, found ${sources.size}" }
    }

    @Test
    fun test1structassigntest2() {
        val db = getDB()
        val repository = CypherRepository(db)
        getTranslationResult(listOf("test1structAssign.c", "test2structAssign.c"), repository)
        val (sources, sinks) = getSinkAndSources(repository)
        AddIDFGEdges().run(repository)
        val paths = GetPaths(sources, listOf(), sinks, withCFG = false).run(repository)
        println("Found ${sinks.size} sinks, ${sources.size} sources ${paths.size} paths")
        assert(paths.isNotEmpty())
        assert(sinks.size == 1) { "Expected 1 sink, found ${sinks.size}" }
        assert(sources.size == 2) { "Expected 2 source, found ${sources.size}" }
    }

    @Test
    fun test1structFunctionIndirectiontest2() {
        val db = getDB()
        val repository = CypherRepository(db)
        getTranslationResult(listOf("test1structFunctionIndirection.c", "test2.c"), repository)
        val (sources, sinks) = getSinkAndSources(repository)
        AddIDFGEdges().run(repository)
        val paths = GetPaths(sources, listOf(), sinks, withCFG = false).run(repository)
        println("Found ${sinks.size} sinks, ${sources.size} sources ${paths.size} paths")
        assert(paths.isNotEmpty())
        assert(sinks.size == 1) { "Expected 1 sink, found ${sinks.size}" }
        assert(sources.size == 2) { "Expected 2 source, found ${sources.size}" }
    }

    @Test
    fun evaluateTestBinaryOperator() {
        val db = getDB()
        val repository = CypherRepository(db)
        getTranslationResult(listOf("evaluateTestBinaryOperator.c"), repository)
        val (sources, sinks) = getSinkAndSources(repository)
        AddIDFGEdges().run(repository)
        val paths = GetPaths(sources, listOf(), sinks, withCFG = false).run(repository)
        println("Found ${sinks.size} sinks, ${sources.size} sources ${paths.size} paths")
        assert(paths.isNotEmpty())
        assert(sinks.size == 1) { "Expected 1 sink, found ${sinks.size}" }
        assert(sources.size == 1) { "Expected 1 source, found ${sources.size}" }
    }

    @Test
    fun evaluateCDGBranching() {
        val db = getDB()
        val repository = CypherRepository(db)
        getTranslationResult(listOf("test1.c", "test2CDG.c"), repository)
        val (sources, sinks) = getSinkAndSources(repository)
        AddIDFGEdges().run(repository)
        val paths = GetPaths(sources, listOf(), sinks, withCFG = false).run(repository)
        println("Found ${sinks.size} sinks, ${sources.size} sources ${paths.size} paths")
        assert(paths.isNotEmpty())
        assert(sinks.size == 1) { "Expected 1 sink, found ${sinks.size}" }
        assert(sources.size == 2) { "Expected 2 source, found ${sources.size}" }
    }

    @Test
    fun evaluateCDGBranchingWhile() {
        val db = getDB()
        val repository = CypherRepository(db)
        getTranslationResult(listOf("test1.c", "test2CDGWhile.c"), repository)
        val (sources, sinks) = getSinkAndSources(repository)
        AddIDFGEdges().run(repository)
        val paths = GetPaths(sources, listOf(), sinks, withCFG = false).run(repository)
        println("Found ${sinks.size} sinks, ${sources.size} sources ${paths.size} paths")
        assert(paths.isNotEmpty())
        assert(sinks.size == 1) { "Expected 1 sink, found ${sinks.size}" }
        assert(sources.size == 2) { "Expected 2 source, found ${sources.size}" }
    }

    @Test
    fun evaluateCDGBranching2() {
        val db = getDB()
        val repository = CypherRepository(db)
        getTranslationResult(listOf("test1Branching.c", "test2.c"), repository)
        val (sources, sinks) = getSinkAndSources(repository)
        AddIDFGEdges().run(repository)
        val paths = GetPaths(sources, listOf(), sinks, withCFG = false).run(repository)
        println("Found ${sinks.size} sinks, ${sources.size} sources ${paths.size} paths")
        assert(paths.isNotEmpty())
        assert(sinks.size == 1) { "Expected 1 sink, found ${sinks.size}" }
        assert(sources.size == 2) { "Expected 2 source, found ${sources.size}" }
    }

    @Test
    fun externTest() {
        val db = getDB()
        val repository = CypherRepository(db)
        getTranslationResult(listOf("externTest.c", "externTest2.c"), repository)
        val (sources, sinks) = getSinkAndSources(repository)
        AddIDFGEdges().run(repository)
        val paths = GetPaths(sources, listOf(), sinks, withCFG = false).run(repository)
        println("Found ${sinks.size} sinks, ${sources.size} sources ${paths.size} paths")
        assert(paths.isNotEmpty())
        assert(sinks.size == 1) { "Expected 1 sink, found ${sinks.size}" }
        assert(sources.size == 1) { "Expected 2 source, found ${sources.size}" }
    }

    @Test
    fun externTestNoIDFG() {
        val db = getDB()
        val repository = CypherRepository(db)
        getTranslationResult(listOf("testNoIDFG.c"), repository)
        val (sources, sinks) = getSinkAndSources(repository)
        AddIDFGEdges().run(repository)
        val paths = GetPaths(sources, listOf(), sinks, withCFG = false).run(repository)
        println("Found ${sinks.size} sinks, ${sources.size} sources ${paths.size} paths")
        assert(paths.isNotEmpty())
        assert(sinks.size == 1) { "Expected 1 sink, found ${sinks.size}" }
        assert(sources.size == 2) { "Expected 2 source, found ${sources.size}" }
    }

    private fun getDB(): BoltDB {
        return BoltDB("bolt://localhost:7687", "", "")
    }

    private fun getSinkAndSources(repository: CypherRepository): Pair<List<Long>, List<Long>> {
        val sources = FindCSources().run(repository).map { it.id }
        val sinks = FindCSinks().run(repository).map { it.id }
        for (sink in sinks) {
            println(sink)
        }
        return Pair(sources, sinks)
    }

}