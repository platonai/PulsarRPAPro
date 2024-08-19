package ai.platon.exotic.crawl.common

import ai.platon.pulsar.common.brief
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.sql.SQLTemplate
import ai.platon.scent.ql.h2.context.ScentSQLContext
import ai.platon.scent.ql.h2.context.ScentSQLContexts
import java.nio.file.Path

open class ProductExtractor(
    val exportDirectory: Path,
    val context: ScentSQLContext = ScentSQLContexts.create(),
) {
    private val logger = getLogger(this)
    
    val isActive get() = context.isActive
    
    val executor = VerboseSQLExecutor(context)
    
    fun extract(itemsSQL: String, reviewsSQLTemplate: String? = null) {
        runCatching { extract0(itemsSQL, reviewsSQLTemplate) }.onFailure { logger.info(it.brief()) }
    }
    
    private fun extract0(itemsSQL: String, reviewsSQLTemplate: String? = null) {
        val rs = executor.executeQuery(itemsSQL)
        
        var path = CSV().export(rs, exportDirectory.resolve("item.csv"))
        println("Items are written to file://$path")
        
        if (reviewsSQLTemplate == null) {
            return
        }
        
        val sqls = mutableListOf<String>()
        while (rs.next()) {
            val url = rs.getString("baseUri")
            val sql = SQLTemplate(reviewsSQLTemplate).createInstance(url).sql
            sqls.add(sql)
        }
        
        val resultSets = executor.queryAll(sqls)
        
        path = CSV().export(resultSets.values, exportDirectory.resolve("reviews.csv"))
        println("Reviews are written to file://$path")
    }
}
