package com.thecout.cpg.CPGParsing.DotVisitor

import com.thecout.cpg.Util.CPGParsingUtil.Companion.getCodeFeature
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.processing.IVisitor
import java.io.PrintStream

class DotNodeVisitor(private var pout: PrintStream = System.out) : IVisitor<Node>() {

    fun accept(node: Node) {
        var location = " "
        if (node.location != null) {
            location = String.format(
                "%d:%d %d:%d",
                node.location!!.region.startLine,
                node.location!!.region.endLine,
                node.location!!.region.startColumn,
                node.location!!.region.endColumn
            )
        }

        pout.println(
            String.format(
                "\"%d\" [label = \"%s\",lexeme = \"%s\", enclosing=\"%s\", location=\"%s\"]",
                node.id,
                node.javaClass.simpleName,
                node.name,
                getCodeFeature(node),
                location
            )
        )
    }

    override fun visit(node: Node) {
        when (node) {
            else -> accept(node)
        }
    }
}