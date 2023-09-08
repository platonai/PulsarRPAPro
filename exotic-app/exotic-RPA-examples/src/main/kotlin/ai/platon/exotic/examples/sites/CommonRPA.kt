package ai.platon.exotic.examples.sites

import ai.platon.pulsar.common.AppContext
import ai.platon.pulsar.common.CheckState
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.crawl.fetch.driver.NavigateEntry
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.persist.WebPage
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant
import kotlin.random.Random

open class CommonRPA {
    companion object {
        const val PREV_PAGE_WILL_READY = 0
        const val PREV_PAGE_READY = 1
        const val PREV_PAGE_NEVER_READY = 3
    }

    private val isActive get() = AppContext.isActive

    private val logger = getLogger(this)

    open suspend fun waitForReferrer(page: WebPage, driver: WebDriver) {
        val referrer = page.referrer ?: return

        val referrerVisited = driver.browser.navigateHistory.any { it.url == referrer }
        if (!referrerVisited) {
            logger.debug("Visiting the referrer | {}", referrer)
            visit(referrer, driver)
        }
    }

    open suspend fun waitForPreviousPage(page: WebPage, driver: WebDriver) {
        var tick = 0
        var checkState = checkPreviousPage(driver)
        while (tick++ <= 180 && checkState.code == PREV_PAGE_WILL_READY) {
            if (checkState.message.isBlank()) {
                // The browser has just started, don't crowd into.
                randomDelay(1_000, 10_000)
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

    open fun checkPreviousPage(driver: WebDriver): CheckState {
        val navigateHistory = driver.browser.navigateHistory
        val now = Instant.now()

        val testNav = navigateHistory.lastOrNull { mayWaitFor(it, driver.navigateEntry) }

        val code = when {
            !isActive -> PREV_PAGE_NEVER_READY
            !driver.isWorking -> PREV_PAGE_NEVER_READY
            testNav == null -> PREV_PAGE_WILL_READY
            testNav.documentReadyTime > now -> PREV_PAGE_WILL_READY
            Duration.between(testNav.documentReadyTime, now).seconds > 10 -> PREV_PAGE_READY
            Duration.between(testNav.lastActiveTime, now).seconds > 60 -> PREV_PAGE_NEVER_READY
            else -> PREV_PAGE_WILL_READY
        }

        return CheckState(code, testNav?.url ?: "")
    }

    open fun mayWaitFor(currentEntry: NavigateEntry, testEntry: NavigateEntry): Boolean {
        val now = Instant.now()

        val may = testEntry.pageId > 0
                && !testEntry.stopped
                && testEntry.createTime < currentEntry.createTime
                && Duration.between(testEntry.lastActiveTime, now).seconds < 30

        return may
    }

    open suspend fun warnUpBrowser(page: WebPage, driver: WebDriver) {
//        visit(TaskDef.homePage, driver)
        page.referrer?.let { visit(it, driver) }

        val pattern = page.url.substringAfterLast("/")
        // driver.clickMatches("ul li a[onclick]", "href", pattern)
    }

    open suspend fun visit(url: String, driver: WebDriver) {
        val display = driver.browser.id.display
        logger.info("Visiting with browser #{} | {}", display, url)

        driver.navigateTo(url)
        driver.waitForSelector("body")
        var n = 2 + Random.nextInt(5)
        while (n-- > 0 && isActive) {
            val deltaY = 100.0 + 20 * Random.nextInt(10)
            driver.mouseWheelDown(deltaY = deltaY)
            randomDelay(500, 500)
        }

        logger.debug("Visited | {}", url)
    }

    open suspend fun humanize(page: WebPage, driver: WebDriver) {
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
            randomDelay(15_000, 10_000)
            driver.scrollToMiddle(0.25f)
        }
    }

    suspend fun randomDelay(timeMillis: Long, delta: Int) {
        delay(timeMillis + Random.nextInt(delta))
    }
}
