package com.thecout.cpg.CPGParsing.DotVisitor

import com.thecout.cpg.CPGParsing.Traversal.ASTForwardStrategy
import com.thecout.cpg.CPGParsing.Traversal.DomainEvaluator
import com.thecout.cpg.GraphManagement.DB.Repository
import com.thecout.cpg.Util.CPGParsingUtil.Companion.getCodeFeature
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.processing.IVisitor

class DBNodeVisitor(private var repository: Repository, private var path: String) : IVisitor<Node>() {
    var file = ""
    val domainEvaluator = DomainEvaluator()

    private fun accept(node: Node) {
        val modelNode = nodeFeatures(node)
        repository.addNode(modelNode)
    }

    private fun accept(node: TranslationUnitDeclaration) {
        file = node.name
        accept(node as Node)
        try {
            node.accept(ASTForwardStrategy(), domainEvaluator)
        } catch (e: Exception) {
        } catch (e: Error) {
        }
    }

    override fun visit(node: Node) {
        if (node.isInferred){
            return
        }
        when (node) {
            is TranslationUnitDeclaration -> accept(node)
            else -> accept(node)
        }
    }

    private fun nodeFeatures(node: Node): com.thecout.cpg.GraphManagement.Model.Node {
        var startLine = 0
        var endLine = 0
        var startColumn = 0
        var endColumn = 0
        if (node.location != null) {
            startLine = node.location!!.region.startLine
            endLine = node.location!!.region.endLine
            startColumn = node.location!!.region.startColumn
            endColumn = node.location!!.region.endColumn
        }
        file = file.split(path).last()
        val n = com.thecout.cpg.GraphManagement.Model.Node(
            node.id!!,
            node.javaClass.simpleName,
            node.name,
            getCodeFeature(node),
            startLine,
            endLine,
            startColumn,
            endColumn,
            file,
            node.argumentIndex
        )

        if (domainEvaluator.nodeValues[node.id] != null) {
            n.lowerBound = domainEvaluator.nodeValues[node.id]!!.lower.toString()
            n.upperBound = domainEvaluator.nodeValues[node.id]!!.upper.toString()
        }
        return n
    }
}
