package com.thecout.cpg.GraphManagement.Model

import org.neo4j.ogm.annotation.*

@RelationshipEntity
class Edge(
    @StartNode
    val source: Node,
    @EndNode
    val target: Node,
    @Property
    val type: String,
    @Property
    val branch: String
) {

    @Id
    @GeneratedValue
    private var id: Long = 0

    override fun toString(): String {
        return " \"${source.id}\" -> \"${target.id}\" [label = \"$type\", properties = \"$branch\"]"
    }

    fun toDot(): String {
        return toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Edge

        if (source.id != other.source.id) return false
        if (target.id != other.target.id) return false
        if (type != other.type) return false
        if (branch != other.branch) return false

        return true
    }

    override fun hashCode(): Int {
        return source.hashCode() xor target.hashCode() xor type.hashCode() xor branch.hashCode()
    }
}
