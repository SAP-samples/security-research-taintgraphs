package com.thecout.cpg

import com.thecout.cpg.CPGParsing.CPGtoDot
import com.thecout.cpg.GraphManagement.DB.BoltDB
import com.thecout.cpg.GraphManagement.DB.CypherRepository
import com.thecout.cpg.GraphManagement.DB.Repository
import com.thecout.cpg.GraphManagement.Model.Node
import com.thecout.cpg.GraphManagement.Model.Path
import com.thecout.cpg.Passes.Services.*
import com.thecout.cpg.RepoManagement.IncrementalGraphUpdater
import com.thecout.cpg.RepoManagement.IncrementalGraphUpdater.Companion.getTranslationResult
import com.thecout.cpg.RepoManagement.IncrementalGraphUpdater.Companion.isDirSkippable
import com.thecout.cpg.RepoManagement.IncrementalGraphUpdater.Companion.loadCpg
import com.thecout.cpg.Util.CPGParsingUtil.Companion.getAllFiles
import com.thecout.cpg.Util.SymbolParser
import com.thecout.cpg.Util.Timing
import de.fraunhofer.aisec.cpg.TranslationResult
import org.slf4j.LoggerFactory
import picocli.CommandLine
import java.io.File
import java.io.PrintStream
import java.nio.file.Paths
import java.util.concurrent.Callable
import kotlin.io.path.exists
import kotlin.io.path.readLines
import kotlin.streams.toList
import kotlin.system.exitProcess


class Application : Callable<Int> {

    @CommandLine.Option(
        names = ["-f", "--file"],
        description = ["The path to analyze. "]
    )
    private var file: String? = null

    @CommandLine.Option(
        names = ["-g", "--gitFile"],
        description = ["The path to output. "]
    )
    private var gitFile: String? = null

    @CommandLine.Option(
        names = ["-t", "--tmpPath"],
        description = ["The path to clone git repo to. Defaults to /tmp "]
    )
    private var tmppath: String = "/tmp"

    @CommandLine.Option(
        names = ["-o", "--output"],
        description = ["The path to output. "]
    )
    private var output: String? = null


    @CommandLine.Option(
        names = ["--host"],
        description = ["host"]
    )
    private var host: String? = null

    @CommandLine.Option(
        names = ["--port"],
        description = ["port"]
    )
    private var port: Int? = null

    @CommandLine.Option(
        names = ["--protocol"],
        description = ["protocol, currently: bolt"]
    )
    private var protocol: String? = null

    @CommandLine.Option(
        names = ["--inference"],
        description = ["inference, if true paths wont intersect with commit changes"]
    )
    private var inference: Boolean = false

    private val log = LoggerFactory.getLogger(Application::class.java)


    /**
     * Call Dot Printer with appropriate configuration
     * @throws IllegalArgumentException, if there was no arguments provided, or the path does not
     * point to a file, is a directory or point to a hidden file or the paths does not have the
     * same top level path
     */
    @Throws(Exception::class, IllegalArgumentException::class)
    override fun call(): Int {
        if (gitFile != null && host != null && port != null && protocol != null && output != null) {
            parseGit()
            return 0
        } else if (file != null && output != null) {
            parseFile()
            return 0
        } else if (file != null && host != null && port != null && protocol != null) {
            parseFileToDb()
            return 0
        }

        throw IllegalArgumentException("Wrong argument usage! Either Git mode, File mode or DB mode must be used!")

    }



    private fun parseFileToDb(repo: Repository? = null, incrementalGraphUpdater: IncrementalGraphUpdater? = null): MutableMap<String, String> {
        var repository: Repository? = repo
        if (repository == null) {
            repository = repository()
        }
        val path = Paths.get(file!!).toAbsolutePath().normalize()
        val file = File(path.toString())
        val symbols: MutableMap<String, String>
        if (!file.exists()) {
            throw IllegalArgumentException("File does not exist")
        }
        if (file.isDirectory) {



            var files = getAllFiles(path.toString(), 1).filter { isDirSkippable(it) && IncrementalGraphUpdater.cExtensions.contains(it.substringAfterLast(".")) }
  	 var count = files.size
          if (incrementalGraphUpdater != null){
                files = files.filter { incrementalGraphUpdater.isFileInteresting(it) }.distinct()
	log.info("loading ${files.size} from $count files")
            }
            symbols = SymbolParser.parseSymbols(files.map { File(it) })
            val timer = Timing()
            timer.start()
            log.info("Parsing  ${files.size} files")
            val translationResults = files.parallelStream().map {
                val tr = getTranslationResult(File(it), symbols, toplevel = path.toFile()) ?: return@map null
                tr.file = it
                tr
            }.toList().filterNotNull()
            timer.stop()
            log.info("Parsing  ${files.size} files took ${timer.getTime()} ms")
            timer.start()
            translationResults.parallelStream().forEach {
                try {
                    loadCpg(it, repository, path.toString())
                } catch (e: Exception) {
                }
            }
            timer.stop()
            log.info("Loading ${files.size} files took ${timer.getTime()} ms")

        } else {
            log.info("Parsing file: ${file.absolutePath}")
            symbols = SymbolParser.parseSymbols(listOf(file))
            val tr = getTranslationResult(file, symbols)
            if (tr != null) {
                tr.file = file.absolutePath
                loadCpg(tr, repository, path.toString())
            }
        }
        AddIDFGEdges().run(repository)
        return symbols
    }
    private fun parseFile(): Int {
        val path = Paths.get(file!!).toAbsolutePath().normalize()
        val file = File(path.toString())
        val outputPath: File? = getValidOutputpath()
        if (!file.exists()) {
            throw IllegalArgumentException("File does not exist")
        }
        if (file.isDirectory) {
            val files = getAllFiles(path.toString(), 10)
            val symbols = SymbolParser.parseSymbols(files.map { File(it) })
            val timer = Timing()
            timer.start()
            log.info("Parsing ${files.size} files ")
            val translationResults = getTranslationResult(files.map { File(it) }, symbols)
            timer.stop()
            log.info("Parsing  ${files.size} files took ${timer.getTime()} ms")
            buildDotCpg(translationResults!!, outputPath)
        } else {
            log.info("Parsing file: ${file.absolutePath}")
            val symbols = SymbolParser.parseSymbols(listOf(file))
            val tr = getTranslationResult(file, symbols)
            if (tr != null) {
                tr.file = file.nameWithoutExtension
                buildDotCpg(tr, outputPath)
            }
        }
        return 0
    }
    private fun hashmapIntersect(a: HashMap<String, ArrayList<Int>>, b: HashMap<String, ArrayList<Int>>): Boolean {
        var result = false
        for (k in a.keys){
            if (b.containsKey(k)) {
                result = result || (a.get(k)!!.intersect(b.get(k)!!)).isNotEmpty()
            }
        }
        return result
    }
private fun parseGit(): Int {
        val repo = repository()
        val path = Paths.get(gitFile!!).toAbsolutePath().normalize()
        val outputPath: File? = getValidOutputpath()

        if (!path.exists()) {
            throw IllegalArgumentException("GitFile does not exist")
        }
        val lines = path.readLines()
        val currentCommit = lines[1]
        val url = lines[0]
        log.info("Cloning and initializing Repository $url with first commit $currentCommit")
        val incrementalGraphUpdater = IncrementalGraphUpdater(repo, currentCommit, tmppath, url)
        file = tmppath + "/" + incrementalGraphUpdater.gitRepo.repoName
        log.info("Repo lies under $file")
        incrementalGraphUpdater.symbols = parseFileToDb(repo)
        var i = 0
        for (line in lines.subList(2, lines.size)) {
            i += 1
            var pastcommits : ArrayList<String> = ArrayList()
            pastcommits.add(line.trim())
            var n = 0
            while(n < 15){
                pastcommits.add(incrementalGraphUpdater.gitRepo.getParentCommit(pastcommits.last()).trim())
				n += 1
            }
             val changes = incrementalGraphUpdater.gitRepo.compare(pastcommits.get(1), line.trim())
	     var gotit = false 
             for(n in 14 downTo 0){
		if (gotit && n > 0) {
			continue
		}
		log.info("trying $n")
            	var parentCommit = pastcommits.get(n)
		log.info("Working on $parentCommit")
		try {

                    log.info("$i/${lines.size - 2}: Working on commit $line")
                    log.info("  --> incremental update to $parentCommit")
		    var incrementedFiles: List<String>
		    if (n > 0) {	
                    val changesprevious = incrementalGraphUpdater.gitRepo.compare(pastcommits.get(n+1), parentCommit)
                    incrementedFiles = incrementalGraphUpdater.increment(pastcommits.get(n), changesprevious.first+changes.first)
		    } else {
		    incrementedFiles = incrementalGraphUpdater.increment(pastcommits.get(n), changes.first)
		    }
             	     var betweenclean: ArrayList<Long> = ArrayList()
		     for (changedFile in changes.first) {
				betweenclean.addAll(
				    FindNodesByFileAndLine(listOf(changedFile), changes.second[changedFile]!!).run(repo)
					.map { it.id }
				)
		     }
		     if (betweenclean.isEmpty()){
		       log.info("could not find clean changes")
		       continue
		     }
                    log.info(" --> get changes sinks and sources")
                    val sinks = FindCSinks().run(repo).map { it.id }
                    val sources = FindCSources().run(repo).map { it.id }
                    var between: ArrayList<Long> = ArrayList()
		    if (n > 0) {
                    val changesprevious = incrementalGraphUpdater.gitRepo.compare(pastcommits.get(n+1), parentCommit)
		      for (changedFile in changesprevious.first) {
                        between.addAll(
                            FindNodesByFileAndLine(listOf(changedFile), changesprevious.second[changedFile]!!).run(repo)
                                .map { it.id }
                        )
                    	}
		  }


                    if (n > 0) {
                    val changesprevious = incrementalGraphUpdater.gitRepo.compare(pastcommits.get(n+1), parentCommit)
                    if (changesprevious.first.intersect(changes.first).isEmpty()) {
                        print(changes)
			print(changesprevious)
			log.info("no relevant changes found --> skipping")
			continue
                    }
}
                    log.info(
                        " --> found ${changes.first.size} changed files, ${between.size} modified lines, ${betweenclean.size} modified clean lines," +
                                " ${sources.size} user controlled sources and ${sinks.size} potentially critical sinks"
                    )
                    log.info(" --> get paths")
                    if (inference) {
                   //        between = ArrayList<Long>()
                    }
		    var paths: List<Path> = emptyList()
		    if (n > 0) {
                    paths = GetPaths(sources, between, sinks).run(repo)
                    } else {
                    paths = GetPaths(sources, betweenclean, sinks).run(repo)

		    }
		    var nodes: MutableSet<Node> = mutableSetOf()
		    if (n == 0){
                        nodes = Path.writePathsToFile(paths, outputPath, line + "_0")

                    } else {
			gotit = true
                        nodes = Path.writePathsToFile(paths, outputPath, line +"_1")
                    }
                    val filesOnPath = nodes.map { it.file }.distinct()
                    incrementalGraphUpdater.trainClassifier(filesOnPath, true)
                    if (filesOnPath.isNotEmpty()) {
                        incrementalGraphUpdater.trainClassifier(
                            incrementedFiles.subtract(filesOnPath).toList(),
                            false
                        )
                    }
                    incrementalGraphUpdater.serializeClassifier()
                } catch (e: Exception) {
                    log.error("Exception occurred, skipping this one", e)
                    continue
		}
            	}
            }


        return 0
    }


    private fun getValidOutputpath(): File? {
        var outputPath: File? = null
        if (output != null && output != "") {
            outputPath = File(Paths.get(output!!).toAbsolutePath().normalize().toString())
            if (!outputPath.isDirectory) {
                throw IllegalArgumentException("Output path does not exist")
            }
        }
        return outputPath
    }


    private fun repository(): Repository {
        val repository: Repository?

        val db = BoltDB("bolt://$host:$port", "", "")
        repository = CypherRepository(db, true)

        return repository
    }


    private fun buildDotCpg(
        translationResult: TranslationResult,
        outputPath: File?
    ) {
        val cpg: CPGtoDot = if (outputPath != null) {
            val outPutFile = File(outputPath.absolutePath + "/" + translationResult.file + ".cpg")
            CPGtoDot(translationResult.translationUnits, PrintStream(outPutFile.outputStream()))
        } else {
            CPGtoDot(translationResult.translationUnits, System.out)
        }
        cpg.processGraph()
    }
}

fun main(args: Array<String>) {
    val exitCode = CommandLine(Application()).execute(*args)
    exitProcess(exitCode)
}
