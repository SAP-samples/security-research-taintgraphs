package com.thecout.cpg.Passes.Services

import com.thecout.cpg.GraphManagement.DB.Repository
import com.thecout.cpg.Passes.Queries.Path
import com.thecout.cpg.Util.Timing
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeoutException
import com.thecout.cpg.GraphManagement.Model.Path as DBPath

class GetPaths(
    val from: List<Long>,
    var between: List<Long>,
    var to: List<Long>,
    val withCFG: Boolean = true
) {
    private val log = LoggerFactory.getLogger(GetPaths::class.java)

    fun run(repository: Repository): List<DBPath> {
        val timer = Timing()
        val paths: MutableList<DBPath> = mutableListOf()
        log.info("Calculating Paths")
        timer.start()
        if (between.isEmpty()) { // For Testing
            return getPathWrapper(repository, from, to, withCFG, 150, 150)
        }
	between = between.shuffled().take(2000)
        paths.addAll(getPathWrapper(repository, from, between, false, 160))
        if (paths.isNotEmpty()) {
            paths.addAll(getPathWrapper(repository, between, to, false, 100))
        }


        if (paths.isEmpty()) {
            log.info("No paths found. Retrying with smaller Path Length and CFG")
            paths.addAll(getPathWrapper(repository, from, between, true, 180))
            if (paths.isNotEmpty()) {
                paths.addAll(getPathWrapper(repository, between, to, true, 160))
            }
        }

        timer.stop()
        log.info("Time taken: ${timer.getTime()}ms")
        return paths
    }

    private fun getPathWrapper(
        repository: Repository,
        from: List<Long>,
        to: List<Long>,
        withCFG: Boolean = true,
        pathLength: Int = 50,
        pathLimit: Int = 20
    ): MutableList<com.thecout.cpg.GraphManagement.Model.Path> {
        val paths: MutableList<DBPath> = mutableListOf()
        from.parallelStream().forEach { f ->

            val future = CompletableFuture.supplyAsync {
                repository.pathQuery(Path(f, to, withCFG = withCFG, pathLength = pathLength, limit = pathLimit), timeOut = 20)
            }
            try {
                val path = future.get(2000, java.util.concurrent.TimeUnit.SECONDS)
                synchronized(paths) {
                    paths.addAll(path)
                }
            } catch (e: TimeoutException) {
                future.cancel(true)
            } catch (e: Exception) {
            }
        }

        return paths
    }
}
