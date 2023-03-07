package com.thecout.cpg.CPGParsing.DotVisitor

import com.thecout.cpg.GraphManagement.DB.Repository
import de.fraunhofer.aisec.cpg.graph.DeclarationHolder
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.StatementHolder
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.edge.Properties
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge
import de.fraunhofer.aisec.cpg.graph.statements.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.types.PointerType
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import de.fraunhofer.aisec.cpg.processing.IVisitor
import com.thecout.cpg.GraphManagement.Model.Node as DBNode

class DBEdgeVisitor(private var repository: Repository) :
    IVisitor<Node>() {

    fun accept(node: VariableDeclaration) {
        if (node.initializer != null) {
            addEdge(node, listOf(node.initializer!!), "DFG")
        }
        for (dfs in node.nextDFG) {
            if (dfs is DeclaredReferenceExpression) {
                addEdge(dfs, listOf(node), "DDG")
            }
        }
    }

    /**
     * This comes in handy to find double free errors.
     */
    fun accept(node: DeleteExpression) {
        addEdge(node.operand, listOf(node), "DFG")
    }

    fun accept(node: KeyValueExpression) {
        if (node.key != null) {
            addEdge(node.key!!, listOf(node), "DFG")
        }
        if (node.value != null) {
            addEdge(node.value!!, listOf(node), "DFG")
        }
        if (node.value != null && node.key != null) {
            addEdge(node.key!!, listOf(node.value!!), "DFG")
        }
    }

    fun accept(node: ArraySubscriptionExpression) {
        addEdge(node, listOf(node.subscriptExpression), "DDG")
        addEdge(node, listOf(node.arrayExpression), "DDG")
    }

    fun accept(node: ArrayRangeExpression) {
        addEdge(node.ceiling, listOf(node), "DFG")
        addEdge(node.floor, listOf(node), "DFG")
        addEdge(node, listOf(node.ceiling), "DDG")
        addEdge(node, listOf(node.floor), "DDG")
    }

    fun accept(node: BinaryOperator) {
        addEdge(node.lhs, listOf(node), "DFG")
        addEdge(node.rhs, listOf(node), "DFG")
    }

    /**
     * A unary operator like "-", "!" or "&" in C needs some clarification here:
     * 1. the input data flows into the operator node
     * 2. the operator node is data dependand on the input, since it could be a reference. We need to further traverse that node
     * 3. if its previous CFG node is a CallExpression, the function call could change the referred object.
     */
    fun accept(node: UnaryOperator) {
        addEdge(node.input, listOf(node), "DFG")
        if (node.operatorCode == "&") {
            addEdge(node, listOf(node.input), "DDG")
        }

    }

    /**
     * Since our Path query starts with a CallExpression, we need a way to traverse forward.
     * Thats why we say the Call flows to its arguments. But Let's call that DDG to avoid confusion.
     */
    fun accept(node: CallExpression) {
        addEdge(node, SubgraphWalker.getAstChildren(node), "AST")
        addEdge(node, node.invokes, "CG")
        for (argument in node.arguments) {
            addEdge(argument, listOf(node), "DFG")
        }
        for (argument in node.arguments) {
            if (argument is Expression && argument.type is PointerType && argument !is Literal<*>) {
                addEdge(node, listOf(argument), "DDG")
            }
        }

    }

    /**
     * Although a DeclaredReferenceExpression is not strictly flowing to its declaration,
     * we add a DDG edge to make it possible to backtrace to the declaration and move on from there
     */
    fun accept(node: DeclaredReferenceExpression) {
        if (node.refersTo != null) {
            addEdge(node, listOf(node.refersTo!!), "DFG")
            addEdge(node.refersTo!!, listOf(node), "DDG")
        }
    }

    /**
     * This is gonna be needed for Java
     */
    fun accept(node: DeclarationHolder) {
        addEdge(node as Node, node.declarations, "isDeclarationHolderOf")
    }

    fun accept(node : FunctionDeclaration) {
        addEdge(node, SubgraphWalker.getAstChildren(node), "AST")
    }

    /**
     * This is gonna be needed for Java
     */
    fun accept(node: StatementHolder) {
        addEdge(node as Node, node.statements, "isStatementHolderOf")
    }

    fun accept(node: MemberExpression) {
        addEdge(node as Node, listOf(node.base), "DFG")
        node.astChildren.forEach {
            addEdge(it, listOf(node), "DDG")
        }

    }

    fun accept(node: CastExpression) {
        addEdge(node, node.prevEOG, "DDG")
    }

    fun acceptConditional(node: Node) {
        addEdge(node, SubgraphWalker.getAstChildren(node), "AST")
    }


    override fun visit(node: Node) {
        addEdge(node, node.nextEOG, "CFG", node.nextEOGEdges)
        addEdge(node, node.nextDFG, "DFG")
        //addEdge(node, SubgraphWalker.getAstChildren(node), "AST")
        addEdge(node, node.annotations, "Annotation")

        when (node) {
            is CastExpression -> accept(node as CastExpression)
            is MemberExpression -> accept(node as MemberExpression)
            is VariableDeclaration -> accept(node as VariableDeclaration)
            is KeyValueExpression -> accept(node as KeyValueExpression)
            is DeleteExpression -> accept(node as DeleteExpression)
            is ArraySubscriptionExpression -> accept(node as ArraySubscriptionExpression)
            is ArrayRangeExpression -> accept(node as ArrayRangeExpression)
            is CallExpression -> accept(node as CallExpression)
            is DeclaredReferenceExpression -> accept(node as DeclaredReferenceExpression)
            is IfStatement -> acceptConditional(node)
            is ForStatement -> acceptConditional(node)
            is WhileStatement -> acceptConditional(node)
            is ForEachStatement -> acceptConditional(node)
            is SwitchStatement -> acceptConditional(node)
            is CaseStatement -> acceptConditional(node)
            is DoStatement -> acceptConditional(node)
            is FunctionDeclaration -> accept(node as FunctionDeclaration)
            is BinaryOperator -> accept(node as BinaryOperator)
            is UnaryOperator -> accept(node as UnaryOperator)

            //is DeclarationHolder -> accept(node as DeclarationHolder)
            //is StatementHolder -> accept(node as StatementHolder)
            else -> return
        }
    }

private fun addEdge(
        srcNode: Node,
        targetNodes: Collection<Node>,
        label: String,
        properties: List<PropertyEdge<Node>> = emptyList()

    ) {
        if (srcNode.id == null || targetNodes.isEmpty() || srcNode.isInferred) {
            return
        }
        val from: Long = srcNode.id ?: return
        var i = 0
        for (targetNode in targetNodes) {
            if (targetNode.id == null || targetNode.isInferred) {
                continue
            }
            val to: Long = targetNode.id ?: return
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
            repository.addEdge(
                DBNode(id = from, nameAST = srcNode.javaClass.simpleName),
                DBNode(id = to, nameAST = targetNode.javaClass.simpleName),
                label,
                propertyString,
            )
            i += 1
        }
    }

}
