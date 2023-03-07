package com.thecout.cpg

import com.thecout.cpg.CPGParsing.SymbolicExecution.BoxedValue
import com.thecout.cpg.CPGParsing.Traversal.ASTForwardStrategy
import com.thecout.cpg.CPGParsing.Traversal.DomainEvaluator
import com.thecout.cpg.CPGParsing.Traversal.IdentifierPass
import com.thecout.cpg.CPGParsing.Traversal.ValueEvaluator
import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.TranslationManager
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import org.junit.jupiter.api.Test
import java.nio.file.Paths

class EvaluatorTest {
    @Test
    fun evaluateTest() {
        val translationUnitDeclarations = getTranslationunits("evaluatorTestSimplifying.c")
        val valueEvaluator = ValueEvaluator()
        translationUnitDeclarations.forEach {
            it.accept(ASTForwardStrategy(), valueEvaluator)
        }
        for (value in valueEvaluator.nodeValues) {
            println(value)
        }
    }

    @Test
    fun evaluateElseTest() {
        val translationUnitDeclarations = getTranslationunits("evaluatorTestBranchingElse.c")
        val valueEvaluator = DomainEvaluator()
        translationUnitDeclarations.forEach {
            it.accept(ASTForwardStrategy(), valueEvaluator)
        }
        assert(valueEvaluator.nodeValues.values.first().lower is BoxedValue.Numerical)
        assert((valueEvaluator.nodeValues.values.first().lower as BoxedValue.Numerical).value == 5)
        assert((valueEvaluator.nodeValues.values.first().upper is BoxedValue.Unchecked))
    }

    @Test
    fun evaluateBranchingTest() {
        val translationUnitDeclarations = getTranslationunits("evaluatorTestBranchingMulti.c")
        val valueEvaluator = DomainEvaluator()
        translationUnitDeclarations.forEach {
            it.accept(ASTForwardStrategy(), valueEvaluator)
        }
        assert(valueEvaluator.nodeValues.values.first().lower is BoxedValue.Numerical)
        assert(valueEvaluator.nodeValues.values.first().upper is BoxedValue.Numerical)
        assert((valueEvaluator.nodeValues.values.first().lower as BoxedValue.Numerical).value == 5)
        assert((valueEvaluator.nodeValues.values.first().upper as BoxedValue.Numerical).value == 5)
    }

    @Test
    fun evaluateScopedTest() {
        val translationUnitDeclarations = getTranslationunits("evaluatorTestBranchingScoped.c")
        val valueEvaluator = DomainEvaluator()
        translationUnitDeclarations.forEach {
            it.accept(ASTForwardStrategy(), valueEvaluator)
        }
        assert(valueEvaluator.nodeValues.values.first().lower is BoxedValue.Unchecked)
        assert((valueEvaluator.nodeValues.values.first().upper as BoxedValue.Numerical).value == 5)
        for (value in valueEvaluator.nodeValues.values) {
            println(value)
        }
    }

    @Test
    fun evaluateIdentifierTest() {
        val translationUnitDeclarations = getTranslationunits("evaluatorTestBranchingIdentifier.c")
        val valueEvaluator = DomainEvaluator()
        translationUnitDeclarations.forEach {
            it.accept(ASTForwardStrategy(), valueEvaluator)
        }
        assert(valueEvaluator.nodeValues.values.first().lower is BoxedValue.Unchecked)
        assert((valueEvaluator.nodeValues.values.first().upper as BoxedValue.Numerical).value == 5)
    }


    private fun getTranslationunits(file: String): List<TranslationUnitDeclaration> {
        val topLevel = Paths.get("src").resolve("test").resolve("resources").toAbsolutePath()
        val paths = listOf(file).map { topLevel.resolve(it).toFile() }
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

        val translationManager = TranslationManager.builder().config(translationConfiguration).build()
        val translationResult = translationManager.analyze().get()
        val translationUnitDeclarations = translationResult!!.translationUnits
        return translationUnitDeclarations
    }
}