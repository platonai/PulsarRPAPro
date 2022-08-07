package ai.platon.exotic.services.api

import ai.platon.exotic.driver.common.DEV_MAX_PENDING_TASKS
import ai.platon.exotic.driver.common.IS_DEVELOPMENT
import ai.platon.exotic.driver.common.PRODUCT_MAX_PENDING_TASKS
import ai.platon.exotic.driver.crawl.ExoticCrawler
import ai.platon.exotic.services.api.component.CrawlTaskRunner
import ai.platon.exotic.services.api.component.ScrapeResultCollector
import ai.platon.pulsar.common.DateTimes.MILLIS_PER_SECOND
import ai.platon.pulsar.common.stringify
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@EnableScheduling
class ExoticScheduler(
    private val exoticCrawler: ExoticCrawler,
    private val crawlTaskRunner: CrawlTaskRunner,
    private val crawlResultChecker: ScrapeResultCollector,
) {
    companion object {
        const val INITIAL_DELAY = 10 * MILLIS_PER_SECOND
        const val INITIAL_DELAY_2 = 30 * MILLIS_PER_SECOND + 10 * MILLIS_PER_SECOND
        const val INITIAL_DELAY_3 = 30 * MILLIS_PER_SECOND + 20 * MILLIS_PER_SECOND
    }

    private val logger = LoggerFactory.getLogger(ExoticScheduler::class.java)

    @Bean
    fun runStartupTasks() {
        if (!serverIsRunning()) {
            return
        }

        crawlTaskRunner.loadUnfinishedTasks()
    }

    @Scheduled(initialDelay = INITIAL_DELAY, fixedDelay = 10 * MILLIS_PER_SECOND)
    fun startCreatedCrawlRules() {
        if (!serverIsRunning()) {
            return
        }
        crawlTaskRunner.startCreatedCrawlRules()
    }

    @Scheduled(initialDelay = INITIAL_DELAY, fixedDelay = 10 * MILLIS_PER_SECOND)
    fun restartCrawlRules() {
        if (!serverIsRunning()) {
            return
        }
        crawlTaskRunner.restartCrawlRulesNextRound()
    }

    @Scheduled(initialDelay = INITIAL_DELAY_2, fixedDelay = 10 * MILLIS_PER_SECOND)
    fun runPortalTasksWhenFew() {
        if (!serverIsRunning()) {
            return
        }

        try {
            val submitter = exoticCrawler.outPageScraper.taskSubmitter
            val maxPendingTaskCount = if (IS_DEVELOPMENT) DEV_MAX_PENDING_TASKS else PRODUCT_MAX_PENDING_TASKS

            if (submitter.pendingTaskCount >= maxPendingTaskCount) {
                return
            }

            if (submitter.pendingPortalTaskCount > 2) {
                return
            }

            crawlTaskRunner.loadAndSubmitPortalTasks(2)
        } catch (t: Throwable) {
            logger.warn(t.stringify())
        }
    }

    @Scheduled(initialDelay = INITIAL_DELAY_2, fixedDelay = 10 * MILLIS_PER_SECOND)
    fun runRetryingTasks() {
        if (!serverIsRunning()) {
            return
        }

        try {
            val submitter = exoticCrawler.outPageScraper.taskSubmitter
            val maxPendingTaskCount = if (IS_DEVELOPMENT) DEV_MAX_PENDING_TASKS else PRODUCT_MAX_PENDING_TASKS

            if (submitter.pendingTaskCount >= maxPendingTaskCount) {
                return
            }

            val limit = maxPendingTaskCount - submitter.pendingTaskCount
            crawlTaskRunner.submitRetryingScrapeTasks(limit)
        } catch (t: Throwable) {
            logger.warn(t.stringify())
        }
    }

    @Scheduled(initialDelay = INITIAL_DELAY_3, fixedDelay = 30 * MILLIS_PER_SECOND)
    fun synchronizeProducts() {
        if (!serverIsRunning()) {
            return
        }
        crawlResultChecker.synchronizeProducts()
    }

    private fun serverIsRunning(): Boolean {
        val submitter = exoticCrawler.outPageScraper.taskSubmitter

        return try {
            submitter.driver.count()
            true
        } catch (e: Exception) {
            false
        }
    }
}
