package ai.platon.exotic.services

import ai.platon.exotic.driver.common.DEV_MAX_PENDING_TASKS
import ai.platon.exotic.driver.common.IS_DEVELOPMENT
import ai.platon.exotic.driver.common.PRODUCT_MAX_PENDING_TASKS
import ai.platon.exotic.driver.crawl.ExoticCrawler
import ai.platon.exotic.services.component.CrawlTaskRunner
import ai.platon.exotic.services.component.ScrapeResultCollector
import ai.platon.pulsar.common.DateTimes.MILLIS_OF_SECOND
import ai.platon.pulsar.common.stringify
import com.cronutils.model.Cron
import com.cronutils.model.CronType
import com.cronutils.model.definition.CronDefinitionBuilder
import com.cronutils.model.time.ExecutionTime
import com.cronutils.parser.CronParser
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.ZoneId
import java.time.ZonedDateTime

@Component
@EnableScheduling
class ExoticScheduler(
    private val exoticCrawler: ExoticCrawler,
    private val crawlTaskRunner: CrawlTaskRunner,
    private val crawlResultChecker: ScrapeResultCollector,
) {
    companion object {
        const val INITIAL_DELAY = 10 * MILLIS_OF_SECOND
        const val INITIAL_DELAY_2 = 30 * MILLIS_OF_SECOND + 10 * MILLIS_OF_SECOND
        const val INITIAL_DELAY_3 = 30 * MILLIS_OF_SECOND + 20 * MILLIS_OF_SECOND
    }

    private val logger = LoggerFactory.getLogger(ExoticScheduler::class.java)

    @Bean
    fun runStartupTasks() {
        crawlTaskRunner.loadUnfinishedTasks()
    }

    @Scheduled(initialDelay = INITIAL_DELAY, fixedDelay = 10 * MILLIS_OF_SECOND)
    fun startCreatedCrawlRules() {
        crawlTaskRunner.startCreatedCrawlRules()
    }

    @Scheduled(initialDelay = INITIAL_DELAY, fixedDelay = 10 * MILLIS_OF_SECOND)
    fun restartCrawlRules() {
        crawlTaskRunner.restartCrawlRulesNextRound()
    }

    @Scheduled(initialDelay = INITIAL_DELAY_2, fixedDelay = 10 * MILLIS_OF_SECOND)
    fun runPortalTasksWhenFew() {
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

    @Scheduled(initialDelay = INITIAL_DELAY_3, fixedDelay = 30 * MILLIS_OF_SECOND)
    fun synchronizeProducts() {
        crawlResultChecker.synchronizeProducts()
    }
}
