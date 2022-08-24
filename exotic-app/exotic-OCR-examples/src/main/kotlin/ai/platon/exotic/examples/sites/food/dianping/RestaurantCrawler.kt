package ai.platon.exotic.examples.sites.food.dianping

import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.context.support.AbstractPulsarContext
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.persist.PageDatum
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.protocol.browser.emulator.BrowserResponseHandler
import ai.platon.pulsar.protocol.browser.emulator.HtmlIntegrityChecker
import ai.platon.pulsar.session.PulsarSession
import ai.platon.scent.context.ScentContexts
import ai.platon.scent.jackson.prettyScentObjectWritter
import com.google.gson.GsonBuilder
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import net.sourceforge.tess4j.Tesseract
import java.io.ByteArrayInputStream
import java.time.Instant.MAX
import java.util.*
import javax.imageio.ImageIO

object TaskDef {

    val commentSelectors = IntRange(1, 10)
        .map { i ->
            listOf(
                "comment-$i-user-name" to cs(i) + " .content .user-info p",
                "comment-$i-avePrice" to cs(i) + " .content .shop-info .average",
                "comment-$i-desc" to cs(i) + " .content p.desc.J-desc",
//                "comment-$i-publishTime" to cs(i) + " .content .misc-info .time",
//                "comment-$i-praise" to cs(i) + " .content .misc-info .J-praise",
//                "comment-$i-response" to cs(i) + " .content .misc-info .J-response",
//                "comment-$i-favorite" to cs(i) + " .content .misc-info .J-favorite",
//                "comment-$i-report" to cs(i) + " .content .misc-info .J-report",
//                "comment-$i-shop" to cs(i) + " .content .misc-info .shop"
            )
        }.flatten().associate { it.first to it.second }

    val fieldSelectors = mutableMapOf(
        "shopName" to ".basic-info .shop-name",
        "score" to ".basic-info .brief-info .mid-score",
        "reviewCount" to "#reviewCount",
        "avgPrice" to "#avgPriceTitle",
        "commentScores" to "#comment_score",
        "address" to "#address",
        "tel" to ".tel",
    )
        .also { it.putAll(commentSelectors) }

    val homePage = "https://www.dianping.com/"

    val portalUrls = listOf(
        "https://www.dianping.com/beijing/ch10/g104",
        "https://www.dianping.com/beijing/ch10/g105",
        "https://www.dianping.com/beijing/ch10/g106",
        "https://www.dianping.com/beijing/ch10/g107",
        "https://www.dianping.com/beijing/ch10/g109",
        "https://www.dianping.com/beijing/ch10/g110",
        "https://www.dianping.com/beijing/ch75/g34309",
        "https://www.dianping.com/beijing/ch25/g136",
        "https://www.dianping.com/beijing/ch25/g105",
        "https://www.dianping.com/beijing/ch25/g106",
        "https://www.dianping.com/beijing/ch25/g107",
        "https://www.dianping.com/beijing/ch30",
        "https://www.dianping.com/beijing/ch30/g141",
        "https://www.dianping.com/beijing/ch30/g135",
        "https://www.dianping.com/beijing/ch30/g144",
        "https://www.dianping.com/beijing/ch30/g134",
    )

    fun cs(i: Int) = buildCommentSelector(i)

    fun buildCommentSelector(i: Int): String {
        return "#reviewlist-wrapper li.comment-item:nth-child($i)"
    }

    fun isShop(url: String): Boolean {
        return "shop" in url
    }
}

class DianPingHtmlIntegrityChecker: HtmlIntegrityChecker {
    // Since we need to check the html integrity of the page, we need active dom urls, which is calculated in javascript.
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

class Screenshot(
    val page: WebPage,
    val driver: WebDriver
) {
    companion object {
        val OCR = "OCR-"
    }

    private val logger = getLogger(this)

    private val screenshotDir = AppPaths.WEB_CACHE_DIR
        .resolve("screenshot")
        .resolve(AppPaths.fileId(page.url))

    private val tesseract get() = Tesseract().apply {
        setDatapath("/usr/share/tesseract-ocr/4.00/tessdata/")
        setLanguage("chi_sim")
        // setConfigs(listOf("--dpi 70"))
        setTessVariable("user_defined_dpi", "70")
    }

    suspend fun doOCR(name: String, selector: String): String? {
        val screenshot = driver.captureScreenshot(selector)
        if (screenshot == null) {
            logger.info("Failed to take screenshot | {} | {}", selector, page.url)
            return null
        }

        val path = screenshotDir.resolve("$name.jpg")
        val bytes = Base64.getDecoder().decode(screenshot)
        if (page.id < 1000) {
            AppFiles.saveTo(bytes, path, true)
        }

        val image = ImageIO.read(ByteArrayInputStream(bytes))
        val text = tesseract.doOCR(image)
        page.setVar("$OCR$selector", text)

        return text
    }
}

class RestaurantCrawler(
    val session: PulsarSession = ScentContexts.createSession()
) {
    private val logger = getLogger(this)

    private val context = session.context as AbstractPulsarContext

    private val isActive get() = AppContext.isActive
    private val htmlIntegrityChecker get() = context.getBean<BrowserResponseHandler>().htmlIntegrityChecker

    init {
        htmlIntegrityChecker.checkers.add(0, DianPingHtmlIntegrityChecker())
    }

    fun options(args: String): LoadOptions {
        val options = session.options(args)
        val eh = options.ensureItemEventHandler()

        eh.loadEventHandler.onBeforeLoad.addLast {
            // sleepSeconds(3)
        }

        eh.loadEventHandler.onBeforeFetch.addLast { page ->

        }

        eh.simulateEventHandler.onBeforeFetch.addLast { page, driver ->
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
            driver.bringToFront()
            TaskDef.commentSelectors.entries.mapIndexed { i, _ -> TaskDef.cs(i) + " .more" }
                .asFlow().flowOn(Dispatchers.IO).collect { selector ->
                    if (driver.exists(selector)) {
                        driver.click(selector)
                        delay(500)
                    }
                }
        }

        seh.onAfterComputeFeature.addLast { page, driver ->
            driver.evaluate("window.stop()")
            driver.evaluate("__pulsar_utils__.scrollToTop()") // Scroll to the top of the page.
            driver.bringToFront()
            TaskDef.fieldSelectors.entries.asFlow().flowOn(Dispatchers.IO).collect { (name, selector) ->
                if (driver.exists(selector)) {
                    Screenshot(page, driver).runCatching { doOCR(name, selector) }
                        .onFailure { logger.warn("Unexpected exception", it) }.getOrNull()
                    delay(500)
                }
            }
        }

        eh.loadEventHandler.onAfterHtmlParse.addLast { page, document ->
            page.variables.variables.filterKeys { it.startsWith(Screenshot.OCR) }.forEach { (key, text) ->
                val selector = key.substringAfter(Screenshot.OCR)

                val ele = document.selectFirstOrNull(selector)
                if (ele != null) {
                    ele.appendElement("div")
                        .attr("style", "display: none")
                        .addClass("ocr").text(text.toString())
                }
            }
        }

        return options
    }

    private suspend fun warnUpBrowser(page: WebPage, driver: WebDriver) {
        // portalUrls.shuffled().first().let { visit(it, driver) }
        page.referrer?.let { visit(it, driver) }
    }

    private suspend fun visit(url: String, driver: WebDriver) {
        val display = driver.browserInstance.id.display
        logger.info("Visit with browser #{} | {}", display, url)

        try {
            driver.navigateTo(url)
            driver.waitForSelector("body")
            var n = 10
            while (isActive && n-- > 0) {
                driver.scrollDown(1)
//                driver.evaluate("__pulsar_utils__.scrollDown()")
                delay(1000)
            }

            logger.info("Visited | {}", url)
        } catch (e: Exception) {
            logger.warn("Can not visit $url", e)
        }
    }

    private suspend fun waitForPreviousPage(page: WebPage, driver: WebDriver) {
        var tick = 60
        var checkState = checkPreviousPage(driver)
        while (isActive && tick-- > 0 && checkState.code != 0) {
            // The last page does not load completely, wait for it.
            if (tick % 10 == 0) {
                val navigateHistory = driver.browserInstance.navigateHistory
                // println(prettyScentObjectWritter().writeValueAsString(navigateHistory))
                logger.info("Waiting for page to load | {}.\t{} <- {}", tick, page.url, checkState.message)
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
