package com.thecout.cpg.CPGParsing.DotVisitor

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.edge.Properties
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import de.fraunhofer.aisec.cpg.processing.IVisitor
import java.io.PrintStream

class DotEdgeVisitor(private var pout: PrintStream = System.out) : IVisitor<Node>() {


    fun accept(node: CallExpression) {
        printEdgeNodes(node, node.invokes, "CG")
        accept(node as Node)
    }

    fun accept(node: Node) {
        printEdgeNodes(node, node.nextEOG, "CFG", node.nextEOGEdges)
        printEdgeNodes(node, node.nextDFG, "DFG")
        printEdgeNodes(node, SubgraphWalker.getAstChildren(node), "AST")
        printEdgeNodes(node, node.annotations, "Annotation")
    }

    override fun visit(node: Node) {
        accept(node)
        when (node) {
            is CallExpression -> accept(node)
            else -> return
        }
    }


    private fun printEdgeNodes(
        srcNode: Node,
        targetNodes: Collection<Node>,
        label: String,
        properties: List<PropertyEdge<Node>> = emptyList()
    ) {
        val srcId: Long? = srcNode.id
        if (srcId != null) {
            var i = 0
            for (targetNode in targetNodes) {
                val targetId: Long? = targetNode.id
                val propertyString = extractProperty(properties, i)
                if (targetId != null) {
                    pout.println(
                        String.format(
                            "  \"%d\" -> \"%d\" [label = \"%s\", properties = \"%s\"]",
                            srcId,
                            targetId,
                            label,
                            propertyString.trim()
                        )
                    )
                }
                i += 1
            }
        }
    }

    private fun extractProperty(
        properties: List<PropertyEdge<Node>>,
        i: Int
    ): String {
        var propertyString = ""
        val property = properties.getOrNull(i)
        if (property != null && !property.equals("null")) {
            var prob = property.getProperty(Properties.BRANCH)
            if (prob != null && prob != "null") {
                propertyString += String.format("%s ", prob)
            }
            prob = property.getProperty(Properties.INSTANTIATION)
            if (prob != null && prob != "null") {
                propertyString += String.format("%s ", prob)
            }
        }
        return propertyString
    }

}