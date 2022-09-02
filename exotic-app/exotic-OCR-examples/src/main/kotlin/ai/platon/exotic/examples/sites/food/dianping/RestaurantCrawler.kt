package ai.platon.exotic.examples.sites.food.dianping

import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.message.MiscMessageWriter
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.context.support.AbstractPulsarContext
import ai.platon.pulsar.crawl.CoreMetrics
import ai.platon.pulsar.crawl.fetch.driver.NavigateEntry
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.persist.PageDatum
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.protocol.browser.emulator.BrowserResponseHandler
import ai.platon.pulsar.protocol.browser.emulator.HtmlIntegrityChecker
import ai.platon.pulsar.session.PulsarSession
import ai.platon.scent.context.ScentContexts
import ai.platon.scent.jackson.prettyScentObjectWritter
import com.fasterxml.jackson.core.JsonProcessingException
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import java.io.IOException
import java.nio.file.Files
import java.text.NumberFormat
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

class DianPingHtmlIntegrityChecker: HtmlIntegrityChecker {
    override fun isRelevant(url: String): Boolean {
        return true
    }

    // Since we need to check the html integrity of the page, we need active dom urls,
    // which is calculated in javascript.
    override fun invoke(pageSource: String, pageDatum: PageDatum): HtmlIntegrity {
        val url = pageDatum.activeDomUrls?.location ?: pageDatum.url
        // Authorization verification
        return when {
            "verify" in url -> HtmlIntegrity.ROBOT_CHECK_3
            "403 Forbidden" in pageSource -> HtmlIntegrity.FORBIDDEN
            else -> HtmlIntegrity.OK
        }
    }
}

class RestaurantCrawler(
    val session: PulsarSession = ScentContexts.createSession()
) {
    companion object {
        const val PREV_PAGE_WILL_READY = 0
        const val PREV_PAGE_READY = 1
        const val PREV_PAGE_NEVER_READY = 3
    }

    private val logger = getLogger(this)

    private val context = session.context as AbstractPulsarContext

    private val htmlIntegrityChecker get() = context.getBean<BrowserResponseHandler>().htmlIntegrityChecker
    private val messageWriter = context.getBean(MiscMessageWriter::class)
    private val coreMetrics = context.getBean<CoreMetrics>()

    private val ocrFieldSelectors = TaskDef.fieldSelectors.mapValues { (_, selector) -> "$selector .ocr" }
    private val totalFields = AtomicInteger()
    private val numberFormat = NumberFormat.getInstance().apply { maximumFractionDigits = 2 }

    private val isActive get() = AppContext.isActive

    init {
        htmlIntegrityChecker.checkers.add(0, DianPingHtmlIntegrityChecker())
    }

    fun options(args: String): LoadOptions {
        val options = session.options(args)

        registerEventHandlers(options)
        registerItemEventHandlers(options)

        return options
    }

    private fun registerEventHandlers(options: LoadOptions) {
        options.ensureEventHandler().loadEventHandler.onAfterHtmlParse.addLast { _, document: FeaturedDocument ->
            collectPortalUrls(document) }
    }

    private fun registerItemEventHandlers(options: LoadOptions) {
        val eh = options.ensureItemEventHandler()

        eh.loadEventHandler.onWillLoad.addLast {

        }

        eh.loadEventHandler.onWillFetch.addLast { page ->
            page.maxRetries = 6
            page.pageModel.clear()
        }

        eh.loadEventHandler.onLoaded.addLast { page ->
            dumpPageModel(page)
        }

        eh.simulateEventHandler.onWillFetch.addLast { page, driver ->
            waitForReferrer(page, driver)
            waitForPreviousPage(page, driver)
        }

        // Warp up the browser to avoid being blocked by the server.
        eh.loadEventHandler.onBrowserLaunched.addLast { page, driver ->
            warnUpBrowser(page, driver)
        }

        val seh = eh.simulateEventHandler
        seh.onWillCheckDOMState.addLast { page, driver ->
            // driver.waitForSelector("#reviewlist-wrapper li.comment-item")
        }

        seh.onWillComputeFeature.addLast { page, driver ->
            driver.bringToFront()
            TaskDef.commentSelectors.entries.mapIndexed { i, _ -> TaskDef.cs(i) + " .more" }.shuffled()
                .asFlow().flowOn(Dispatchers.IO).collect { selector ->
                    if (driver.exists(selector)) {
                        driver.click(selector)
                        delay(500L, 2_000)
                    }
                }
        }

        seh.onFeatureComputed.addLast { page, driver ->
            driver.bringToFront()
            TaskDef.fieldSelectors.entries.shuffled().asFlow().flowOn(Dispatchers.IO).collect { (name, selector) ->
                val point = driver.clickablePoint(selector)
                if (point != null) {
                    driver.moveMouseTo(point.x, point.y)
                    Screenshot(page, driver).doOCR(name, selector)
                    delay(1000, 3000)
                }
            }
        }

        seh.onWillStopTab.addLast { page, driver ->
            val currentUrl = driver.currentUrl()
            if (TaskDef.isShop(currentUrl)) {
                if (Random.nextInt(2) == 0) {
                    // humanize(page, driver)
                }
            }
        }

        eh.loadEventHandler.onHTMLDocumentParsed.addLast { page, document ->
            val fields = page.variables.variables
                .filterKeys { it.startsWith(Screenshot.OCR) }
                .mapValues { it.value.toString() }
            if (fields.isEmpty()) {
                return@addLast
            }

            page.pageModel.emplace(0, "OCR", fields)

            fields.forEach { (key, text) ->
                val selector = key.substringAfter(Screenshot.OCR)

                document.selectFirstOrNull(selector)
                    ?.appendElement("div")
                    ?.attr("style", "display: none")
                    ?.addClass("ocr")
                    ?.text(text)
            }
        }.addLast { page, document ->
            val fields = ocrFieldSelectors.entries.associate { it.key to document.select(it.value).text() }
                .filter { it.value.isNotBlank() }

            if (fields.isNotEmpty()) {
                totalFields.addAndGet(fields.size)
                val elapsedTime = coreMetrics.elapsedTime.truncatedTo(ChronoUnit.SECONDS)
                val speed = totalFields.get().toDouble() / elapsedTime.seconds
                val speedText = numberFormat.format(speed)
                logger.info("Extracted {}/{} fields in {}, {}/s", fields.size, totalFields, elapsedTime, speedText)
            }
        }
    }

    private suspend fun warnUpBrowser(page: WebPage, driver: WebDriver) {
//        visit(TaskDef.homePage, driver)
        page.referrer?.let { visit(it, driver) }

        val pattern = page.url.substringAfterLast("/")
        // driver.clickMatches("ul li a[onclick]", "href", pattern)
        // TODO: create a new driver with the opened tab
    }

    private suspend fun visit(url: String, driver: WebDriver) {
        val display = driver.browser.id.display
        logger.info("Visiting with browser #{} | {}", display, url)

        driver.navigateTo(url)
        driver.waitForSelector("body")
        var n = 2 + Random.nextInt(5)
        while (n-- > 0 && isActive) {
            val deltaY = 100.0 + 20 * Random.nextInt(10)
            driver.mouseWheelDown(deltaY = deltaY)
            delay(500, 500)
        }

        logger.debug("Visited | {}", url)
    }

    private suspend fun humanize(page: WebPage, driver: WebDriver) {
        val i = Random.nextInt(1, 20)
        val selector = listOf("#around-info", ".main").shuffled().first()
        val n = Random.nextInt(1, 5)
        repeat(n) {
//            driver.moveMouseTo(500.0 + 1.4372 * i * n, 300.0 + 1.2732 * i * n)
//            delay(500, 500)
        }

        val href = driver.clickNthAnchor(i, selector)
        if (page.id < 1000) {
            logger.info("Random click and navigate to $href")
        }

        if (href != null) {
            driver.waitForNavigation()
            driver.waitForSelector("body")
            delay(15_000, 10_000)
            driver.scrollToMiddle(0.25f)
        }
    }

    private fun collectPortalUrls(document: FeaturedDocument) {
        document.select("a[data-cat-id]")
            .forEach { messageWriter.write(it.attr("abs:href"), "portal.urls.txt") }
    }

    private fun dumpPageModel(page: WebPage) {
        val fields = page.variables.variables.filterKeys { it.startsWith(Screenshot.OCR) }
        if (fields.isEmpty()) {
            return
        }

        val pageModel = page.pageModel
        val fieldGroups = pageModel.fieldGroups.map { it.name to it.fields }
        if (fieldGroups.isEmpty()) {
            return
        }

        val path = Screenshot.generateScreenshotDir(page).resolve("0000pageModel.json")
        try {
            val json = prettyScentObjectWritter().writeValueAsString(fieldGroups)
            Files.deleteIfExists(path)
            Files.createDirectories(path.parent)
            Files.writeString(path, json)
        } catch (e: JsonProcessingException) {
            logger.warn(e.brief("dumpPageModel | ", " | " + page.url))
        } catch (e: IOException) {
            logger.warn(e.stringify("dumpPageModel | "))
        }
    }

    private suspend fun waitForReferrer(page: WebPage, driver: WebDriver) {
        val referrer = page.referrer ?: return
        val referrerVisited = driver.browser.navigateHistory.any { it.url == referrer }
        if (!referrerVisited) {
            logger.debug("Visiting the referrer | {}", referrer)
            visit(referrer, driver)
        }
    }

    private suspend fun waitForPreviousPage(page: WebPage, driver: WebDriver) {
        var tick = 0
        var checkState = checkPreviousPage(driver)
        while (tick++ <= 180 && checkState.code == PREV_PAGE_WILL_READY) {
            if (checkState.message.isBlank()) {
                // The browser has just started, don't crowd into.
                delay(1_000, 10_000)
                break
            }

            // The last page does not load completely, wait for it.
            val shouldReport = (tick > 150 && tick % 10 == 0)
            if (shouldReport) {
                val urlToWait = checkState.message
                logger.info("Waiting for page | {} | {} <- {}", tick, urlToWait, page.url)
            }

            delay(1000L)
            checkState = checkPreviousPage(driver)
        }
    }

    private fun checkPreviousPage(driver: WebDriver): CheckState {
        val navigateHistory = driver.browser.navigateHistory
        val now = Instant.now()

        val testNav = navigateHistory.lastOrNull { mayWaitFor(it, driver.navigateEntry) }

        val code = when {
            testNav == null -> PREV_PAGE_WILL_READY
            testNav.documentReadyTime > now -> PREV_PAGE_WILL_READY
            Duration.between(testNav.documentReadyTime, now).seconds > 10 -> PREV_PAGE_READY
            Duration.between(testNav.lastActiveTime, now).seconds > 60 -> PREV_PAGE_NEVER_READY
            !isActive -> PREV_PAGE_NEVER_READY
            !driver.isWorking -> PREV_PAGE_NEVER_READY
            else -> PREV_PAGE_WILL_READY
        }

        return CheckState(code, testNav?.url ?: "")
    }

    private fun mayWaitFor(currentEntry: NavigateEntry, testEntry: NavigateEntry): Boolean {
        val now = Instant.now()

        val may = testEntry.pageId > 0
                && !testEntry.stopped
                && TaskDef.isShop(testEntry.url)
                && testEntry.createTime < currentEntry.createTime
                && Duration.between(testEntry.lastActiveTime, now).seconds < 30

        return may
    }

    private suspend fun delay(timeMillis: Long, delta: Int) {
        delay(timeMillis + Random.nextInt(delta))
    }
}

/**
 * If running the program directly in the IDE may crash the system, use command line instead:
 *
java -Xmx10g -Xms2G -cp exotic-OCR-examples*.jar \
-D"loader.main=ai.platon.exotic.examples.sites.food.dianping.RestaurantCrawlerKt" \
org.springframework.boot.loader.PropertiesLauncher
 * */
fun main() {
    val url = "https://www.dianping.com/shop/G3ZxMJTDLITGsxLX"
    val args = "-i 1s -ignoreFailure -parse"

//    BrowserSettings.headless()

    val crawler = RestaurantCrawler()

    val fieldSelectors = TaskDef.fieldSelectors.mapValues { (_, selector) -> "$selector .ocr" }
    val options = crawler.options(args)
    options.referrer = "https://www.dianping.com/beijing/ch10/r2596"
    val fields = crawler.session.scrape(url, options, fieldSelectors)
    println(GsonBuilder().setPrettyPrinting().create().toJson(fields))
}
