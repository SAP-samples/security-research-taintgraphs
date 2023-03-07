package com.thecout.cpg.Util

import java.io.File

class SymbolParser {
    companion object {
        // match string in string
        private fun match(str: String, pattern: String): Pair<String?, String?> {
            if (str.contains(pattern)) {
                val tmp = str.replace(pattern, "").trim().split(" ")
                if (tmp.size > 1)
                    return Pair(tmp[0].trim(), tmp[1].trim())
                else
                    return Pair(tmp[0].trim(), "1")

            } else {
                return Pair(null, null)
            }
        }

        // read files line by line
        fun parseSymbols(file: List<File>): MutableMap<String, String> {
            val lines = mutableMapOf<String, String>()
            file.forEach { f ->
                f.forEachLine {
                    val matched = match(it, "#define")
                    if (matched.first != null && matched.second != null) {
                        lines[matched.first!!] = matched.second!!
                    }
                }
            }
            return lines
        }
    }
}