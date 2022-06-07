package ai.platon.exotic.services.api.component

import ai.platon.exotic.driver.common.IS_DEVELOPMENT
import ai.platon.exotic.driver.crawl.ExoticCrawler
import ai.platon.exotic.driver.crawl.entity.CrawlRule
import ai.platon.exotic.driver.crawl.entity.PortalTask
import ai.platon.exotic.driver.crawl.scraper.*
import ai.platon.exotic.services.api.persist.CrawlRuleRepository
import ai.platon.exotic.services.api.persist.PortalTaskRepository
import ai.platon.pulsar.common.DateTimes
import ai.platon.pulsar.common.collect.queue.ConcurrentNEntrantQueue
import ai.platon.pulsar.common.stringify
import ai.platon.pulsar.common.urls.UrlUtils
import com.cronutils.model.Cron
import com.cronutils.model.CronType
import com.cronutils.model.definition.CronDefinitionBuilder
import com.cronutils.model.time.ExecutionTime
import com.cronutils.parser.CronParser
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.temporal.ChronoUnit

@Component
class CrawlTaskRunner(
    val crawlRuleRepository: CrawlRuleRepository,
    val portalTaskRepository: PortalTaskRepository,
    val scraper: ExoticCrawler
) {
    private val logger = LoggerFactory.getLogger(CrawlTaskRunner::class.java)

    private val retryingPortalTasks = ConcurrentNEntrantQueue<ScrapeTask>(5)
    private val retryingItemTasks = ConcurrentNEntrantQueue<ScrapeTask>(3)

    @Synchronized
    fun loadUnfinishedTasks() {
        // portalTaskRepository.findAllByStatus("Running")
    }

    @Synchronized
    fun startCreatedCrawlRules() {
        val now = Instant.now()

        val status = listOf(RuleStatus.Created).map { it.toString() }
        val sort = Sort.by(Sort.Order.desc("id"))
        val page = PageRequest.of(0, 1000, sort)

        val rules = crawlRuleRepository.findAllByStatusIn(status, page)
            .filter { (it.startTime?.epochSecond ?: 0) <= now.epochSecond }

        rules.forEach { rule -> startCrawl(rule) }
    }

    @Synchronized
    fun restartCrawlRulesNextRound() {
        val status = listOf(RuleStatus.Running, RuleStatus.Finished).map { it.toString() }
        val sort = Sort.by(Sort.Order.desc("id"))
        val page = PageRequest.of(0, 1000, sort)
        val rules = crawlRuleRepository.findAllByStatusIn(status, page)
            .filter { shouldRun(it) }

        rules.forEach { rule -> startCrawl(rule) }
    }

    fun shouldRun(rule: CrawlRule): Boolean {
        return try {
            shouldRun0(rule)
        } catch (e: Exception) {
            logger.warn(e.stringify())
            false
        }
    }

    @Synchronized
    fun startCrawl(rule: CrawlRule) {
        try {
            val now = Instant.now()

            rule.status = RuleStatus.Running.toString()
            rule.crawlCount = rule.crawlCount?.inc()
            rule.lastCrawlTime = now
            crawlRuleRepository.save(rule)
            crawlRuleRepository.flush()

            val portalUrls = rule.portalUrls

            if (portalUrls.isBlank()) {
                rule.status = RuleStatus.Finished.toString()
                logger.info("No portal urls in rule #{}", rule.id)
                return
            }

            val maxPages = if (IS_DEVELOPMENT) 2 else rule.maxPages
            val pagedPortalUrls = portalUrls.split("\n")
                .map { it.trim() }
                .filter { UrlUtils.isValidUrl(it) }
                .distinct()
                .flatMap { url -> createPagedUrls(url, maxPages) }
            if (pagedPortalUrls.isEmpty()) {
                logger.info("No portal urls in rule #{}", rule.id)
            }

            // the client controls the retry
            val portalTasks = pagedPortalUrls.map {
                PortalTask(it, "-refresh -nMaxRetry 0", 3).also {
                    it.rule = rule
                    it.status = TaskStatus.CREATED
                }
            }

            crawlRuleRepository.save(rule)
            portalTaskRepository.saveAll(portalTasks)

            logger.debug("Created {} portal tasks", portalTasks.size)
        } catch (t: Throwable) {
            logger.warn(t.stringify())
        }
    }

    fun loadAndSubmitPortalTask(task: PortalTask) {
        task.startTime = Instant.now()
        task.status = TaskStatus.LOADED
        portalTaskRepository.save(task)
        scraper.scrapeOutPages(createListenablePortalTask(task, true))
    }

    fun loadAndSubmitPortalTasks(limit: Int) {
        val order = Sort.Order.asc("id")
        val pageRequest = PageRequest.of(0, limit, Sort.by(order))
        val portalTasks = portalTaskRepository.findAllByStatus(TaskStatus.CREATED, pageRequest)
        if (portalTasks.isEmpty) {
            return
        }

        portalTasks.forEach {
            it.startTime = Instant.now()
            it.status = TaskStatus.LOADED
        }
        portalTaskRepository.saveAll(portalTasks)

        portalTasks.shuffled()
            .asSequence()
            .map { createListenablePortalTask(it, true) }
            .forEach { task -> scraper.scrapeOutPages(task) }
    }

    fun submitRetryingScrapeTasks(limit: Int) {
        val retryingTasks = retryingItemTasks.take(limit)
        retryingTasks.forEach {
            scraper.scrape(createListenableScrapeTask(it))
        }
    }

    fun createListenablePortalTask(portalTask: PortalTask, refresh: Boolean = false): ListenablePortalTask {
        return ListenablePortalTask(
            portalTask, refresh = refresh,

            onSubmitted = {
                val rule = portalTask.rule

                it.status = TaskStatus.SUBMITTED

                portalTask.serverTaskId = it.serverTaskId
                portalTask.status = TaskStatus.SUBMITTED
                portalTaskRepository.save(portalTask)
            },
            onRetry = {
                it.status = TaskStatus.RETRYING

                portalTask.status = TaskStatus.RETRYING
                portalTaskRepository.save(portalTask)

                retryingPortalTasks.add(it)
            },
            onSuccess = {
                it.status = TaskStatus.OK

                portalTask.status = TaskStatus.OK
                portalTaskRepository.save(portalTask)
            },
            onFailed = {
                it.status = TaskStatus.FAILED

                portalTask.status = TaskStatus.FAILED
                portalTaskRepository.save(portalTask)
            },
            onFinished = {

            },
            onTimeout = {

            },

            onItemSubmitted = {
                it.status = TaskStatus.SUBMITTED

                portalTask.status = TaskStatus.SUBMITTED
                // portalTask.serverTaskId = it.id
                ++portalTask.submittedCount
                portalTaskRepository.save(portalTask)
            },
            onItemRetry = {
                it.status = TaskStatus.RETRYING

                portalTask.status = TaskStatus.RETRYING
                ++portalTask.retryCount
                portalTaskRepository.save(portalTask)

                it.status = TaskStatus.RETRYING
                retryingItemTasks.add(it)
            },
            onItemSuccess = {
                it.status = TaskStatus.OK

                portalTask.status = TaskStatus.OK
                ++portalTask.successCount
            },
            onItemFailed = {
                it.status = TaskStatus.FAILED

                portalTask.status = TaskStatus.FAILED
                ++portalTask.failedCount
                ++portalTask.finishedCount
                portalTaskRepository.save(portalTask)
            },
            onItemFinished = {
                ++portalTask.finishedCount
                portalTaskRepository.save(portalTask)
            },
            onItemTimeout = {

            },
        )
    }

    fun createListenableScrapeTask(task: ScrapeTask, refresh: Boolean = false): ListenableScrapeTask {
        val portalTask = task.companionPortalTask
        val listenableScrapeTask = ListenableScrapeTask(task)
        listenableScrapeTask.onSubmitted = {
            task.status = TaskStatus.SUBMITTED

            portalTask?.also {
                it.status = TaskStatus.SUBMITTED
                // portalTask.serverTaskId = it.id
                ++it.submittedCount
                portalTaskRepository.save(it)
            }
        }
        listenableScrapeTask.onRetry = {
            portalTask?.also {
                it.status = TaskStatus.RETRYING
                ++it.retryCount
                portalTaskRepository.save(it)
            }

            task.status = TaskStatus.RETRYING
            retryingItemTasks.add(task)
        }
        listenableScrapeTask.onSuccess = {
            task.status = TaskStatus.OK

            portalTask?.also {
                it.status = TaskStatus.OK
                ++it.successCount
                ++it.finishedCount
                portalTaskRepository.save(it)
            }
        }
        listenableScrapeTask.onFailed = {
            task.status = TaskStatus.FAILED

            portalTask?.also {
                it.status = TaskStatus.FAILED
                ++it.failedCount
                ++it.finishedCount
                portalTaskRepository.save(it)
            }
        }
        listenableScrapeTask.onFinished = {

        }
        listenableScrapeTask.onTimeout = {

        }

        return listenableScrapeTask
    }

    private fun shouldRun0(rule: CrawlRule): Boolean {
        val lastCrawlTime = rule.lastCrawlTime ?: Instant.EPOCH
        if (rule.period.seconds > 0) {
            val now = Instant.now()
            if (lastCrawlTime + rule.period <= now) {
                return true
            }
        }

        val expression = rule.cronExpression
        if (expression.isNullOrBlank()) {
            return false
        }

        val cronDefinition = CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ)
        val parser = CronParser(cronDefinition)
        val quartzCron: Cron = parser.parse(expression)
        quartzCron.validate()
        val executionTime = ExecutionTime.forCron(quartzCron)

        val zonedLastCrawlTime = lastCrawlTime.atZone(DateTimes.zoneId)
        val timeToNextExecution = executionTime.timeToNextExecution(zonedLastCrawlTime)
        if (timeToNextExecution.isPresent && timeToNextExecution.get().seconds <= 0) {
            return true
        }

        return false
    }

    private fun createPagedUrls(url: String, maxPages: Int): List<String> {
        return if (url.contains("{{page}}")) {
            IntRange(1, maxPages).map { pg -> url.replace("{{page}}", pg.toString()) }
        } else listOf(url)
    }
}
