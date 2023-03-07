package com.thecout.cpg.Util

class Timing {
    fun start() {
        startTime = System.currentTimeMillis()
    }

    fun stop() {
        endTime = System.currentTimeMillis()
    }

    fun getTime(): Long {
        return endTime - startTime
    }

    companion object {
        private var startTime: Long = 0
        private var endTime: Long = 0
    }
}