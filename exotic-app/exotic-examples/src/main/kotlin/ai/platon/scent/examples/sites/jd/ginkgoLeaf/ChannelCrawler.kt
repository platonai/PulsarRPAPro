package ai.platon.scent.examples.sites.jd.ginkgoLeaf

import ai.platon.pulsar.common.sql.ResultSetFormatter
import ai.platon.pulsar.common.sql.SQLTemplate
import ai.platon.pulsar.common.urls.PlainUrl
import ai.platon.pulsar.crawl.StreamingCrawler
import ai.platon.pulsar.ql.h2.H2MemoryDb
import ai.platon.scent.ScentSession
import ai.platon.scent.ql.h2.context.withSQLContext
import kotlinx.coroutines.runBlocking

class ChannelCrawler(
        val seed: String,
        val loadArguments: String,
        val sqlTemplate: SQLTemplate,
        val session: ScentSession
) {
    private val conn = H2MemoryDb().getRandomConnection()
    private val statement = conn.createStatement()
    private val loadOptions = session.options(loadArguments)

    fun scrape() {
        val itemUrls = mutableSetOf<PlainUrl>()
        IntRange(1, 2).forEach { p ->
            val url = "$seed&page=$p"
            session.loadDocument(url).select(loadOptions.outLinkSelector).mapTo(itemUrls) {
                PlainUrl(it.attr("abs:href"))
            }
        }

        runBlocking {
            StreamingCrawler(itemUrls.asSequence(), session.options(), session).use { it.run() }
        }

        itemUrls.forEachIndexed { i, url ->
            val sql = sqlTemplate.createInstance(url.url)
            execute(sql.sql)
        }
    }

    private fun execute(sql: String) {
        val regex = "^(SELECT|CALL).+".toRegex()
        if (sql.toUpperCase().filter { it != '\n' }.trimIndent().matches(regex)) {
            val rs = statement.executeQuery(sql)
            print(ResultSetFormatter(rs, withHeader = false))
        } else {
            val r = statement.execute(sql)
            println(r)
        }
    }
}

fun main() {
    val portalUrl = "https://search.jd.com/Search?keyword=银杏叶&enc=utf-8&wq=银杏叶"
    val args = "-i 1d -scrollCount 15 -topLinks 10000 -outLink \"#J_goodsList li[data-sku] div.p-name a[href~=/item]\""
    val sqlTemplate = """
        select
            dom_first_text(dom, '.sku-name') as Name,
            dom_first_text(dom, '.p-price') as Price,
            dom_first_text(dom, '#comment-count .count') as Reviews,
            array_join_to_string(dom_all_texts(dom, '#crumb-wrap .crumb a'), ' > ') as Category,
            dom_first_text(dom, '#summary-quan') as Coupon,
            dom_first_text(dom, '#summary-promotion') as Promotion,
            dom_first_text(dom, '#summary-service') as Delivery,
            dom_first_text(dom, '#parameter-brand') as Brand,
            dom_first_text(dom, '.parameter2') as Parameters,
            dom_first_attr(dom, '.p-img img', 'abs:src') as Img,
            dom_base_uri(dom) as Url
        from
            load_and_select(@url, ':root body');
    """.trimIndent()

    withSQLContext { cx ->
        val session = cx.createSession()
        val crawler = ChannelCrawler(portalUrl, args, SQLTemplate(sqlTemplate), session)
        crawler.scrape()
    }
}
