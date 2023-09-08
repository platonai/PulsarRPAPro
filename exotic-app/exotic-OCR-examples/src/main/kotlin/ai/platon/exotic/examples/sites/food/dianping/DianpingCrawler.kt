package ai.platon.exotic.examples.sites.food.dianping

import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.common.ResourceLoader
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.common.urls.UrlAware
import ai.platon.pulsar.common.urls.UrlUtils
import ai.platon.pulsar.context.support.AbstractPulsarContext
import ai.platon.pulsar.crawl.common.url.ParsableHyperlink
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.dom.select.selectHyperlinks
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.session.PulsarSession
import ai.platon.scent.context.ScentContexts
import java.time.Duration

class DianpingCrawler(private val session: PulsarSession = ScentContexts.createSession()) {
    private val context = session.context as AbstractPulsarContext

    private val rpa = RestaurantRPA(session)

    private val retryDelayPolicy = { nextRetryNumber: Int, _: UrlAware? ->
        if (nextRetryNumber <= 2) {
            Duration.ofSeconds(10)
        } else {
            val minutes = nextRetryNumber.coerceAtMost(3).toLong()
            Duration.ofMinutes(minutes)
        }
    }

    private val parseHandler = { _: WebPage, document: FeaturedDocument -> }

    init {
        context.crawlLoops.loops.forEach {
            it.crawler.retryDelayPolicy = retryDelayPolicy
        }
    }

    fun runDefault(args: String) {
        val args1 = "-i 1s -ol \"#shop-all-list .tit a[href~=shop]\" -parse -ignoreFailure $args"
        val portalUrls = ResourceLoader.readAllLines("portal.urls.dianping.txt")
            .filter { UrlUtils.isStandard(it) }
            .shuffled()
        crawl(portalUrls, args1)
    }

    fun crawl(portalUrls: List<String>, args: String) {
        portalUrls.forEach { portalUrl -> scrapeOutPages(portalUrl, args) }
        context.await()
    }

    fun scrapeOutPages(portalUrl: String, args: String) {
        val options = rpa.options(args)
        val itemOptions = options.createItemOptions()

        val document = session.loadDocument(portalUrl, options)

        val links = document.document.selectHyperlinks(options.outLinkSelector)
            .asSequence()
            .take(10000)
            .distinct()
            .map { ParsableHyperlink("$it $itemOptions -requireSize 300000 -ignoreFailure", parseHandler) }
            .onEach {
                it.referrer = portalUrl
                it.event.chain(options.itemEvent)
            }
            .toList()
            .shuffled()

        context.submitAll(links)
    }
}

fun main(argv: Array<String>) {
    BrowserSettings.privacy(2).maxTabs(8).headless()

    val context = ScentContexts.create()
    val session = context.createSession()

    val args0 = LoadOptions.normalize(argv.joinToString(" "))
    val args = "-i 1s -ol \"#shop-all-list .tit a[href~=shop]\" -parse -ignoreFailure $args0"
    val portalUrls = ResourceLoader.readAllLines("portal.urls.dianping.txt")
        .filter { UrlUtils.isStandard(it) }
        .shuffled()
    DianpingCrawler(session).crawl(portalUrls, args)
}
