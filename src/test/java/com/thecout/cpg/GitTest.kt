package com.thecout.cpg

import com.thecout.cpg.RepoManagement.Repo
import org.junit.jupiter.api.Test

class GitTest {
    @Test
    fun gitTest() {
        val repo = Repo("https://github.com/FFmpeg/FFmpeg", "/tmp/libxml2")
        for (file in repo.compare(
            "4f5352d5fe6ce801cfd0a45e8564373eeec595da",
            "130250d3affb26222b76b73185308337dcbb431c"
        ).second) {
            println(file)
        }
    }

    @Test
    fun getFileTest() {
        val repo = Repo("https://github.com/GNOME/libxml2", "/tmp/libxml2")
        println(repo.getFile("22f1521122402bee88b58a463af58b5ab865dc3f", "CMakeLists.txt"))
    }

}