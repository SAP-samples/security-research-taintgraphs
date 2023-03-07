package com.thecout.cpg.CPGParsing.Traversal


import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import de.fraunhofer.aisec.cpg.passes.Pass
import java.util.concurrent.atomic.AtomicLong

/** This [Pass] adds a unique ID to each [Node] in the CPG. */
class IdentifierPass : Pass() {
    companion object {
        var lastId: AtomicLong = AtomicLong(0)
    }

    override fun accept(t: TranslationResult) {
        for (tu in t.translationUnits) {
            handle(tu)
        }
    }

    private fun handle(node: Node) {
        node.id = lastId.incrementAndGet()
        for (child in SubgraphWalker.getAstChildren(node)) {
            handle(child)
        }
    }

    override fun cleanup() {
        // nothing to do
    }
}
