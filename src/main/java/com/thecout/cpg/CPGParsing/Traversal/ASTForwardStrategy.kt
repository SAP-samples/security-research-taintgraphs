package com.thecout.cpg.CPGParsing.Traversal

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.processing.IStrategy
import de.fraunhofer.aisec.cpg.processing.strategy.Strategy

class ASTForwardStrategy : IStrategy<Node> {
    override fun getIterator(v: Node?): MutableIterator<Node> {
        return Strategy.AST_FORWARD(v!!)
    }
}