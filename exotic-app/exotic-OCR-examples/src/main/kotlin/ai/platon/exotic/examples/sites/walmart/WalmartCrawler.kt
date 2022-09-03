package ai.platon.exotic.examples.sites.walmart

import ai.platon.exotic.examples.sites.CommonRPA
import ai.platon.pulsar.common.HtmlIntegrity
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.common.urls.UrlAware
import ai.platon.pulsar.context.support.AbstractPulsarContext
import ai.platon.pulsar.crawl.common.url.ParsableHyperlink
import ai.platon.pulsar.dom.select.selectHyperlinks
import ai.platon.pulsar.persist.PageDatum
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.protocol.browser.emulator.HtmlIntegrityChecker
import ai.platon.pulsar.session.PulsarSession
import ai.platon.scent.context.ScentContexts
import org.jsoup.nodes.Document
import java.time.Duration

class WalmartHtmlChecker: HtmlIntegrityChecker {
    override fun isRelevant(url: String): Boolean {
        return true
    }

    // Since we need to check the html integrity of the page, we need active dom urls,
    // which is calculated in javascript.
    override fun invoke(pageSource: String, pageDatum: PageDatum): HtmlIntegrity {
        val url = pageDatum.activeDomUrls?.location ?: pageDatum.url
        // Authorization verification
        return when {
            "blocked" in url -> HtmlIntegrity.ROBOT_CHECK
            "403 Forbidden" in pageSource -> HtmlIntegrity.FORBIDDEN
            else -> HtmlIntegrity.OK
        }
    }
}

class WalmartRPA(
    val session: PulsarSession = ScentContexts.createSession()
): CommonRPA() {

    private val logger = getLogger(this)

    fun options(args: String): LoadOptions {
        val options = session.options(args)
        val eh = options.ensureItemEventHandler()
        val leh = eh.loadEventHandler
        val seh = eh.simulateEventHandler
        // Warp up the browser to avoid being blocked by the server.
        leh.onBrowserLaunched.addLast { page, driver ->
            warnUpBrowser(page, driver)
        }
        seh.onWillFetch.addLast { page, driver ->
            waitForReferrer(page, driver)
            waitForPreviousPage(page, driver)
        }
        seh.onWillCheckDOMState.addLast { page, driver ->
            driver.waitForSelector("body h1[itemprop=name]")
        }
        return options
    }
}

class WalmartCrawler(private val session: PulsarSession) {
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

    private val parseHandler = { _: WebPage, document: Document -> }

    init {
        context.crawlLoops.loops.forEach {
            it.crawler.retryDelayPolicy = retryDelayPolicy
        }
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
            .map { ParsableHyperlink("$it -i 10s -ignoreFailure", parseHandler) }
            .onEach {
                it.referer = portalUrl
                it.eventHandler.combine(options.itemEventHandler!!)
            }
            .toList()
            .shuffled()

//        links.forEach {
//            println(it)
//        }

        context.submitAll(links)
    }
}

fun main() {
    System.setProperty(CapabilityTypes.PRIVACY_CONTEXT_NUMBER, "3")
    System.setProperty(CapabilityTypes.BROWSER_MAX_ACTIVE_TABS, "3")

//    BrowserSettings.headless()
//    BrowserSettings.supervised()

    val portalUrls = """
https://www.walmart.com/browse/cell-phones/apple-iphone/1105910_7551331_1127173?povid=web_globalnav_cellphones_iphone
    """.trimIndent().split("\n")
    val args = "-i 1s -requireSize 250000 -ol a[href~=/ip/] -ignoreFailure"

    val session = ScentContexts.createSession()
    WalmartCrawler(session).crawl(portalUrls, args)
}
