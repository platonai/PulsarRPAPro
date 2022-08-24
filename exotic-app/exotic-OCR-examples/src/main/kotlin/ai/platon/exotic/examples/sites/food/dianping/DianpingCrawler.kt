package ai.platon.exotic.examples.sites.food.dianping

import ai.platon.pulsar.common.message.MiscMessageWriter
import ai.platon.pulsar.context.support.AbstractPulsarContext
import ai.platon.pulsar.crawl.StreamingCrawler
import ai.platon.pulsar.crawl.common.url.ParsableHyperlink
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.dom.select.selectHyperlinks
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.session.PulsarSession
import ai.platon.scent.context.ScentContexts
import com.google.gson.GsonBuilder
import org.jsoup.nodes.Document
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

class DianpingCrawler(private val session: PulsarSession) {
    private val context = session.context as AbstractPulsarContext

    private val crawler = RestaurantCrawler(session)
    private val fieldSelectors = TaskDef.fieldSelectors.mapValues { (_, selector) -> "$selector .ocr" }
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val totalFieldCount = AtomicInteger()

    private val messageWriter = context.getBean(MiscMessageWriter::class)

    val crawlDelayPolicy = { fetchNum: Int ->
        if (fetchNum <= 2) {
            Duration.ofSeconds(10)
        } else {
            Duration.ofMinutes(1)
        }
    }

    val parseHandler = { _: WebPage, document: Document ->
        val fields = fieldSelectors.entries.associateWith { document.select(it.value).text() }
            .filter { it.value.isNotBlank() }
//        totalFieldCount.addAndGet(fields.size)
//        println("fields: ${fields.size} totalFieldCount: ${totalFieldCount.get()}")
    }

    init {
        context.crawlLoops.loops.map { it.crawler }.filterIsInstance<StreamingCrawler<*>>().forEach {
            it.delayPolicy = crawlDelayPolicy
        }
    }

    fun crawl(portalUrls: List<String>, args: String) {
        portalUrls.forEach { portalUrl -> crawlOutPages(portalUrl, args) }
    }

    fun crawlOutPages(portalUrl: String, args: String) {
        val options = crawler.options(args)
        options.ensureEventHandler().loadEventHandler.onAfterHtmlParse.addLast { _, document: FeaturedDocument ->
            collectPortalUrls(document) }

        val document = session.loadDocument(portalUrl, options)

        val links = document.document.selectHyperlinks(options.outLinkSelector)
            .asSequence()
            .take(100)
            .map { ParsableHyperlink("$it -refresh", parseHandler) }
            .onEach {
                it.referer = portalUrl
                it.eventHandler.combine(options.itemEventHandler!!)
            }
            .toList()
            .shuffled()
        context.submitAll(links).await()
    }

    private fun collectPortalUrls(document: FeaturedDocument) {
        document.select("a[data-cat-id]")
            .forEach { messageWriter.writeLine(it.attr("abs:href"), "portal.urls.txt") }
    }

}

/**
 * If running the program directly in the IDE may crash the system, use command line instead:
 *
java -Xmx10g -Xms2G -cp exotic-OCR-examples*.jar \
  -D"loader.main=ai.platon.exotic.examples.sites.food.dianping.DianpingCrawlerKt" \
  org.springframework.boot.loader.PropertiesLauncher
 * */
fun main() {
//    val args = "-i 1s -ii 5s -ol \"#shop-all-list .tit a[href~=shop]\" -ignoreFailure"
    val args = "-i 1s -ii 10s -ol \"#shop-all-list .tit a[href~=shop]\" -parse -ignoreFailure"

    System.setProperty("privacy.context.number", "3")
    System.setProperty("browser.max.active.tabs", "2")
//    BrowserSettings.headless()

    val context = ScentContexts.create()
    val session = context.createSession()

    DianpingCrawler(session).crawl(TaskDef.portalUrls, args)
}
