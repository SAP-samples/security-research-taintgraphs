package com.thecout.cpg.Util

class HashingUtil {
    companion object {
        /**
         * Simple FNV-like Hash to distinguish nodes without keeping track of their database IDs
         */
        fun properHashcode(
            simpleName: String,
            name1: String,
            codeFeature: String,
            startLine: Int,
            endLine: Int,
            startColumn: Int,
            endColumn: Int,
            file: String,
            argumentIndex: Int
        ): Long {
            var result: Long = 17
            result += 37 * result + simpleName.hashCode()
            result += 37 * result + name1.hashCode()
            result += 37 * result + codeFeature.hashCode()
            result += 37 * result + startLine.hashCode()
            result += 37 * result + endLine.hashCode()
            result += 37 * result + startColumn.hashCode()
            result += 37 * result + endColumn.hashCode()
            result += 37 * result + file.hashCode()
            result += 37 * result + argumentIndex.hashCode()
            return result
        }
    }
}