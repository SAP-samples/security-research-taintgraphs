package com.thecout.cpg.RepoManagement

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk
import java.io.ByteArrayOutputStream
import java.io.File


class Repo(repo: String, val path: String) {
    var git: Git? = null
    var repoName: String = repo.split("/").last()

    private val branch = "master"

    init {
        if (File("$path/$repoName").exists()) {
            git = Git.open(File("$path/$repoName"))
        } else {
            git = Git.cloneRepository()
                .setURI(repo)
                .setDirectory(File("$path/$repoName"))
                .setBranch(branch)
                .call()
        }
    }

    fun clone(commit: String) {
        git!!.checkout().setName(commit).call()
    }

    fun getParentCommit(commit: String): String {
        val commitId = git!!.repository.resolve(commit)
        RevWalk(git!!.repository).use { revWalk ->
            val revCommit: RevCommit = revWalk.parseCommit(commitId)
            return revCommit.getParent(0).name
        }
    }


    fun compare(commit_a: String, commit_b: String): Pair<List<String>, HashMap<String, ArrayList<Int>>> {
        val oldTree = git!!.repository.resolve(commit_a)
        val newTree = git!!.repository.resolve(commit_b)
        val modifiedLines = HashMap<String, ArrayList<Int>>()
        val modifiedFiles = ArrayList<String>()
        val df = DiffFormatter(ByteArrayOutputStream())
        df.setRepository(git!!.repository)
        val entries = df.scan(oldTree, newTree)
        for (entry in entries) {
            if (entry.newPath != DiffEntry.DEV_NULL) {
                modifiedFiles.add(entry.newPath)
            }
            modifiedLines[entry.newPath] = arrayListOf()
            for (edit in df.toFileHeader(entry).toEditList()) {
                when (entry.changeType) {
                    DiffEntry.ChangeType.MODIFY, DiffEntry.ChangeType.ADD -> {
                        for (i in (edit.beginA - 5)..(edit.endA+5)) {
                            modifiedLines[entry.newPath]!!.add(i)
                        }
                        for (i in (edit.beginB - 5)..(edit.endB+5)) {
                            modifiedLines[entry.newPath]!!.add(i)
                        }
                    }
                    DiffEntry.ChangeType.DELETE -> {
                        modifiedLines[entry.oldPath] = arrayListOf()
                    }
                    DiffEntry.ChangeType.RENAME -> {
                        modifiedLines[entry.oldPath] = arrayListOf()
                        for (i in (edit.beginA - 5)..(edit.endA+5)) {
                            modifiedLines[entry.newPath]!!.add(i)
                        }
                        for (i in (edit.beginB - 5)..(edit.endB+5)) {
                            modifiedLines[entry.newPath]!!.add(i)
                        }
                    }
                    DiffEntry.ChangeType.COPY -> {
                        for (i in (edit.beginA - 5)..(edit.endA+5)) {
                            modifiedLines[entry.newPath]!!.add(i)
                        }
                        for (i in (edit.beginB - 5)..(edit.endB+5)) {
                            modifiedLines[entry.newPath]!!.add(i)
                        }
                    }
                    else -> TODO()
                }
            }

            modifiedLines[entry.newPath] = modifiedLines[entry.newPath]!!.distinct().toCollection(ArrayList())
        }
        return Pair(modifiedFiles, modifiedLines)
    }

    fun getFile(commit: String, file: String): String {
        val tree = git!!.repository.resolve(commit)
        val objectId = git!!.repository.resolve(tree.name + ":" + file) ?: return ""
        val objectLoader = git!!.repository.open(objectId)
        return objectLoader.openStream().bufferedReader().use { it.readText() }
    }
}
