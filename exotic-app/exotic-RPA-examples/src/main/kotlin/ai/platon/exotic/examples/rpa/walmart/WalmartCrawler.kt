package ai.platon.exotic.examples.sites.walmart

import ai.platon.exotic.examples.sites.CommonRPA
import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.common.HtmlIntegrity
import ai.platon.pulsar.common.ResourceLoader
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.message.MiscMessageWriter
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.common.urls.UrlAware
import ai.platon.pulsar.common.urls.UrlUtils
import ai.platon.pulsar.context.support.AbstractPulsarContext
import ai.platon.pulsar.crawl.common.url.ParsableHyperlink
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.dom.select.selectHyperlinks
import ai.platon.pulsar.persist.PageDatum
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.protocol.browser.driver.cdt.ChromeDevtoolsDriver
import ai.platon.pulsar.protocol.browser.emulator.BrowserResponseHandler
import ai.platon.pulsar.protocol.browser.emulator.util.HtmlIntegrityChecker
import ai.platon.pulsar.session.PulsarSession
import ai.platon.scent.context.ScentContexts
import org.jsoup.nodes.Document
import java.time.Duration

class WalmartHtmlChecker: HtmlIntegrityChecker {
    override fun isRelevant(url: String): Boolean {
        return true
    }

    // Urls in ActiveDOMUrls are calculated in javascript, which are the actual urls of the page
    override fun invoke(pageSource: String, pageDatum: PageDatum): HtmlIntegrity {
        val url = pageDatum.activeDOMUrls?.location ?: return HtmlIntegrity.OK
        // Authorization verification
        return when {
            "blocked" in url -> HtmlIntegrity.ROBOT_CHECK_3.also { println("blocked") }
            "403 Forbidden" in pageSource -> HtmlIntegrity.FORBIDDEN
            else -> HtmlIntegrity.OK
        }
    }
}

class WalmartRPA(
    val session: PulsarSession = ScentContexts.createSession()
): CommonRPA() {

    private val logger = getLogger(this)

    private val context get() = session.context
    private val messageWriter = context.getBean(MiscMessageWriter::class)
    private val responseHandler get() = context.getBean(BrowserResponseHandler::class)

    init {
        responseHandler.htmlIntegrityChecker.addFirst(WalmartHtmlChecker())
    }

    fun options(args: String): LoadOptions {
        val options = session.options(args)

        val le = options.event.loadEvent
        le.onHTMLDocumentParsed.addLast { _, document: FeaturedDocument ->
            collectPortalUrls(document)
        }

        val be = options.itemEvent.browseEvent
        be.onBrowserLaunched.addLast { page, driver ->
            // Warp up the browser to avoid being blocked by the website
            if (driver is ChromeDevtoolsDriver) {
                val devTools = driver.implementation
//                devTools.network.clearBrowserCache()
                devTools.network.clearBrowserCookies()
            }
            page.fetchRetries = 3
            warnUpBrowser(page, driver)
        }
        be.onWillFetch.addLast { page, driver ->
            waitForReferrer(page, driver)
            waitForPreviousPage(page, driver)
        }
        be.onWillCheckDocumentState.addLast { page, driver ->
            // driver.waitForSelector("body h1[itemprop=name]")
        }

        return options
    }

    override suspend fun warnUpBrowser(page: WebPage, driver: WebDriver) {
        visit("https://www.walmart.com/", driver)
        super.warnUpBrowser(page, driver)
    }

    private fun collectPortalUrls(document: FeaturedDocument) {
        listOf("a[href~=/brands/]", "a[href~=/browse/]").forEach {
            document.select(it).forEach {
                messageWriter.write(it.attr("abs:href"), "walmart.portal.urls.txt")
            }
        }
    }
}

class WalmartCrawler(private val session: PulsarSession = ScentContexts.createSession()) {
    private val context = session.context as AbstractPulsarContext

    private val rpa = WalmartRPA(session)

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
        val portalUrls = ResourceLoader.readAllLines("portal.urls.walmart.txt")
            .filter { UrlUtils.isStandard(it) }
            .shuffled()
        val args1 = "-i 1s -requireSize 250000 -ol a[href~=/ip/] -ignoreFailure $args"
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
            .map { ParsableHyperlink("$it $itemOptions -i 1d -requireSize 300000 -ignoreFailure", parseHandler) }
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
    BrowserSettings.privacy(2).maxTabs(8)
        // .headless() // headless mode can be detected by walmart

    val portalUrls = ResourceLoader.readAllLines("portal.urls.walmart.txt")
        .filter { UrlUtils.isStandard(it) }
        .shuffled()
    val args0 = LoadOptions.normalize(argv.joinToString(" "))
    val args = "-i 1s -requireSize 250000 -ignoreFailure $args0"
    val session = ScentContexts.createSession()
    WalmartCrawler(session).crawl(portalUrls, args)
}
