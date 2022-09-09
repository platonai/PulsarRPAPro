package ai.platon.exotic.examples.sites.jd

import ai.platon.exotic.examples.sites.CommonRPA
import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.common.HtmlIntegrity
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.crawl.common.url.ParsableHyperlink
import ai.platon.pulsar.dom.select.selectHyperlinks
import ai.platon.pulsar.persist.PageDatum
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.protocol.browser.emulator.BrowserResponseHandler
import ai.platon.pulsar.protocol.browser.emulator.HtmlIntegrityChecker
import ai.platon.pulsar.session.PulsarSession
import ai.platon.scent.context.ScentContexts
import org.jsoup.nodes.Document

class JdHtmlChecker: HtmlIntegrityChecker {
    override fun isRelevant(url: String): Boolean {
        return url.contains("jd.com")
    }

    // Since we need to check the html integrity of the page, we need active dom urls,
    // which is calculated in javascript.
    override fun invoke(pageSource: String, pageDatum: PageDatum): HtmlIntegrity {
        val url = pageDatum.activeDomUrls?.location ?: pageDatum.url
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
        val eh = options.ensureItemEventHandler()
        val leh = eh.loadEventHandler
        val seh = eh.simulateEventHandler
        // Warp up the browser to avoid being blocked by the server.
        leh.onBrowserLaunched.addLast { page, driver ->
            driver.addBlockedURLs(blockedUrls)
            warnUpBrowser(page, driver)
        }
        seh.onWillFetch.addLast { page, driver ->
            waitForReferrer(page, driver)
        }
        seh.onWillCheckDOMState.addLast { page, driver ->
            driver.waitForSelector("body .sku-name")
        }
    }
}

class JdCrawler(private val session: PulsarSession) {
    private val context = session.context

    private val rpa = JdRPA(session)

    private val parseHandler = { _: WebPage, document: Document -> }

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
                it.referer = portalUrl
                it.eventHandler.combine(options.itemEventHandler!!)
            }
            .toList()
            .shuffled()

        context.submitAll(links)
    }
}

/**
java -Xmx10g -Xms2G -cp exotic-OCR-examples*.jar \
-D"loader.main=ai.platon.exotic.examples.sites.jd.JdCrawlerKt" \
org.springframework.boot.loader.PropertiesLauncher
 * */
fun main() {
    System.setProperty(CapabilityTypes.PRIVACY_CONTEXT_NUMBER, "8")
    System.setProperty(CapabilityTypes.BROWSER_MAX_ACTIVE_TABS, "8")

//    BrowserSettings.headless()
//    BrowserSettings.supervised()

    // Some websites will detect the user agent, if it's override, the visit is marked as suspicious
    // TODO: This is a fix to disable user agents, will correct in further versions
    BrowserSettings.userAgents.add("")

    val portalUrls = """
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
    JdCrawler(session).crawl(portalUrls, args)

    println("All done.")
}
