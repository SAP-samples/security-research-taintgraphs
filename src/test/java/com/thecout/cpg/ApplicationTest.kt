package com.thecout.cpg

import com.thecout.cpg.CPGParsing.CPGtoDot
import com.thecout.cpg.CPGParsing.Traversal.IdentifierPass
import com.thecout.cpg.Util.Timing
import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.TranslationManager
import de.fraunhofer.aisec.cpg.TranslationResult
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Paths

class ApplicationTest {

    private var translationResult: TranslationResult? = null

    fun getTranslationResult(paths: List<File>): TranslationManager {
        val translationConfiguration = TranslationConfiguration
            .builder()
            .sourceLocations(paths)
            .defaultPasses()
            .defaultLanguages()
            .processAnnotations(true)
            .codeInNodes(true)
            .debugParser(false)
            .failOnError(false)
            .useParallelFrontends(true)
            .typeSystemActiveInFrontend(false)
            .registerPass(IdentifierPass())
            .build()
        return TranslationManager.builder().config(translationConfiguration).build()
    }
    @Test
    fun getGraph() {
        val timer = Timing()
        val topLevel = Paths.get("src").resolve("test").resolve("resources").toAbsolutePath()
        val paths = listOf("hqxvlc.c").map { topLevel.resolve(it).toFile() }
        val translationConfiguration = TranslationConfiguration
            .builder()
            .sourceLocations(paths)
            .defaultPasses()
            .defaultLanguages()
            .processAnnotations(true)
            .debugParser(false)
            .failOnError(false)
            .useParallelFrontends(true)
            .typeSystemActiveInFrontend(false)
            .loadIncludes(false)
            .registerPass(IdentifierPass())
            .build()

        val translationManager = TranslationManager.builder().config(translationConfiguration).build()
        translationResult = translationManager.analyze().get()
        timer.stop()
        println("Time: ${timer.getTime()}")
        val translationUnitDeclarations = translationResult!!.translationUnits
        // Only to remove duplicated elements in the translationUnitDeclarations
        val converter = CPGtoDot(translationUnitDeclarations)
        converter.processGraph()
    }

}