package ai.platon.scent.examples.common

import ai.platon.pulsar.common.sql.ResultSetFormatter
import ai.platon.pulsar.ql.ResultSets
import ai.platon.pulsar.ql.h2.H2MemoryDb
import ai.platon.pulsar.ql.h2.utils.ResultSetUtils
import ai.platon.scent.ScentContext
import ai.platon.scent.ql.h2.context.ScentSQLContext
import ai.platon.scent.ql.h2.context.support.AbstractScentSQLContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.sql.Connection
import java.sql.ResultSet
import java.util.concurrent.ArrayBlockingQueue

/**
 * The base class for all tests.
 */
open class VerboseSqlExtractor(context: ScentSQLContext): VerboseCrawler(context) {

    private val sqlContext get() = context as AbstractScentSQLContext
    private val connectionPool get() = sqlContext.connectionPool
    private val randomConnection get() = sqlContext.randomConnection
    private val stat = randomConnection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE)

    fun allocateDbConnections(concurrent: Int) {
        runBlocking {
            repeat(concurrent) { launch { connectionPool.add(randomConnection) } }
        }
    }

    fun execute(sql: String, printResult: Boolean = true, formatAsList: Boolean = false) {
        try {
            val regex = "^(SELECT|CALL).+".toRegex()
            if (sql.uppercase().filter { it != '\n' }.trimIndent().matches(regex)) {
                val rs = stat.executeQuery(sql)
                if (printResult) {
                    println(ResultSetFormatter(rs, asList = formatAsList))
                }
            } else {
                val r = stat.execute(sql)
                if (printResult) {
                    println(r)
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun query(sql: String, printResult: Boolean = true, withHeader: Boolean = true): ResultSet {
        try {
            val rs = stat.executeQuery(sql)

            rs.beforeFirst()
            val count = ResultSetUtils.count(rs)

            if (printResult) {
                rs.beforeFirst()
                println(ResultSetFormatter(rs, withHeader = withHeader, asList = (count == 1)))
            }

            return rs
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        return ResultSets.newSimpleResultSet()
    }

    override fun close() {
        connectionPool.forEach { it.runCatching { it.close() }.onFailure { log.warn(it.message) } }
        super.close()
    }
}
