package com.thecout.cpg.Passes.Services

import com.thecout.cpg.GraphManagement.DB.Repository
import com.thecout.cpg.GraphManagement.Model.Node
import com.thecout.cpg.Passes.Queries.GetNodesByFileAndLine

class FindNodesByFileAndLine(val files: List<String>, val lines: List<Int>) : Pass {
    override fun run(repository: Repository): List<Node> {
        return repository.nodeQuery(GetNodesByFileAndLine(files, lines))
    }
}