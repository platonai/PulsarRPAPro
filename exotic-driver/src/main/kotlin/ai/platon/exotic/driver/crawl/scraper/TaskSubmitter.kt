package ai.platon.exotic.driver.crawl.scraper

import ai.platon.pulsar.common.DateTimes
import ai.platon.pulsar.common.chrono.scheduleAtFixedRate
import ai.platon.pulsar.common.readable
import ai.platon.pulsar.driver.*
import ai.platon.pulsar.driver.utils.SQLTemplate
import com.google.gson.Gson
import org.apache.commons.lang3.RandomStringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.ConcurrentSkipListMap
import kotlin.random.Random

open class TaskSubmitter(
    private val driverSettings: DriverSettings,
    private val autoCollect: Boolean = true,
): AutoCloseable {
    var logger: Logger = LoggerFactory.getLogger(TaskSubmitter::class.java)
    var dryRun = false
    var driver = Driver(driverSettings)

    private val pendingTasks: MutableMap<String, ListenableScrapeTask> = ConcurrentSkipListMap()

    var scrapeId = 0
        private set
    var collectId = 0
        private set

    val pendingPortalTaskCount get() = pendingTasks.count { it.value.task.isPortal }
    val pendingTaskCount get() = pendingTasks.size

    var totalTaskCount = 0
        private set
    var totalFinishedTaskCount = 0
        private set
    var totalSuccessTaskCount = 0
        private set
    var totalFailedTaskCount = 0
        private set
    var totalRetryTaskCount = 0
        private set

    private val collectTimer = Timer()
    var collectTimerDelay = Duration.ofSeconds(15)
    var collectTimerPeriod = Duration.ofSeconds(15)

    init {
        if (autoCollect) {
            startCollectTimer()
        }
    }

    fun scrape(task: ListenableScrapeTask): ListenableScrapeTask {
        ++scrapeId
        logger.info("{}.\tScraping 1/{}/{} task | {} {}",
            scrapeId, pendingTasks.size, task.task.url, task.task.args, totalTaskCount)
        return submit(task)
    }

    fun scrapeAll(tasks: List<ListenableScrapeTask>): List<ListenableScrapeTask> {
        if (tasks.isEmpty()) {
            return listOf()
        }

        ++scrapeId
        logger.info("{}.\tScraping {}/{}/{} tasks", scrapeId, tasks.size, pendingTasks.size, totalTaskCount)

        submitAll(tasks)

        return tasks
    }

    override fun close() {
        collectTimer.cancel()
        driver.close()
    }

    private fun submit(listenableTask: ListenableScrapeTask): ListenableScrapeTask {
        ++totalTaskCount

        val task = listenableTask.task
        ++task.submitCount

        val configuredUrl = task.url.trim() + " " + task.args.trim()
        val sql = SQLTemplate(task.sqlTemplate).createSQL(configuredUrl)
        try {
            val id = if (dryRun) {
                "mock." + RandomStringUtils.randomAlphanumeric(10)
            } else {
                driver.submit(sql, task.priority, false)
            }
            task.serverTaskId = id

            listenableTask.onSubmitted()
            pendingTasks[id] = listenableTask
        } catch (e: ScrapeException) {
            task.exception = e
            task.exceptionMessage = e.toString()
            logger.warn("Scrape failed, {}\n{}", e.message, sql)
        }

        return listenableTask
    }

    private fun submitAll(listenableTasks: List<ListenableScrapeTask>): List<ListenableScrapeTask> {
        return listenableTasks.map { submit(it) }
    }

    @Throws(InterruptedException::class)
    private fun collectTasks(): List<ScrapeResponse> {
        if (dryRun) {
            return listOf()
        }

        if (pendingTasks.isEmpty()) {
            return listOf()
        }

        ++collectId

        val checkBatchSize = 30
        val checkingTasks = pendingTasks.values
            .filter { it.task.shouldCheck }
            .sortedByDescending { it.task.lastCheckTime }
            .take(checkBatchSize)
        if (checkingTasks.isEmpty()) {
            val estimatedWaitTime = pendingTasks.values.minOfOrNull { it.task.response.estimatedWaitTime } ?: -1
            logger.info("No task to check, next task to wait: {}", estimatedWaitTime)
            return listOf()
        }

        var localFailedCount = 0
        var localRetryCount = 0
        val startTime = Instant.now()

        val checkingIds = checkingTasks.map { it.task.serverTaskId }
        val responses = kotlin.runCatching { driver.findAllByIds(checkingIds) }
            .onFailure { logger.warn(it.message) }
            .getOrNull() ?: listOf()
        checkingTasks.forEach { it.task.lastCheckTime = Instant.now() }

        responses.forEach { response ->
            val task = pendingTasks[response.id]
            if (task != null) {
                ++task.task.collectedCount
                task.task.collectedTime = Instant.now()
                task.task.response = response

                when {
                    response.isDone -> {
                        ++localFailedCount
                        ++totalFinishedTaskCount

                        if (response.statusCode == 200) {
                            ++totalSuccessTaskCount
                            task.onSuccess()
                        } else {
                            ++totalFailedTaskCount
                            task.onFailed()
                        }
                        task.onFinished()
                    }
                    response.statusCode == 1601 -> {
                        ++localRetryCount
                        ++totalRetryTaskCount
                        task.onRetry()
                    }
                }
            } else {
                response.resultSet = null
                logger.warn("Unexpected response {}\n{}", response.id, Gson().toJson(response))
            }
        }

        val roundFinishedTasks = checkingTasks.filter { it.task.response.isDone }
        roundFinishedTasks.forEach { pendingTasks.remove(it.task.serverTaskId) }

        val roundTimeoutTasks = pendingTasks.filter { it.value.task.isTimeout }
        if (roundTimeoutTasks.isNotEmpty()) {
            logger.info("Removing {} timeout tasks", roundTimeoutTasks.size)
            roundTimeoutTasks.forEach { pendingTasks.remove(it.key) }
        }

        val estimatedTime = responses.filter { !it.isDone }.minOfOrNull { it.estimatedWaitTime }?.takeIf { it > 0 } ?: 0
        val nextCheckTime = estimatedTime.coerceAtLeast(collectTimerPeriod.seconds)
        val elapsedTime = Duration.between(startTime, Instant.now())
        val rand = Random.nextInt(10)
        val description = if (rand == 0) " | (failed/retry/responses/checking/pending/finished)" else ""
        logger.info(
            "{}.\tCollected {}/{}/{}/{}/{}/{} responses in {}, recheck after {}s$description",
            collectId,
            localFailedCount, localRetryCount, responses.size, checkingIds.size, pendingTasks.size, totalFinishedTaskCount,
            DateTimes.readableDuration(elapsedTime, ChronoUnit.MILLIS), nextCheckTime
        )

        return responses
    }

    private fun startCollectTimer() {
        collectTimer.scheduleAtFixedRate(collectTimerDelay, collectTimerPeriod) { collectTasks() }
    }
}
