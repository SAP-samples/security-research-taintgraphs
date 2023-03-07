package com.thecout.cpg.GraphManagement.DB

import org.neo4j.driver.Record

interface DB {
    fun close()
    fun reconnect()
    fun connect()
    fun run(query: String, timeOut: Long = 100): List<Record>

}
