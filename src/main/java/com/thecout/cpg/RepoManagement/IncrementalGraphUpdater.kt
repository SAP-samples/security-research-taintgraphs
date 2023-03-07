package com.thecout.cpg.RepoManagement

import com.thecout.cpg.CPGParsing.CPGtoDB
import com.thecout.cpg.CPGParsing.Traversal.IdentifierPass
import com.thecout.cpg.GraphManagement.DB.Repository
import com.thecout.cpg.GraphManagement.Model.Node
import com.thecout.cpg.Passes.Queries.NodeSearchQuery
import com.thecout.cpg.Passes.Services.AddIDFGEdges
import com.thecout.cpg.Util.Timing
import de.daslaboratorium.machinelearning.classifier.bayes.BayesClassifier
import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.TranslationManager
import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.graph.TypeManager
import org.slf4j.LoggerFactory
import java.io.*
import java.util.concurrent.TimeUnit
import kotlin.streams.toList

class IncrementalGraphUpdater(
    var dbRepository: Repository,
    var currentCommit: String,
    private var tmppath: String,
    url: String
) {
    var symbols: MutableMap<String, String> = mutableMapOf()
    var gitRepo: Repo = Repo(url, tmppath)
    var parsedFileHistory: MutableSet<String> = mutableSetOf()
    private var classifier: BayesClassifier<String, Boolean>
    private var i = 0

    init {
        tmppath = File(tmppath).absolutePath
        gitRepo.clone(currentCommit)
        classifier = BayesClassifier<String, Boolean>()
        loadClassifier()
    
}


    fun isFileInteresting(file: String): Boolean {
        val classification = classifier.classify(listOf(file))
	return true;
//        return  i < 30 || classification == null || (classification.category == true && classification.probability > THRESHOLD)
    }

    fun trainClassifier(files: List<String>, isInteresting: Boolean) {
        classifier.learn(isInteresting, files)
    }

    fun isFileInChanges(file: String, fixChanges: List<String>): Boolean {
        return fixChanges.contains(file)
    }

    fun serializeClassifier() {
        if (File("$tmppath/classifier.ser").exists()) {
            File("$tmppath/classifier.ser").delete()
        }
	val fileOutputStream = FileOutputStream("$tmppath/classifier.ser")
        val objectOutputStream = ObjectOutputStream(fileOutputStream)
        objectOutputStream.writeObject(classifier)
        objectOutputStream.flush()
        objectOutputStream.close()
    }

    fun loadClassifier() {
        if (File("$tmppath/classifier.ser").exists()) {
            val fileInputStream = FileInputStream(tmppath + "/classifier.ser")
            val objectInputStream = ObjectInputStream(fileInputStream)
            classifier = objectInputStream.readObject() as BayesClassifier<String, Boolean>
        }
    }

    fun increment(newCommit: String, fixChanges: List<String> = listOf()): List<String> {
        val changes = gitRepo.compare(
            currentCommit,
            newCommit
        )
        val originalChanges = changes.first
        var changedFiles =
            (changes.first)
                .filter { cExtensions.contains(it.substringAfterLast(".")) }
                .filter { isDirSkippable(it) }
                .distinct()
        changedFiles = (changedFiles +
            fixChanges
                .filter { cExtensions.contains(it.substringAfterLast(".")) }
                .filter { isDirSkippable(it) }
                ).distinct()
        
	val sizeOfFilesAfter = changedFiles.size
        log.info("Reduced file update by: ${sizeOfFilesAfter.toDouble() / originalChanges.size.toDouble()}")
        log.info("changes in ${changedFiles.size} Files")
        val changedFilePaths = mutableListOf<String>()
        changedFiles.parallelStream().forEach { change ->
            val fileContent = gitRepo.getFile(newCommit, change)
            val tempFilePath = "$tmppath/$change"
            writeContentTo(tempFilePath, fileContent)
            synchronized(changedFilePaths) {
                changedFilePaths.add(tempFilePath)
            }
        }

        val timer = Timing()
        timer.start()
  	var i = 0
        val translationsResults = changedFiles.parallelStream().map { change ->
            val tr = getTranslationResult(
                File("$tmppath/$change"),
                symbols,
                gitRepo.path + "/" + gitRepo.repoName,
                toplevel = File(tmppath)
            ) ?: return@map null
            i += 1
	    tr.file = change
            print("${i} files parsed\n")
	    tr
        }.toList().filterNotNull()
        timer.stop()

        log.info("Translated files in ${timer.getTime()}")
        changedFiles.forEach { change ->
            dbRepository.deleteNode(NodeSearchQuery(Node(file = change.trim())))
        }

        timer.start()
        translationsResults.parallelStream().forEach { change ->
            loadCpg(change, dbRepository, tmppath)
        }
        timer.stop()
        log.info("Loaded files in ${timer.getTime()}")

        if (changedFiles.isNotEmpty()) {
            AddIDFGEdges(changedFiles).run(dbRepository)
        }
	changedFiles.forEach {
	File("$tmppath/$it").delete()
}
        parsedFileHistory.addAll(changedFiles)
        currentCommit = newCommit
        i += 1
        return changedFiles
    }

    private fun writeContentTo(filePath: String, content: String) {
        val file = File(filePath)
        if (!file.parentFile.exists()) {
            file.parentFile.mkdirs()
        }
        file.writeText(content)
    }

    companion object {
        private const val THRESHOLD = 0.0
        private val log = LoggerFactory.getLogger(IncrementalGraphUpdater::class.java)

        val cExtensions = arrayOf("c", "cpp", "cc", "cxx", "java")
        val skipDirs = arrayOf("doc", "docs", "examples", "test", "fuzz", "example", "tests", "tools", "presets")

        fun isDirSkippable(dir: String): Boolean {
            return !skipDirs.any { dir.contains(it) }
        }

        fun parseFile(
            filePath: File, symbols: MutableMap<String, String>, blackList: String? = null, toplevel: File
        ): TranslationManager {
            val translationConfiguration = TranslationConfiguration.builder()
                .sourceLocations(filePath)
                .topLevel(toplevel)
                .loadIncludes(false)
                .defaultPasses()
                .defaultLanguages()
                .debugParser(false)
                .failOnError(false)
                .symbols(symbols)
                .typeSystemActiveInFrontend(false)
                .registerPass(IdentifierPass())
            if (blackList != null) {
                translationConfiguration.includeBlacklist(blackList)
            }
            return TranslationManager.builder().config(translationConfiguration.build()).build()
        }

        fun parseFile(
            filePaths: List<File>,
            symbols: MutableMap<String, String>,
            blackList: String? = null
        ): TranslationManager {
            val translationConfiguration = TranslationConfiguration.builder()
                .sourceLocations(filePaths)
                .loadIncludes(false)
                .defaultLanguages()
                .debugParser(false)
                .failOnError(false)
                .symbols(symbols)
                .useParallelFrontends(false) //if concurrency issues false
                .typeSystemActiveInFrontend(false)
                .defaultPasses()
                .registerPass(IdentifierPass())


            return TranslationManager.builder().config(translationConfiguration.build()).build()
        }

        fun loadCpg(
            translationResult: TranslationResult,
            repository: Repository,
            path: String,
        ) {
            val cpg = CPGtoDB(translationResult.translationUnits, repository)
            cpg.path = "$path/"
            cpg.processGraph()
        }

        fun getTranslationResult(
            filePath: File,
            symbols: MutableMap<String, String>,
            blackList: String? = null,
            toplevel: File = filePath.parentFile
        ): TranslationResult? {
            val manager = parseFile(filePath, symbols, blackList, toplevel)
            return try {
                val res = manager.analyze()
                res.get(60, TimeUnit.SECONDS)
            } catch (e: Exception) {
		e.printStackTrace()
                log.info("Could not parse file $filePath: $e")
                manager.passes.forEach { it.cleanup() }
                null
            } finally {
                TypeManager.reset()
            }
        }

        fun getTranslationResult(
            filePath: List<File>,
            symbols: MutableMap<String, String>,
            blackList: String? = null
        ): TranslationResult? {
            val manager = parseFile(filePath, symbols, blackList)
            return try {
                manager.analyze().get(60, TimeUnit.SECONDS)
            } catch (e: Exception) {
                log.info("Could not parse file $filePath", e)
                manager.passes.forEach { it.cleanup() }
                null
            }
        }
    }
}
