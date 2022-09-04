package ai.platon.exotic.examples.sites

import ai.platon.exotic.examples.sites.food.dianping.RestaurantRPA
import ai.platon.exotic.examples.sites.food.dianping.TaskDef
import ai.platon.pulsar.common.AppContext
import ai.platon.pulsar.common.CheckState
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.crawl.fetch.driver.NavigateEntry
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.persist.WebPage
import java.time.Duration
import java.time.Instant
import kotlin.random.Random

open class CommonRPA {
    private val isActive get() = AppContext.isActive

    private val logger = getLogger(this)

    suspend fun waitForReferrer(page: WebPage, driver: WebDriver) {
        val referrer = page.referrer ?: return
        val referrerVisited = driver.browser.navigateHistory.any { it.url == referrer }
        if (!referrerVisited) {
            logger.debug("Visiting the referrer | {}", referrer)
            visit(referrer, driver)
        }
    }

    suspend fun waitForPreviousPage(page: WebPage, driver: WebDriver) {
        var tick = 0
        var checkState = checkPreviousPage(driver)
        while (tick++ <= 180 && checkState.code == RestaurantRPA.PREV_PAGE_WILL_READY) {
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

            kotlinx.coroutines.delay(1000L)
            checkState = checkPreviousPage(driver)
        }
    }

    fun checkPreviousPage(driver: WebDriver): CheckState {
        val navigateHistory = driver.browser.navigateHistory
        val now = Instant.now()

        val testNav = navigateHistory.lastOrNull { mayWaitFor(it, driver.navigateEntry) }

        val code = when {
            testNav == null -> RestaurantRPA.PREV_PAGE_WILL_READY
            testNav.documentReadyTime > now -> RestaurantRPA.PREV_PAGE_WILL_READY
            Duration.between(testNav.documentReadyTime, now).seconds > 10 -> RestaurantRPA.PREV_PAGE_READY
            Duration.between(testNav.lastActiveTime, now).seconds > 60 -> RestaurantRPA.PREV_PAGE_NEVER_READY
            !isActive -> RestaurantRPA.PREV_PAGE_NEVER_READY
            !driver.isWorking -> RestaurantRPA.PREV_PAGE_NEVER_READY
            else -> RestaurantRPA.PREV_PAGE_WILL_READY
        }

        return CheckState(code, testNav?.url ?: "")
    }

    fun mayWaitFor(currentEntry: NavigateEntry, testEntry: NavigateEntry): Boolean {
        val now = Instant.now()

        val may = testEntry.pageId > 0
                && !testEntry.stopped
                && TaskDef.isShop(testEntry.url)
                && testEntry.createTime < currentEntry.createTime
                && Duration.between(testEntry.lastActiveTime, now).seconds < 30

        return may
    }

    suspend fun warnUpBrowser(page: WebPage, driver: WebDriver) {
//        visit(TaskDef.homePage, driver)
        page.referrer?.let { visit(it, driver) }

        val pattern = page.url.substringAfterLast("/")
        // driver.clickMatches("ul li a[onclick]", "href", pattern)
        // TODO: create a new driver with the opened tab
    }

    suspend fun visit(url: String, driver: WebDriver) {
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

    suspend fun humanize(page: WebPage, driver: WebDriver) {
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

    suspend fun delay(timeMillis: Long, delta: Int) {
        kotlinx.coroutines.delay(timeMillis + Random.nextInt(delta))
    }
}
