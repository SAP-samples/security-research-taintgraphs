package com.thecout.cpg.Util

import com.thecout.cpg.RepoManagement.IncrementalGraphUpdater
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.statements.DeclarationStatement
import java.io.File

class CPGParsingUtil {
    companion object {
        fun sanitizeNodeCode(code: String, max_size: Int = 10000): String {
            if (code.length > max_size) {
                return ""
            }
            val codeWithoutNewLines = code.replace("\n", " ").replace("\r", " ").replace("\t", " ")
            val codeWithoutCurlyBrackets = codeWithoutNewLines.replace("{", "").replace("}", "")
            val codeWithEscapedQuotes = codeWithoutCurlyBrackets.replace("\"", "").replace("\\", "")
            return codeWithEscapedQuotes
        }

        fun isNodeDeclarationStatement(node: Node): Boolean {
            return node is DeclarationStatement
        }

        fun getCodeFeature(node: Node, max_size: Int = 10000): String {
            if (node.code != null || isNodeDeclarationStatement(
                    node
                )
            ) {
                return sanitizeNodeCode(node.code!!, max_size)
            }
            return ""
        }

        /**
         *  get all files recursively with max depth
         */
        fun getAllFiles(path: String, maxDepth: Int): List<String> {
            val files = mutableListOf<String>()
            val dir = File(path)
            if (dir.isDirectory) {
                dir.listFiles().forEach {
                    if (it.isDirectory) {
                        if (maxDepth > 0) {
                            files.addAll(getAllFiles(it.absolutePath, maxDepth - 1))
                        }
                    } else {
                        if (it.extension in IncrementalGraphUpdater.cExtensions) {
                            files.add(it.absolutePath)
                        }
                    }
                }
            }
            return files
        }
    }

}