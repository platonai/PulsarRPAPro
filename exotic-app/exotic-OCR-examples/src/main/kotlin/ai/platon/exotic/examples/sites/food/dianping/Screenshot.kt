package ai.platon.exotic.examples.sites.food.dianping

import ai.platon.pulsar.common.*
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.crawl.fetch.driver.WebDriverCancellationException
import ai.platon.pulsar.crawl.fetch.driver.WebDriverException
import ai.platon.pulsar.persist.WebPage
import net.sourceforge.tess4j.Tesseract
import net.sourceforge.tess4j.TesseractException
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.SystemUtils
import java.io.ByteArrayInputStream
import java.io.IOException
import java.nio.file.Path
import java.text.SimpleDateFormat
import java.util.*
import javax.imageio.ImageIO

class Screenshot(
    val page: WebPage,
    val driver: WebDriver
) {
    companion object {
        val OCR = "OCR"
        private val PATH_SAFE_FORMAT_5 = SimpleDateFormat("HHmmss")
        val TASK_NAME = PATH_SAFE_FORMAT_5.format(Date())
        val SCREENSHOT_TASK_DIR = AppPaths.WEB_CACHE_DIR
            .resolve("screenshot")
            .resolve(DateTimes.PATH_SAFE_FORMAT_1.format(Date()))
            .resolve(TASK_NAME)

        fun generateScreenshotDir(page: WebPage): Path {
            return SCREENSHOT_TASK_DIR.resolve(StringUtils.leftPad(page.id.toString(), 4, '0'))
        }
    }

    private val logger = getLogger(this)

    private val tesseract
        get() = Tesseract().apply {
            if (SystemUtils.IS_OS_LINUX) {
                setDatapath("/usr/share/tesseract-ocr/4.00/tessdata/")
            } else if (SystemUtils.IS_OS_WINDOWS) {
                setDatapath("D:\\Users\\Administrator\\AppData\\Local\\Tesseract-OCR\\tessdata")
            }
            setLanguage("chi_sim")
            // setConfigs(listOf("--dpi 70"))
            setTessVariable("user_defined_dpi", "70")
        }

    private val screenshotDir get() = generateScreenshotDir(page)

    suspend fun doOCR(name: String, selector: String): String? {
        try {
            return doOCR0(name, selector)
        } catch (e: WebDriverCancellationException) {
            logger.warn(e.message)
        } catch (e: WebDriverException) {
            logger.warn(e.message)
        } catch (e: TesseractException) {
            logger.warn(e.message)
        } catch (e: IOException) {
            logger.warn(e.message)
        } catch (e: Exception) {
            logger.warn(e.stringify("[Unexpected]"))
        }

        return null
    }

    @Throws(WebDriverException::class, IOException::class, TesseractException::class)
    private suspend fun doOCR0(name: String, selector: String): String? {
        val screenshot = driver.captureScreenshot(selector)
        if (screenshot == null) {
            if (driver.exists(selector)) {
                logger.info("Failed to take screenshot | {} | {}", selector, page.url)
            } else {
                logger.info("Can not take screenshot, node does not exist | {} | {}", selector, page.url)
            }

            return null
        }

        val bytes = Base64.getDecoder().decode(screenshot)

        val image = ImageIO.read(ByteArrayInputStream(bytes))
        val text = tesseract.doOCR(image)
        if (text.isNotBlank()) {
            page.setVar("$OCR$selector", text)

            if (page.id < 1000000) {
                val path = screenshotDir.resolve("$name.jpg")
                val textPath = path.resolveSibling("$name.txt")
                AppFiles.saveTo(bytes, path, true)
                AppFiles.saveTo(text, textPath, true)
            }
        }

        return text
    }
}
