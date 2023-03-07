package com.thecout.cpg.GraphManagement.DB

import com.thecout.cpg.GraphManagement.Model.Edge
import com.thecout.cpg.GraphManagement.Model.Node
import com.thecout.cpg.GraphManagement.Model.Path
import com.thecout.cpg.Passes.Queries.NodeSearchQuery
import com.thecout.cpg.Passes.Queries.Query
import org.neo4j.driver.types.Relationship
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import org.neo4j.driver.types.Node as Neo4jNode
import org.neo4j.driver.types.Path as Neo4jPath

class CypherRepository(val db: BoltDB, val reset: Boolean = true) : Repository {
    private val log = LoggerFactory.getLogger(CypherRepository::class.java)
    private val asts: ConcurrentHashMap<String, Boolean> = ConcurrentHashMap()

    init {
        if (reset) {
            db.run("MATCH(n) DETACH DELETE n;")
        }
        db.run("SET GLOBAL TRANSACTION ISOLATION LEVEL READ COMMITTED;")

    }


    private fun mapNode(node: Neo4jNode): Node {
        val nodeMap = node.asMap()
        return Node(
            code = nodeMap["code"] as String,
            id = nodeMap["id"] as Long,
            nameAST = node.labels().first() as String,
            nameParseTree = nodeMap["nameParseTree"] as String,
            file = nodeMap["file"] as String,
            lineStart = (nodeMap["lineStart"] as Long).toInt(),
            lineEnd = (nodeMap["lineEnd"] as Long).toInt(),
            columnStart = (nodeMap["columnStart"] as Long).toInt(),
            columnEnd = (nodeMap["columnEnd"] as Long).toInt(),
            argumentIndex = (nodeMap["argumentIndex"] as Long).toInt(),
            lowerBound = nodeMap["lowerBound"].toString(),
            upperBound = nodeMap["upperBound"].toString(),
        )
    }


    /**
     * Implemented own Search instead of OGM based, since Memgraph has problems with it
     * and it will be easier to integrate redis-graph
     */
    override fun searchNode(searchQuery: NodeSearchQuery): List<Node> {
        val results = db.run(
            "MATCH (n${searchQuery.getLabel()})" +
                    " WHERE ${searchQuery.getQuery("n")}" +
                    " RETURN n;"
        )
        val nodes: MutableList<Node> = mutableListOf()
        for (result in results) {
            nodes.add(mapNode(result.get("n").asNode()))
        }
        return nodes

    }

    override fun deleteNode(searchQuery: NodeSearchQuery) {
        val query = "MATCH (n${searchQuery.getLabel()})" +
                " WHERE ${searchQuery.getQuery("n")}" +
                " DETACH DELETE n;"
        db.run(query)
        log.debug("Deleted nodes with $query")

    }

    override fun addNodes(nodes: List<Node>) {
        for (node in nodes) {
            addNode(node)
        }
    }

    override fun nodeQuery(query: Query): List<Node> {
        val nodes: MutableList<Node> = mutableListOf()

        val results = db.run(
            query.getQuery()
        )
        for (result in results) {
            nodes.add(mapNode(result.get("n").asNode()))
        }

        return nodes
    }

    override fun pathQuery(query: Query, timeOut: Long): List<Path> {
        val pathsResult: MutableList<Path> = mutableListOf()
        freeMemory()
        val results = db.run(
            query.getQuery(),
            timeOut
        )
        for (result in results) {
            val path = result.get("p").asPath();
            pathsResult.add(mapPath(path))
        }
        freeMemory()

        return pathsResult
    }

    private fun freeMemory() {
        try {
            db.run("FREE MEMORY;")
        } catch (e: Exception) {
            log.info("Free memory not supported for this db", e)
        }
    }

    private fun mapEdge(edge: Relationship, mapping: MutableMap<Long, Long>) = Edge(
        Node(mapping[edge.startNodeId()]!!),
        Node(mapping[edge.endNodeId()]!!),
        edge.type(),
        edge.asMap()["branch"].toString()
    )

    private fun mapPath(path: Neo4jPath): Path {
        val mapping: MutableMap<Long, Long> = mutableMapOf()
        val nodes: List<Node> = path.nodes().map {
            val node = mapNode(it)
            mapping[it.id()] = node.id
            node
        }
        return Path(
            path.relationships().map { mapEdge(it, mapping) },
            mapNode(path.start()),
            mapNode(path.end()),
            nodes
        )
    }

    override fun nodeQuery(query: Query, binding: String): List<Node> {
        return nodeQuery(query)
    }


    override fun addNode(node: Node): Node {
        asts[node.nameAST] = true
        try {
            db.run(
                "CREATE (c:${node.nameAST} {code: \"${
                    node.code.replace(
                        "\\",
                        "\\\\"
                    )
                }\", columnEnd: ${node.columnEnd}," +
                        "columnStart: ${node.columnStart}, lineEnd: ${node.lineEnd}, lineStart: ${node.lineStart}, nameParseTree: \"${node.nameParseTree}\"," +
                        " id: ${node.id}, file: \"${
                            node.file.replace(
                                "\\",
                                "\\\\"
                            )
                        }\" , argumentIndex: ${node.argumentIndex}, lowerBound: \"${node.lowerBound}\", upperBound: \"${node.upperBound}\"});",

                )
        } catch (e: Exception) {
            log.error(e.message)
        }
        return node
    }


    override fun addEdge(a: Node, b: Node, type: String, branch: String) {
        db.run(
            "MATCH (c1:${a.nameAST} { id: ${a.id} }) " +
                    "MATCH (c2:${b.nameAST} { id: ${b.id} }) " +
                    "MERGE (c1)-[:${type.uppercase()} {branch: \"${branch}\"}]->(c2);"
        )
    }


    override fun createIndex() {
        for (label in asts.keys()) {
            db.run("CREATE INDEX ON :$label(id);")
            db.run("CREATE INDEX ON :$label(nameParseTree);")
            db.run("CREATE INDEX ON :$label(file);")
            db.run("CREATE CONSTRAINT ON (c:$label) ASSERT c.id IS UNIQUE;")
        }
    }

    override fun query(query: Query): Any {
        return db.run(query.getQuery())
    }


}
