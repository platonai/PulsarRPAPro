package ai.platon.exotic.examples.sites.jd

import ai.platon.exotic.examples.sites.CommonRPA
import ai.platon.pulsar.common.HtmlIntegrity
import ai.platon.pulsar.common.ResourceLoader
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.crawl.common.url.ParsableHyperlink
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.dom.select.selectHyperlinks
import ai.platon.pulsar.persist.PageDatum
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.protocol.browser.emulator.BrowserResponseHandler
import ai.platon.pulsar.protocol.browser.emulator.util.HtmlIntegrityChecker
import ai.platon.pulsar.session.PulsarSession
import ai.platon.scent.context.ScentContexts
import org.jsoup.nodes.Document
import kotlin.streams.toList

class JdHtmlChecker: HtmlIntegrityChecker {
    override fun isRelevant(url: String): Boolean {
        return url.contains("jd.com")
    }

    // Since we need to check the html integrity of the page, we need active dom urls,
    // which is calculated in javascript.
    override fun invoke(pageSource: String, pageDatum: PageDatum): HtmlIntegrity {
        val url = pageDatum.activeDOMUrls?.location ?: pageDatum.url
        // Authorization verification
        return when {
            "login" in url -> HtmlIntegrity.ROBOT_CHECK_3
            "403 Forbidden" in pageSource -> HtmlIntegrity.FORBIDDEN
            else -> HtmlIntegrity.OK
        }
    }
}

class JdRPA(
    val session: PulsarSession = ScentContexts.createSession()
): CommonRPA() {

    private val logger = getLogger(this)

    val context = session.context
    private val htmlChecker get() = context.getBean(BrowserResponseHandler::class).htmlIntegrityChecker
    private val blockedUrls = listOf("*.jpg", "*.png", "*.gif", "*.avif")

    init {
        htmlChecker.addFirst(JdHtmlChecker())
    }

    fun options(args: String): LoadOptions {
        val options = session.options(args)
        initItemItemEventHandler(options)
        return options
    }

    private fun initItemItemEventHandler(options: LoadOptions) {
        val eh = options.itemEvent
        val be = eh.browseEvent
        // Warp up the browser to avoid being blocked by the server.
        be.onBrowserLaunched.addLast { page, driver ->
            driver.addBlockedURLs(blockedUrls)
            warnUpBrowser(page, driver)
        }
        be.onWillFetch.addLast { page, driver ->
            waitForReferrer(page, driver)
        }
        be.onWillCheckDocumentState.addLast { page, driver ->
            driver.waitForSelector("body .sku-name")
        }
    }
}

class JdCrawler(private val session: PulsarSession = ScentContexts.createSession()) {
    private val context = session.context

    private val rpa = JdRPA(session)

    private val parseHandler = { _: WebPage, document: FeaturedDocument -> }

    fun runDefault(args: String) {
        val portalUrls = ResourceLoader.readAllLines("portal.urls.jd.txt")
        val args1 = "-i 1s -requireSize 250000 -ol a[href~=/item] -ignoreFailure $args"
        crawl(portalUrls, args1)
    }

    fun crawl(portalUrls: List<String>, args: String) {
        portalUrls.forEach { portalUrl -> scrapeOutPages(portalUrl, args) }
        context.await()
    }

    fun scrapeOutPages(portalUrl: String, args: String) {
        val options = rpa.options(args)

        val document = session.loadDocument(portalUrl, options)

        val links = document.document.selectHyperlinks(options.outLinkSelector)
            .asSequence()
            .take(10000)
            .distinct()
            .map { ParsableHyperlink("$it -i 10s -requireSize 300000 -ignoreFailure", parseHandler) }
            .onEach {
                it.referrer = portalUrl
                it.event.chain(options.itemEvent)
            }
            .toList()
            .shuffled()

        context.submitAll(links)
    }

    fun extractPortalUrls() {
        val seedUrls = """
        https://list.jd.com/list.html?cat=670,677,11762
        https://list.jd.com/list.html?cat=670,677,688
        https://list.jd.com/list.html?cat=670,671,1105
        https://list.jd.com/list.html?cat=670,671,672
        https://list.jd.com/list.html?cat=1318,1463,1484
        https://list.jd.com/list.html?cat=1318,1463,1483
        https://list.jd.com/list.html?cat=1318,1463,14666
        https://list.jd.com/list.html?cat=1318,12115,12117
        https://list.jd.com/list.html?cat=1318,1466,1694
        https://list.jd.com/list.html?cat=1318,2628,12136
    """.trimIndent().split("\n")
        val args = "-i 1s -requireSize 250000 -ol a[href~=/item] -ignoreFailure"

        val session = ScentContexts.createSession()
        session.normalize(seedUrls)
            .parallelStream()
            .map { session.loadDocument(it) }
            .map { it.select("a[href~=/list]").map { it.attr("abs:href") } }
            .toList()
            .flatten()
            .filter { it.contains("?cat=") }
            .sorted()
            .distinct()
            .forEach { println(it) }
    }
}

/**
java -Xmx10g -Xms2G -cp exotic-OCR-examples*.jar \
-D"loader.main=ai.platon.exotic.examples.sites.jd.JdCrawlerKt" \
org.springframework.boot.loader.PropertiesLauncher
 * */
fun main(argv: Array<String>) {
    val session = ScentContexts.createSession()
    val portalUrls = ResourceLoader.readAllLines("portal.urls.jd.txt")
    val args = "-i 1s -requireSize 250000 -ol a[href~=/item] -ignoreFailure"
    JdCrawler(session).crawl(portalUrls, args)
}
