package com.thecout.cpg.GraphManagement.DB

import org.neo4j.driver.*
import org.neo4j.driver.exceptions.ClientException
import org.neo4j.driver.exceptions.ServiceUnavailableException
import org.slf4j.LoggerFactory
import java.lang.Thread.sleep
import java.time.Duration
import java.util.concurrent.TimeUnit

class BoltDB(val uri: String, val user: String, val password: String) : DB {
    var driver: Driver? = null
    var session: ThreadLocal<Session?> = ThreadLocal()
    private val log = LoggerFactory.getLogger(BoltDB::class.java)
    var available = false
    var reconnecting = false

    init {
        connect()
    }

    @Throws(Exception::class)
    override fun close() {
        driver!!.close()
    }

    override fun reconnect() {
        close()
        connect()
    }

    override fun connect() {
        val config = Config.builder()
            .withConnectionTimeout(10, TimeUnit.DAYS)
            .withMaxConnectionLifetime(10, TimeUnit.DAYS)
            .withRoutingFailureLimit(100000)
            .withConnectionAcquisitionTimeout(10, TimeUnit.DAYS)
            .build()
        driver = GraphDatabase.driver(uri, AuthTokens.basic(user, password), config)
        available = true
    }
 override fun run(query: String, timeOut: Long): List<Record> {
        return try {
            if (session.get() == null || !session.get()!!.isOpen) {
		session.set(driver!!.session())
            }
            if (available) {
                val res =
                    session.get()!!.run(query, TransactionConfig.builder().withTimeout(Duration.ofSeconds(timeOut)).build())
		reconnecting = false
                res.list()
            } else {
               return listOf()
	    }

        } catch (e: ServiceUnavailableException) {
       	     sleep(180000) 
	     synchronized(reconnecting) {
                if (!reconnecting && available) {
	    	    println("${Thread.currentThread().id} is renewing DB connection")
	            available = false
                    reconnecting = true
                    reconnect()
                }
                return listOf()
            } 
            listOf()



	} catch (e: ClientException) {
            e.printStackTrace()
	    listOf()
        } catch (e: IllegalStateException) {
                session.set(driver!!.session())
		listOf()
        }
    }

}

