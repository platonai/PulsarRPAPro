package ai.platon.exotic.driver.crawl.scraper

import ai.platon.pulsar.common.DateTimes
import ai.platon.pulsar.common.ResourceStatus
import ai.platon.pulsar.common.chrono.scheduleAtFixedRate
import ai.platon.pulsar.driver.Driver
import ai.platon.pulsar.driver.DriverSettings
import ai.platon.pulsar.driver.ScrapeException
import ai.platon.pulsar.driver.ScrapeResponse
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
import java.util.concurrent.ConcurrentSkipListSet
import kotlin.random.Random

open class TaskSubmitter(
    private val driverSettings: DriverSettings,
    private val autoCollect: Boolean = true,
): AutoCloseable {
    var logger: Logger = LoggerFactory.getLogger(TaskSubmitter::class.java)
    var dryRun = System.getProperty("scrape.submitter.dry.run") == "true"
    var driver = Driver(driverSettings)

    private val pendingTasks: MutableMap<String, ListenableScrapeTask> = ConcurrentSkipListMap()
    private val retryingTaskIds: ConcurrentSkipListSet<String> = ConcurrentSkipListSet()

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
            scrapeId, pendingTasks.size, totalTaskCount, task.task.url, task.task.args)
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

    /**
     * About API control parameters and status code:
     * 1. 404 NotFound -> 检测到 Sorry! We couldn't find that page 或者类似信息
     * 2. 412 PreconditionFailed -> 国家不对，地域不对，语言不对，其他前置条件不对
     * 3. 1601 Retry -> 系统正在准备下一轮重试，最大重试次数为 3
     * 4. 410 Gone -> 系统重试 3 次均失败，应用层自行判断是否需要强制重新采集。
     *    在出现 410 Gone 后：
     *    0. 加 -i 0s 告诉系统网页已经过期，未过期的均不采集
     *    1. 如果不加其他参数，应用层每发送一次请求，强制重新采集一次，retry 计数 +1，内部重试机制不生效
     *    2. 如果 pageStatusCode 为 1601 Retry 之外的其他错误，默认不采集，需要加 -ignoreFailure 强制忽略错误
     *    3. 如果需要激活内部重试机制，加参数 -refresh, -refresh 清除 retry 计数并强制忽略错误，-refresh = -expires 0s -ignoreFailure
     * */
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
        checkingTasks.forEach { it.task.lastCheckTime = startTime }

        responses.forEach { response ->
            val task = pendingTasks[response.id]

            if (task != null) {
                ++task.task.collectedCount
                task.task.collectedTime = startTime
                task.task.response = response

                val responseId = response.id ?: ""
                val isRetrying = response.pageStatusCode == 1601
                if (isRetrying) {
                    if (!retryingTaskIds.contains(responseId)) {
                        retryingTaskIds.add(responseId)

                        ++localRetryCount
                        ++totalRetryTaskCount
                        task.onRetry()
                    }
                } else {
                    retryingTaskIds.remove(response.id)
                }

                if (!isRetrying) {
                    when {
                        response.isDone -> {
                            handleTaskDone(task)
                            if (task.task.response.pageStatusCode != ResourceStatus.SC_OK) {
                                ++localFailedCount
                            }
                        }
                    }
                }
            } else {
                response.resultSet = null
                logger.warn("Unexpected response {}\n{}", response.id, Gson().toJson(response))
            }
        }

        // we can only handle retries at the server side in pulsar-1.9.9
        val roundFinishedTasks = checkingTasks.filter { isCompleted(it.task.response) }
        roundFinishedTasks.forEach { pendingTasks.remove(it.task.serverTaskId) }

        val roundTimeoutTasks = pendingTasks.filter { it.value.task.isTimeout }
        if (roundTimeoutTasks.isNotEmpty()) {
            val timeoutRetryTaskCount = roundTimeoutTasks.count { it.value.task.response.statusCode == 1601 }
            logger.info("Removing {}/{} timeout tasks | (retry/all)", timeoutRetryTaskCount, roundTimeoutTasks.size)
            roundTimeoutTasks.forEach { pendingTasks.remove(it.key) }
        }

        val estimatedTime = responses.filter { !isCompleted(it) }.minOfOrNull { it.estimatedWaitTime }?.takeIf { it > 0 } ?: 0
        val nextCheckTime = estimatedTime.coerceAtLeast(collectTimerPeriod.seconds)
        val elapsedTime = Duration.between(startTime, Instant.now())
        val rand = Random.nextInt(10)
        val description = if (rand == 0) " | (failed/retry/responses/checking/pending total failed/success/retry/finished)" else ""
        logger.info(
            "{}.\tCollected {}/{}/{}/{}/{} responses in {}, total {}/{}/{}/{}, recheck after {}s$description",
            collectId,
            localFailedCount, localRetryCount, responses.size, checkingIds.size, pendingTasks.size,
            DateTimes.readableDuration(elapsedTime, ChronoUnit.MILLIS),
            totalFailedTaskCount, totalSuccessTaskCount, totalRetryTaskCount, totalFinishedTaskCount,
            nextCheckTime
        )

        return responses
    }

    private fun handleTaskDone(task: ListenableScrapeTask) {
        val response = task.task.response
        retryingTaskIds.remove(response.id)

        ++totalFinishedTaskCount

        if (response.statusCode == ResourceStatus.SC_OK) {
            ++totalSuccessTaskCount
            task.onSuccess()
        } else {
            ++totalFailedTaskCount
            task.onFailed()
        }

        task.onFinished()
    }

    // TODO: the server side may not mark is done when pageStatusCode as 1601 any more
    private fun isCompleted(response: ScrapeResponse): Boolean {
        return response.isDone && response.pageStatusCode != 1601
    }

    private fun startCollectTimer() {
        collectTimer.scheduleAtFixedRate(collectTimerDelay, collectTimerPeriod) { collectTasks() }
    }
}
