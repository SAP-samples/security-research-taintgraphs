package com.thecout.cpg.GraphManagement.Model

import org.slf4j.LoggerFactory
import java.io.File

class Path(
    var edges: List<Edge>,
    var startNode: Node,
    var endNode: Node,
    var nodes: List<Node>
) {
    override fun toString(): String {
        return "$startNode\n$endNode\n${nodes.joinToString("\n")}\n$edges"
    }

    companion object {
        private val log = LoggerFactory.getLogger(Path::class.java)
        fun writePathsToFile(
            paths: List<Path>,
            outputPath: File?,
            name: String
        ): MutableSet<Node> {
            val nodes: MutableSet<Node> = mutableSetOf()
            val edges: MutableSet<Edge> = mutableSetOf()
            if (paths.isEmpty()) {
                log.info("No paths found")
                return nodes
            }
            for (p in paths) {
                nodes.addAll(p.nodes)
                edges.addAll(p.edges)
            }
            val outPutFile = File(outputPath!!.absolutePath + "/" + name + ".cpg")
            log.info(" --> write to file $outPutFile")
            if (outPutFile.exists()) {
                outPutFile.delete()
            }
            outPutFile.writeText("digraph g { \n ${nodes.joinToString("\n")} \n ${edges.joinToString("\n")}} \n }")
            return nodes
        }
    }
}