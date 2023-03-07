package com.thecout.cpg.Passes.Services

import com.thecout.cpg.GraphManagement.DB.Repository
import com.thecout.cpg.GraphManagement.Model.Node
import com.thecout.cpg.Passes.Queries.AddCDGEdges
import com.thecout.cpg.Passes.Queries.AddCallEdgesQuery
import com.thecout.cpg.Passes.Queries.AddIDFGEdge
import com.thecout.cpg.Passes.Queries.AddParamVarDeclEdges
import com.thecout.cpg.Util.Timing
import org.neo4j.driver.exceptions.TransientException
import org.slf4j.LoggerFactory

class AddIDFGEdges(private val fileNames: List<String> = emptyList()) : Pass {
    private val log = LoggerFactory.getLogger(AddIDFGEdges::class.java)

    override fun run(repository: Repository): List<Node> {
        val timer = Timing()
        timer.start()
        val functionDefs = try {
            repository.nodeQuery(AddCallEdgesQuery(fileNames = fileNames))
        } catch (e: TransientException) {
            fileNames.map { repository.nodeQuery(AddCallEdgesQuery(fileNames = listOf(it))) }.flatten()
        }
        timer.stop()
        log.info(" Call Edges added in ${timer.getTime()}ms")

        timer.start()
        repository.nodeQuery(AddCDGEdges(fileNames = fileNames))
        timer.stop()
        log.info(" CDG Edges added in ${timer.getTime()}ms")

        timer.start()
        repository.query(AddParamVarDeclEdges(fileNames = fileNames))
        timer.stop()
        log.info(" ParamVarDecl DFG Edges added in ${timer.getTime()}ms")

        timer.start()
        
        try {
            repository.query(AddIDFGEdge(functionDefs.map { it.id }, fileNames = fileNames))
        } catch (e: Exception) {
            functionDefs.map { it.id }.parallelStream().forEach {
                try {
                    repository.query(AddIDFGEdge(listOf(it), fileNames = fileNames))
                } catch (e: Exception){}
            }
        }
	timer.stop()
        log.info(" Added IDFG edges for ${functionDefs.size} functions in ${timer.getTime()}ms")

        return listOf()
    }
}
