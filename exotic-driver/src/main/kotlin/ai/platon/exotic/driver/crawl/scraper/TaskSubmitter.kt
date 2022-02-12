package ai.platon.exotic.driver.crawl.scraper

import ai.platon.pulsar.common.chrono.scheduleAtFixedRate
import ai.platon.pulsar.driver.Driver
import ai.platon.pulsar.driver.DriverSettings
import ai.platon.pulsar.driver.ScrapeException
import ai.platon.pulsar.driver.ScrapeResponse
import ai.platon.pulsar.driver.utils.SQLTemplate
import com.google.gson.Gson
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentSkipListMap

open class TaskSubmitter(
    private val driverSettings: DriverSettings,
    private val autoCollect: Boolean = true,
) {
    var logger: Logger = LoggerFactory.getLogger(TaskSubmitter::class.java)
    var driver = Driver(driverSettings)

    private val pendingTasks: MutableMap<String, ListenableScrapeTask> = ConcurrentSkipListMap()

    val pendingPortalTaskCount get() = pendingTasks.count { it.value.task.isPortal }
    val pendingTaskCount get() = pendingTasks.size

    private val collectTimer = Timer()
    var collectTimerDelay = Duration.ofSeconds(15)
    var collectTimerPeriod = Duration.ofSeconds(15)

    var taskCount = 0
        private set
    var finishedTaskCount = 0
        private set
    var successTaskCount = 0
        private set
    var failedTaskCount = 0
        private set
    var retryTaskCount = 0
        private set

    init {
        if (autoCollect) {
            startCollectTimer()
        }
    }

    fun scrape(task: ListenableScrapeTask): ListenableScrapeTask {
        logger.info("Scraping 1/{} task | {} {}", pendingTasks.size, task.task.url, task.task.args)
        return submit(task)
    }

    fun scrapeAll(tasks: List<ListenableScrapeTask>): List<ListenableScrapeTask> {
        if (tasks.isEmpty()) {
            return listOf()
        }

        logger.info("Scraping {}/{} tasks", tasks.size, pendingTasks.size)

        submitAll(tasks)

        return tasks
    }

    private fun submit(listenableTask: ListenableScrapeTask): ListenableScrapeTask {
        ++taskCount

        val task = listenableTask.task
        val configuredUrl = task.url.trim() + " " + task.args.trim()
        val sql = SQLTemplate(task.sqlTemplate).createSQL(configuredUrl)
        try {
            val id = driver.submit(sql, task.priority, false)
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
        if (pendingTasks.isEmpty()) {
            return listOf()
        }

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

        var fc = 0
        var rc = 0
        val startTime = Instant.now()

        val checkingIds = checkingTasks.map { it.task.serverTaskId }
        val responses = driver.findAllByIds(checkingIds)
        checkingTasks.forEach { it.task.lastCheckTime = Instant.now() }

        responses.forEach { response ->
            val task = pendingTasks[response.id]
            if (task != null) {
                task.task.response = response
                when {
                    response.isDone -> {
                        ++fc
                        ++finishedTaskCount

                        if (response.statusCode == 200) {
                            task.onSuccess()
                        } else {
                            ++failedTaskCount
                            task.onFailed()
                        }
                    }
                    response.statusCode == 1601 -> {
                        ++rc
                        ++retryTaskCount
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

        val nextCheckTime = responses.filter { !it.isDone }.minOfOrNull { it.estimatedWaitTime } ?: collectTimerPeriod.seconds
        val elapsedTime = Duration.between(startTime, Instant.now())
        logger.info(
            "Collected {}/{}/{}/{}/{} responses in {}, next check: {}s",
            fc, rc, responses.size, checkingIds.size, pendingTasks.size, elapsedTime, nextCheckTime
        )

        return responses
    }

    private fun startCollectTimer() {
        collectTimer.scheduleAtFixedRate(collectTimerDelay, collectTimerPeriod) { collectTasks() }
    }
}
