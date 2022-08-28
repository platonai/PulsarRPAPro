package ai.platon.exotic.examples.sites.food.dianping

import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.message.MiscMessageWriter
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.context.support.AbstractPulsarContext
import ai.platon.pulsar.crawl.CoreMetrics
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
import java.time.Instant.MAX
import java.time.temporal.ChronoUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random
import java.awt.Robot
import java.awt.event.InputEvent


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
            "verify" in url -> HtmlIntegrity.FORBIDDEN
            "403 Forbidden" in pageSource -> HtmlIntegrity.FORBIDDEN
            else -> HtmlIntegrity.OK
        }
    }
}

class RestaurantCrawler(
    val session: PulsarSession = ScentContexts.createSession()
) {
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

        eh.loadEventHandler.onBeforeLoad.addLast {
        }

        eh.loadEventHandler.onBeforeFetch.addLast { page ->
            page.pageModel.clear()
        }

        eh.loadEventHandler.onAfterLoad.addLast { page ->
            dumpPageModel(page)
        }

        eh.simulateEventHandler.onBeforeFetch.addLast { page, driver ->
            waitForReferrer(page, driver)
            waitForPreviousPage(page, driver)
        }

        // Warp up the browser to avoid being blocked by the server.
        eh.loadEventHandler.onAfterBrowserLaunch.addLast { page, driver ->
            warnUpBrowser(page, driver)
        }

        val seh = eh.simulateEventHandler
        seh.onAfterCheckDOMState.addLast { page, driver ->
            // driver.waitForSelector("#reviewlist-wrapper li.comment-item")
        }

        seh.onBeforeComputeFeature.addLast { page, driver ->
            TaskDef.commentSelectors.entries.mapIndexed { i, _ -> TaskDef.cs(i) + " .more" }
                .asFlow().flowOn(Dispatchers.IO).collect { selector ->
                    if (driver.exists(selector)) {
                        driver.click(selector)
                        delay(500)
                    }
                }
        }

        seh.onAfterComputeFeature.addLast { page, driver ->
            TaskDef.fieldSelectors.entries.asFlow().flowOn(Dispatchers.IO).collect { (name, selector) ->
                val point = driver.clickablePoint(selector)
                if (point != null) {
//                    robot?.mouseMove(point.x.toInt(), point.y.toInt())
                    driver.moveMouseTo(point.x, point.y)

                    Screenshot(page, driver).runCatching { doOCR(name, selector) }
                        .onFailure { logger.warn("Unexpected exception", it) }.getOrNull()

                    delay(500)
                }
            }
        }

        eh.loadEventHandler.onAfterHtmlParse.addLast { page, document ->
            val fields = page.variables.variables
                .filterKeys { it.startsWith(Screenshot.OCR) }
                .mapValues { it.toString() }
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
        page.referrer?.let { visit(it, driver) }
    }

    private suspend fun visit(url: String, driver: WebDriver) {
        val display = driver.browserInstance.id.display
        logger.info("Visiting with browser #{} | {}", display, url)

        try {
            driver.navigateTo(url)
            driver.waitForSelector("body")
            var n = 5 + Random.nextInt(5)
            while (isActive && n-- > 0) {
                driver.scrollDown()
                val delayMillis = 500L + Random.nextInt(500)
                delay(delayMillis)
            }

            logger.debug("Visited | {}", url)
        } catch (e: Exception) {
            logger.warn("Can not visit $url", e)
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
            logger.warn(e.simplify("dumpPageModel | ", " | " + page.url))
        } catch (e: IOException) {
            logger.warn(e.stringify("dumpPageModel | "))
        }
    }

    private suspend fun waitForReferrer(page: WebPage, driver: WebDriver) {
        val referrer = page.referrer ?: return
        val referrerVisited = driver.browserInstance.navigateHistory.any { it.url == referrer }
        if (!referrerVisited) {
            logger.debug("Visiting the referrer | {}", referrer)
            visit(referrer, driver)
        }
    }

    private suspend fun waitForPreviousPage(page: WebPage, driver: WebDriver) {
        var tick = 60
        var checkState = checkPreviousPage(driver)
        while (isActive && tick-- > 0 && checkState.code != 0) {
            // The last page does not load completely, wait for it.
            if (tick % 10 == 0) {
                val urlToWait = checkState.message
                logger.info("Waiting for page | {} | {} <- {}", tick, urlToWait, page.url)
            }

            delay(1000L)
            checkState = checkPreviousPage(driver)
        }
    }

    private fun checkPreviousPage(driver: WebDriver): CheckState {
        val navigateHistory = driver.browserInstance.navigateHistory

        val lastNav = navigateHistory.lastOrNull {
            it.pageId > 0 && !it.stopped && TaskDef.isShop(it.url)
                    && it.createTime < driver.navigateEntry.createTime
        } ?: return CheckState(0, "No previous page")

        val code = if (lastNav.documentReadyTime == MAX) 1 else 0
        return CheckState(code, lastNav.url)
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
    val url = "https://www.dianping.com/shop/Enk0gTkqu0Cyj7Ch"
    val args = "-i 1s -ignoreFailure -parse"

//    BrowserSettings.headless()

    val crawler = RestaurantCrawler()

    val fieldSelectors = TaskDef.fieldSelectors.mapValues { (_, selector) -> "$selector .ocr" }
    val fields = crawler.session.scrape(url, crawler.options(args), fieldSelectors)
    println(GsonBuilder().setPrettyPrinting().create().toJson(fields))
}
