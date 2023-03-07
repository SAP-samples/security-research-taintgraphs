package com.thecout.cpg.CPGParsing.Traversal

import com.thecout.cpg.CPGParsing.SymbolicExecution.BoxedType
import com.thecout.cpg.CPGParsing.SymbolicExecution.BoxedValue
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.statements.ForStatement
import de.fraunhofer.aisec.cpg.graph.statements.IfStatement
import de.fraunhofer.aisec.cpg.graph.statements.WhileStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.UnaryOperator
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import de.fraunhofer.aisec.cpg.processing.IVisitor
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class DomainEvaluator(

) : IVisitor<Node>() {
    private val log: Logger
        get() = LoggerFactory.getLogger(ValueEvaluator::class.java)
    val nodeValues: MutableMap<Long, BoxedType> = mutableMapOf()

    private val valueEvaluator = ValueEvaluator()
    val cannotEvaluate: (Node?, ValueEvaluator) -> Any? = { node: Node?, _: ValueEvaluator ->
        node?.name ?: CouldNotResolve()
    }

    fun setOccurencesToRange(scope: Node, boxedType: BoxedType) {
        for (nextNode in SubgraphWalker.flattenAST(scope)) {
            if (nextNode.name == boxedType.id) {
                if (nextNode.id in nodeValues && nodeValues[nextNode.id] != null) {
                    nodeValues[nextNode.id!!] = nodeValues[nextNode.id]!!.merge(boxedType)
                } else {
                    nodeValues[nextNode.id!!] = boxedType
                }
            }
        }
    }

    fun evaluateDomain(node: IfStatement): Any? {
        val condition = node.condition
        var resultList = listOf<BoxedType?>()
        if (condition is BinaryOperator) {
            resultList = evaluateOperator(condition)
        } else if (condition is UnaryOperator) {
            resultList = evaluateOperator(condition)
        }

        for (result in resultList) {
            if (result != null)
                setOccurencesToRange(node.thenStatement, result)

            if (node.elseStatement != null) {
                if (result != null) {
                    setOccurencesToRange(
                        node.elseStatement,
                        BoxedType(result.id, result.upper, result.lower)
                    )
                }
            }
        }
        return null
    }

    fun evaluateDomain(node: WhileStatement): Any? {
        val condition = node.condition
        var resultList = listOf<BoxedType?>()
        if (condition is BinaryOperator) {
            resultList = evaluateOperator(condition)
        } else if (condition is UnaryOperator) {
            resultList = evaluateOperator(condition)
        }
        for (result in resultList) {
            if (result != null)
                setOccurencesToRange(node.statement, result)
        }
        return null
    }

    private fun evaluateDomain(node: ForStatement): Any? {
        val condition = node.condition
        var resultList = listOf<BoxedType?>()
        if (condition is BinaryOperator) {
            resultList = evaluateOperator(condition)
        } else if (condition is UnaryOperator) {
            resultList = evaluateOperator(condition)
        }

        for (result in resultList) {
            if (result != null)
                setOccurencesToRange(node.statement, result)
        }
        return null
    }

    private fun evaluateOperator(condition: UnaryOperator): List<BoxedType?> {
        val returnList = mutableListOf<BoxedType?>()
        if (condition.input is BinaryOperator) {
            val resultList = evaluateOperator(condition.input as BinaryOperator)
            for (result in resultList) {
                if (result != null) {
                    returnList.add(BoxedType(result.id, result.upper, result.lower))
                }
            }
        }

        return returnList
    }

    private fun evaluateOperator(
        condition: BinaryOperator
    ): List<BoxedType?> {
        val returnList = mutableListOf<BoxedType?>()
        if (condition.lhs is BinaryOperator) {
            if ((condition.lhs as BinaryOperator).operatorCode == "||") {
              //  log.error("Cannot evaluate || operator")
                return returnList
            }
            returnList.addAll(evaluateOperator(condition.lhs as BinaryOperator))
        }
        if (condition.rhs is BinaryOperator) {
            if ((condition.rhs as BinaryOperator).operatorCode == "||") {
              //  log.error("Cannot evaluate || operator")
                return returnList
            }
            returnList.addAll(evaluateOperator(condition.rhs as BinaryOperator))
        }

        val lhsResult = evalAndParse(condition.lhs)
        val left = lhsResult.first
        val leftValue = lhsResult.second

        val rhsResult = evalAndParse(condition.rhs)
        val right = rhsResult.first
        val rightValue = rhsResult.second

        var lhs: BoxedType? = null
        var rhs: BoxedType? = null
        when (condition.operatorCode) {
            "==" -> {
                if (leftValue is BoxedValue.Identifier)
                    lhs = BoxedType(left.name, rightValue, rightValue)
                if (rightValue is BoxedValue.Identifier)
                    rhs = BoxedType(right.name, leftValue, leftValue)
            }
            "<", "<=" -> {
                if (leftValue is BoxedValue.Identifier)
                    lhs = BoxedType(left.name, BoxedValue.Unchecked, rightValue)
                if (rightValue is BoxedValue.Identifier)
                    rhs = BoxedType(right.name, leftValue, BoxedValue.Unchecked)
            }
            ">", ">=" -> {
                if (leftValue is BoxedValue.Identifier)
                    lhs = BoxedType(left.name, rightValue, BoxedValue.Unchecked)
                if (rightValue is BoxedValue.Identifier)
                    rhs = BoxedType(right.name, BoxedValue.Unchecked, leftValue)
            }
        }
        returnList.add(lhs)
        returnList.add(rhs)
        return returnList
    }

    private fun evalAndParse(expression: Expression): Pair<Expression, BoxedValue> {

        var value = valueEvaluator.evaluate(expression)
        value = when (value) {
            is CouldNotResolve -> {
                BoxedValue.Identifier(expression.name)
            }
            is Boolean -> {
                BoxedValue.Numerical(if (value) 1 else 0)
            }
            is Number -> {
                BoxedValue.Numerical(value.toLong())
            }
            else -> BoxedValue.Unchecked
        }
        return Pair(expression, value)
    }

    override fun visit(node: Node) {
        when (node) {
            is IfStatement -> evaluateDomain(node)
            is WhileStatement -> evaluateDomain(node)
            is ForStatement -> evaluateDomain(node)
            else -> cannotEvaluate(node, valueEvaluator)
        }
    }

}