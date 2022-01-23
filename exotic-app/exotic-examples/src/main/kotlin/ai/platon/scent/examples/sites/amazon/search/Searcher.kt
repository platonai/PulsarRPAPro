package ai.platon.scent.examples.sites.amazon.search

import ai.platon.pulsar.crawl.CrawlLoops
import ai.platon.pulsar.crawl.DefaultLoadEventHandler
import ai.platon.pulsar.crawl.common.GlobalCache
import ai.platon.pulsar.crawl.common.GlobalCacheFactory
import ai.platon.pulsar.crawl.common.url.StatefulListenableHyperlink
import ai.platon.scent.ScentSession
import ai.platon.scent.context.withContext
import ai.platon.scent.crawl.diffusing.DefaultDiffusingCrawler
import ai.platon.scent.crawl.diffusing.config.DiffusingCrawlerConfig

class Searcher(session: ScentSession, val crawlLoops: CrawlLoops) {
    val label = "sleep"
    val home = "https://www.amazon.com"
    val portalUrl = home

    val excludedCategories = "Kindle Store, Digital Music, Prime Video, Movies & TV, Books，CDs & Vinyl"
    val excludedSearchAlias = mutableListOf("aps")
    val keywords = mutableListOf("sleep", "nap", "insomnia", "stress relief")

    val globalCacheFactory = GlobalCacheFactory(session.unmodifiedConfig)
    val config = DiffusingCrawlerConfig(label, portalUrl, excludedCategories, excludedSearchAlias, keywords)
    val searcher = DefaultDiffusingCrawler(config, session, globalCacheFactory)

    fun crawl() {
        searcher.generate().mapIndexed { i, url ->
            println("$i. $url")
            StatefulListenableHyperlink(url.url).also {
                it.args = "-i 3m"
                val eventHandler = it.loadEventHandler as DefaultLoadEventHandler
                eventHandler.onAfterHtmlParsePipeline.addLast { page, document ->
                    searcher.onAfterHtmlParse(page, document)
                }
            }
        }.toCollection(searcher.defaultFetchCache.nonReentrantQueue)
    }
}

fun main() {
    withContext { context ->
        val session = context.createSession()
        val globalCache = GlobalCache(session.unmodifiedConfig)
        val streamingLoopArgs = "-i 10d -parse -ignoreFailure"
//        val crawlLoop = StreamingCrawlLoop(globalCache, session.unmodifiedConfig)
//            .apply { defaultOptions = session.options(streamingLoopArgs) }
        Searcher(session, session.context.crawlLoops).crawl()
        readLine()
    }
}
