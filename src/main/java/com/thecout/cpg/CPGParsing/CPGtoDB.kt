package com.thecout.cpg.CPGParsing

import com.thecout.cpg.CPGParsing.DotVisitor.DBEdgeVisitor
import com.thecout.cpg.CPGParsing.DotVisitor.DBNodeVisitor
import com.thecout.cpg.CPGParsing.Traversal.ASTForwardStrategy
import com.thecout.cpg.GraphManagement.DB.Repository
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration

class CPGtoDB(
    translationUnitDeclarations: List<TranslationUnitDeclaration>,
    private var repository: Repository
) : IGraphParser(translationUnitDeclarations) {
    var path: String = "/"

    /**
     * Entrypoint to import CPG to DB
     */
    override fun processGraph() {
        translationUnitDeclarations.parallelStream().forEach {
            val edgeVisitor = DBEdgeVisitor(repository)
            it.accept(ASTForwardStrategy(), DBNodeVisitor(repository, path))
            repository.createIndex()
            it.accept(ASTForwardStrategy(), edgeVisitor)
        }


    }

}
