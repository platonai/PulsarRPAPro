package ai.platon.exotic.examples.sites.food.dianping

import ai.platon.pulsar.crawl.StreamingCrawler
import ai.platon.pulsar.crawl.common.url.ParsableHyperlink
import ai.platon.pulsar.dom.select.selectHyperlinks
import ai.platon.pulsar.persist.WebPage
import ai.platon.scent.context.ScentContexts
import com.google.gson.GsonBuilder
import org.jsoup.nodes.Document
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

/**
 * Running the program directly in the IDE may crash the system, use command line instead:
 *
java -Xmx10g -Xms2G -cp exotic-OCR-examples*.jar \
  -D"loader.main=ai.platon.exotic.examples.sites.food.dianping.DianpingCrawlerKt" \
  org.springframework.boot.loader.PropertiesLauncher
 * */
fun main() {
//    val args = "-i 1s -ii 5s -ol \"#shop-all-list .tit a[href~=shop]\" -ignoreFailure"
    val args = "-i 1s -ii 10s -ol \"#shop-all-list .tit a[href~=shop]\" -parse -ignoreFailure"

    System.setProperty("privacy.context.number", "3")
    System.setProperty("browser.max.active.tabs", "3")
//    BrowserSettings.headless()

    val context = ScentContexts.create()
    val session = context.createSession()

    context.crawlLoops.loops.map { it.crawler }.filterIsInstance<StreamingCrawler<*>>().forEach {
        it.delayPolicy = { fetchNum ->
            if (fetchNum <= 2) {
                Duration.ofSeconds(10)
            } else {
                Duration.ofMinutes(1)
            }
        }
    }

    val crawler = RestaurantCrawler(session)
    val fieldSelectors = crawler.fieldSelectors.mapValues { (_, selector) -> "$selector .ocr" }
    val gson = GsonBuilder().setPrettyPrinting().create()
    val totalFieldCount = AtomicInteger()

    val parseHandler = { _: WebPage, document: Document ->
        // use the document
        // ...
        // and then extract further hyperlinks
        val fields = fieldSelectors.entries.associateWith { document.select(it.value).text() }
            .filter { it.value.isNotBlank() }
        // println("fields: ${gson.toJson(fields)}")
        totalFieldCount.addAndGet(fields.size)
        println("fields: ${fields.size} totalFieldCount: ${totalFieldCount.get()}")
    }

    val document = session.loadDocument(crawler.portalUrl, args)

    val options = crawler.options(args)
    val links = document.document.selectHyperlinks(options.outLinkSelector)
        .asSequence()
        .take(100)
        .map { ParsableHyperlink("$it -refresh", parseHandler) }
        .onEach { it.eventHandler.combine(options.itemEventHandler!!) }
        .toList()
    context.submitAll(links).await()
}
