package com.thecout.cpg.CPGParsing

import com.thecout.cpg.CPGParsing.DotVisitor.DotEdgeVisitor
import com.thecout.cpg.CPGParsing.DotVisitor.DotNodeVisitor
import com.thecout.cpg.CPGParsing.Traversal.ASTForwardStrategy
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import java.io.PrintStream


class CPGtoDot(
    translationUnitDeclarations: List<TranslationUnitDeclaration>,
    private var pout: PrintStream = System.out
) : IGraphParser(translationUnitDeclarations) {


    /**
     * Entrypoint to print CPG as Dot
     * Sets up a Digraph called G and adds the annotated nodes
     */
    override fun processGraph() {
        pout.println(String.format("digraph G {"))
        translationUnitDeclarations.forEach {
            it.accept(ASTForwardStrategy(), DotNodeVisitor(pout))
            it.accept(ASTForwardStrategy(), DotEdgeVisitor(pout))
        }
        pout.println("}")
    }

}

