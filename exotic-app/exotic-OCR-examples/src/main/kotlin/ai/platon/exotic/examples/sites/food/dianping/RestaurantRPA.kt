package ai.platon.exotic.examples.sites.food.dianping

import ai.platon.exotic.examples.sites.CommonRPA
import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.message.MiscMessageWriter
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.context.support.AbstractPulsarContext
import ai.platon.pulsar.crawl.CoreMetrics
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.persist.PageDatum
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.protocol.browser.emulator.BrowserResponseEvents
import ai.platon.pulsar.protocol.browser.emulator.BrowserResponseHandler
import ai.platon.pulsar.protocol.browser.emulator.util.HtmlIntegrityChecker
import ai.platon.pulsar.session.PulsarSession
import ai.platon.scent.context.ScentContexts
import ai.platon.scent.jackson.prettyScentObjectWritter
import com.fasterxml.jackson.core.JsonProcessingException
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import java.io.IOException
import java.nio.file.Files
import java.text.NumberFormat
import java.time.temporal.ChronoUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

class DianPingHtmlChecker: HtmlIntegrityChecker {
    override fun isRelevant(url: String): Boolean {
        return true
    }

    // Since we need to check the html integrity of the page, we need active dom urls,
    // which is calculated in javascript.
    override fun invoke(pageSource: String, pageDatum: PageDatum): HtmlIntegrity {
        val url = pageDatum.activeDOMUrls?.location ?: pageDatum.url
        // Authorization verification
        return when {
            "verify" in url -> HtmlIntegrity.ROBOT_CHECK_3
            "403 Forbidden" in pageSource -> HtmlIntegrity.FORBIDDEN
            else -> HtmlIntegrity.OK
        }
    }
}

class RestaurantRPA(
    val session: PulsarSession = ScentContexts.createSession()
): CommonRPA() {
    private val logger = getLogger(this)

    private val context = session.context as AbstractPulsarContext

    private val responseHandler get() = context.getBean<BrowserResponseHandler>()
    private val messageWriter = context.getBean(MiscMessageWriter::class)
    private val coreMetrics = context.getBean<CoreMetrics>()

    private val ocrFieldSelectors = TaskDef.fieldSelectors.mapValues { (_, selector) -> "$selector .ocr" }
    private val totalFields = AtomicInteger()
    private val numberFormat = NumberFormat.getInstance().apply { maximumFractionDigits = 2 }

    init {
        responseHandler.htmlIntegrityChecker.addLast(DianPingHtmlChecker())
    }

    fun options(args: String): LoadOptions {
        val options = session.options(args)

        registerEventHandlers(options)
        registerItemEventHandlers(options)

        return options
    }

    private fun registerEventHandlers(options: LoadOptions) {
        options.event.loadEvent.onHTMLDocumentParsed.addLast { _, document: FeaturedDocument ->
            collectPortalUrls(document) }
    }

    private fun registerItemEventHandlers(options: LoadOptions) {
        val ie = options.itemEvent

        ie.loadEvent.onWillLoad.addLast {
            it
        }

        ie.loadEvent.onWillFetch.addLast { page ->
            page.fetchRetries = 0
//            page.maxRetries = 6
            page.pageModel?.clear()
        }

        ie.loadEvent.onLoaded.addLast { page ->
            dumpPageModel(page)
        }

        val be = ie.browseEvent
        be.onWillCheckDocumentState.addLast { page, driver ->
        }

        be.onWillFetch.addLast { page, driver ->
            // TODO: the handler is already added by default
            waitForReferrer(page, driver)
            // TODO: the handler is already added by default
            waitForPreviousPage(page, driver)
        }

        // Warp up the browser to avoid being blocked by the server.
        be.onBrowserLaunched.addLast { page, driver ->
            // TODO: the handler is already added
            warnUpBrowser(page, driver)
        }

        be.onDocumentActuallyReady.addLast { page, driver ->
            driver.scrollTo("#comment")
            driver.waitForSelector("ul.comment-list li.comment-item")
        }

        be.onWillComputeFeature.addLast { page, driver ->
            driver.bringToFront()
            TaskDef.commentSelectors.entries.mapIndexed { i, _ -> TaskDef.cs(i) + " .more" }.shuffled()
                .asFlow().flowOn(Dispatchers.IO).collect { selector ->
                    if (driver.exists(selector)) {
                        driver.click(selector)
                        randomDelay(500L, 2_000)
                    }
                }
        }

        be.onFeatureComputed.addLast { page, driver ->
            driver.bringToFront()
            TaskDef.fieldSelectors.entries.shuffled().asFlow().flowOn(Dispatchers.IO).collect { (name, selector) ->
                val point = driver.clickablePoint(selector)
                if (point != null) {
                    driver.moveMouseTo(point.x, point.y)
                    Screenshot(page, driver).doOCR(name, selector)
                    randomDelay(1000, 3000)
                }
            }
        }

        be.onWillStopTab.addLast { page, driver ->
            val currentUrl = driver.currentUrl()
            if (TaskDef.isShop(currentUrl)) {
                if (Random.nextInt(2) == 0) {
                    // humanize(page, driver)
                }
            }
        }

        ie.loadEvent.onHTMLDocumentParsed.addLast { page, document ->
            val fields = page.variables.variables
                .filterKeys { it.startsWith(Screenshot.OCR) }
                .mapValues { it.value.toString() }
            if (fields.isEmpty()) {
                return@addLast null
            }

            page.ensurePageModel().emplace(0, "OCR", fields)

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

    private fun collectPortalUrls(document: FeaturedDocument) {
        document.select("a[data-cat-id]")
            .forEach { messageWriter.write(it.attr("abs:href"), "portal.urls.txt") }
    }

    private fun dumpPageModel(page: WebPage) {
        val fields = page.variables.variables.filterKeys { it.startsWith(Screenshot.OCR) }
        if (fields.isEmpty()) {
            return
        }

        val pageModel = page.pageModel ?: return
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
}

fun main() {
    val url = "https://www.dianping.com/shop/G3ZxMJTDLITGsxLX"
    val args = "-i 1s -ignoreFailure -parse"
//    BrowserSettings.headless()

    val rpa = RestaurantRPA()

    val fieldSelectors = TaskDef.fieldSelectors.mapValues { (_, selector) -> "$selector .ocr" }
    val options = rpa.options(args)
    options.referrer = "https://www.dianping.com/beijing/ch10/r2596"
    val fields = rpa.session.scrape(url, options, fieldSelectors)
    println(GsonBuilder().setPrettyPrinting().create().toJson(fields))
}
