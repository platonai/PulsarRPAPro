package ai.platon.exotic.examples.sites.food.dianping

import ai.platon.pulsar.common.AppFiles
import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.HtmlIntegrity
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.context.support.AbstractPulsarContext
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.persist.PageDatum
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.protocol.browser.emulator.BrowserEmulatorEventHandler
import ai.platon.pulsar.protocol.browser.emulator.HtmlIntegrityChecker
import ai.platon.pulsar.session.PulsarSession
import ai.platon.scent.context.ScentContexts
import com.google.gson.GsonBuilder
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import net.sourceforge.tess4j.Tesseract
import java.awt.RenderingHints
import java.awt.Transparency
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.util.*
import javax.imageio.ImageIO

class DianPingHtmlIntegrityChecker: HtmlIntegrityChecker {
    // Since we need to check the html integrity of the page, we need active dom urls, which is calculated in javascript.
    override fun invoke(pageSource: String, pageDatum: PageDatum): HtmlIntegrity {
        val url = pageDatum.activeDomUrls?.location ?: pageDatum.url
        // Authorization verification
        if ("verify" in url) {
            return HtmlIntegrity.FORBIDDEN
        }
        if ("403 Forbidden" in pageSource) {
            return HtmlIntegrity.FORBIDDEN
        }
        return HtmlIntegrity.OK
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

    private fun getScaledImage(srcImg: BufferedImage, w: Int, h: Int): BufferedImage {
        val resizedImg = BufferedImage(w, h, Transparency.TRANSLUCENT)
        val g2 = resizedImg.createGraphics()
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        g2.drawImage(srcImg, 0, 0, w, h, null)
        g2.dispose()
        return resizedImg
    }
}

class RestaurantCrawler(
    val session: PulsarSession = ScentContexts.createSession()
) {
    private val logger = getLogger(this)

    private val context = session.context as AbstractPulsarContext

    private val coroutineScope = CoroutineScope(Dispatchers.IO) + CoroutineName("RestaurantCrawler")

    val commentSelectors = IntRange(1, 10)
        .map { i ->
            listOf(
                "comment-$i-user-name" to commentSelector(i) + " .content .user-info p",
                "comment-$i-avePrice" to commentSelector(i) + " .content .shop-info .average",
                "comment-$i-desc" to commentSelector(i) + " .content p.desc.J-desc",
                "comment-$i-publishTime" to commentSelector(i) + " .content .misc-info .time",
                "comment-$i-praise" to commentSelector(i) + " .content .misc-info .J-praise",
                "comment-$i-response" to commentSelector(i) + " .content .misc-info .J-response",
                "comment-$i-favorite" to commentSelector(i) + " .content .misc-info .J-favorite",
                "comment-$i-report" to commentSelector(i) + " .content .misc-info .J-report",
                "comment-$i-shop" to commentSelector(i) + " .content .misc-info .shop"
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

    val portalUrl = "https://www.dianping.com/beijing/ch10/g104"

    val warnUpUrls = listOf(
        "https://www.dianping.com/",
        "https://www.dianping.com/beijing/ch10/g104",
        "https://www.dianping.com/beijing/ch75/g34309",
        "https://www.dianping.com/beijing/ch25/g136",
        "https://www.dianping.com/beijing/ch25/g105",
        "https://www.dianping.com/beijing/ch25/g106",
        "https://www.dianping.com/beijing/ch25/g107",
    )

    init {
        context.getBean<BrowserEmulatorEventHandler>().htmlIntegrityChecker.checkers.add(0, DianPingHtmlIntegrityChecker())
    }

    fun options(args: String): LoadOptions {
        val options = session.options(args)
        val eh = options.ensureItemEventHandler()

        eh.loadEventHandler.onBeforeLoad.addLast {
            // sleepSeconds(3)
        }

        // Warp up the browser to avoid the browser being blocked by the server.
        eh.loadEventHandler.onAfterBrowserLaunch.addLast { driver ->
            runBlocking { visit(warnUpUrls[0], driver) }
            runBlocking { visit(portalUrl, driver) }

            warnUpUrls.shuffled().take(1).forEach {
                runBlocking { visit(it, driver) }
            }
        }

        val seh = eh.simulateEventHandler
        seh.onAfterCheckDOMState.addLast { page, driver ->
            // driver.waitForSelector("#reviewlist-wrapper li.comment-item")
        }

        seh.onBeforeComputeFeature.addLast { page, driver ->
            driver.bringToFront()
            commentSelectors.entries.mapIndexed { i, _ -> commentSelector(i) + " .more" }
                .asFlow().flowOn(Dispatchers.IO).collect { selector ->
                    if (driver.exists(selector)) {
                        driver.click(selector)
                        delay(500)
                    }
                }
        }

        seh.onAfterComputeFeature.addLast { page, driver ->
            driver.bringToFront()
            fieldSelectors.entries.asFlow().flowOn(Dispatchers.IO).collect { (name, selector) ->
                // driver.waitForSelector(selector)
                if (driver.exists(selector)) {
                    Screenshot(page, driver).runCatching { doOCR(name, selector) }
                        .onFailure { logger.warn("Unexpected exception", it) }.getOrNull()
                    delay(300)
                }
            }
        }

        eh.loadEventHandler.onAfterHtmlParse.addLast { page, document ->
            page.variables.variables.filterKeys { it.startsWith(Screenshot.OCR) }.forEach { (key, text) ->
                val selector = key.substringAfter(Screenshot.OCR)

                val ele = document.selectFirstOrNull(selector)
                if (ele != null) {
                    ele.appendElement("br")
                    ele.appendElement("div").addClass("ocr").text(text.toString())
                }
            }
        }

        return options
    }

    private suspend fun visit(url: String, driver: WebDriver) {
        try {
            driver.navigateTo(portalUrl)
            delay(1000)
            var n = 10
            while (n-- > 0) {
                driver.scrollDown(1)
                delay(1000)
            }
        } catch (e: Exception) {
            logger.warn("Can not visit $url", e)
        }
    }

    private fun commentSelector(i: Int): String {
        return "#reviewlist-wrapper li.comment-item:nth-child($i)"
    }
}

/**
 * Running the program directly in the IDE may crash the system, use command line instead:
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

    val fieldSelectors = crawler.fieldSelectors.mapValues { (_, selector) -> "$selector .ocr" }
    val fields = crawler.session.scrape(url, crawler.options(args), fieldSelectors)
    println(GsonBuilder().setPrettyPrinting().create().toJson(fields))
}
