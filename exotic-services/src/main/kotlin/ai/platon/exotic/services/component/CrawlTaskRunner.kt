package ai.platon.exotic.services.component

import ai.platon.exotic.driver.common.IS_DEVELOPMENT
import ai.platon.exotic.driver.crawl.ExoticCrawler
import ai.platon.exotic.driver.crawl.entity.CrawlRule
import ai.platon.exotic.driver.crawl.entity.PortalTask
import ai.platon.exotic.driver.crawl.scraper.ListenablePortalTask
import ai.platon.exotic.driver.crawl.scraper.TaskStatus
import ai.platon.exotic.services.persist.CrawlRuleRepository
import ai.platon.exotic.services.persist.PortalTaskRepository
import ai.platon.pulsar.common.stringify
import ai.platon.pulsar.common.urls.Urls
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Component
class CrawlTaskRunner(
    val crawlRuleRepository: CrawlRuleRepository,
    val portalTaskRepository: PortalTaskRepository,
    val scraper: ExoticCrawler
) {
    private val logger = LoggerFactory.getLogger(CrawlTaskRunner::class.java)

    fun loadUnfinishedTasks() {
        // portalTaskRepository.findAllByStatus("Running")
    }

    fun startCreatedCrawlRules() {
        val now = LocalDateTime.now()
        val rules = crawlRuleRepository.findAll()
            .filter { it.status == "Created" }
            .filter { it.startTime <= now }

        rules.forEach { rule -> startCrawl(rule) }
    }

    fun restartCrawlRulesNextRound() {
        val now = LocalDateTime.now()
        val rules = crawlRuleRepository.findAll()
            .filter { it.status == "Running" || it.status == "Finished" }
            .filter { it.lastCrawlTime + it.period <= now }

        rules.forEach { rule -> startCrawl(rule) }
    }

    fun startCrawl(rule: CrawlRule) {
        try {
            rule.status = "Running"
            rule.lastCrawlTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)
            rule.lastModifiedDate = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)

            val portalUrls = rule.portalUrls

            if (portalUrls.isBlank()) {
                rule.status = "Finished"
                logger.info("No portal urls in rule #{}", rule.id)
                return
            }

            val maxPages = if (IS_DEVELOPMENT) 2 else rule.maxPages
            val pagedPortalUrls = portalUrls.split("\n")
                .filter { Urls.isValidUrl(it) }
                .flatMap { url -> IntRange(1, maxPages).map { pg -> "$url&page=$pg" } }
            if (pagedPortalUrls.isEmpty()) {
                logger.info("No portal urls in rule #{}", rule.id)
            }

            val portalTasks = pagedPortalUrls.map {
                PortalTask(it, "-refresh", 3).also {
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

    fun loadAndSubmitPortalTasks(limit: Int) {
        val order = Sort.Order.asc("id")
        val pageRequest = PageRequest.of(0, limit, Sort.by(order))
        val portalTasks = portalTaskRepository.findAllByStatus(TaskStatus.CREATED, pageRequest)
        if (portalTasks.isEmpty) {
            return
        }

        portalTasks.forEach {
            it.startTime = LocalDateTime.now()
            it.status = TaskStatus.LOADED
        }
        portalTaskRepository.saveAll(portalTasks)

        portalTasks.shuffled()
            .asSequence()
            .map { createListenablePortalTask(it, true) }
            .forEach { task -> scraper.scrapeOutPages(task) }
    }

    fun createListenablePortalTask(portalTask: PortalTask, refresh: Boolean = false): ListenablePortalTask {
        return ListenablePortalTask(
            portalTask, refresh = refresh,

            onSubmitted = {
                val rule = portalTask.rule
                if (rule != null) {
//                    rule.crawlHistory += ","
//                    rule.crawlHistory += LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).toString()
//                    while (rule.crawlHistory.length > 1024) {
//                        rule.crawlHistory = rule.crawlHistory
//                            .split(",").drop(5).joinToString(",")
//                    }
                    // crawlRuleRepository.save(rule)
                }

                it.status = TaskStatus.SUBMITTED

                portalTask.serverTaskId = it.serverTaskId
                portalTask.status = TaskStatus.SUBMITTED
                portalTaskRepository.save(portalTask)
            },
            onRetry = {
                it.status = TaskStatus.RETRYING

                portalTask.status = TaskStatus.RETRYING
                portalTaskRepository.save(portalTask)
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
            },
            onItemSuccess = {
                it.status = TaskStatus.OK

                portalTask.status = TaskStatus.OK
                ++portalTask.successCount
                ++portalTask.finishedCount
                portalTaskRepository.save(portalTask)
            },
            onItemFailed = {
                it.status = TaskStatus.FAILED

                portalTask.status = TaskStatus.FAILED
                ++portalTask.failedCount
                ++portalTask.finishedCount
                portalTaskRepository.save(portalTask)
            },
            onItemFinished = {

            },
            onItemTimeout = {

            },
        )
    }
}
