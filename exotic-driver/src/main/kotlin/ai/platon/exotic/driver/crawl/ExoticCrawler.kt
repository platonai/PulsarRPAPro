package ai.platon.exotic.driver.crawl

import ai.platon.exotic.driver.common.IS_DEVELOPMENT
import ai.platon.exotic.driver.crawl.entity.ItemDetail
import ai.platon.exotic.driver.crawl.scraper.ListenablePortalTask
import ai.platon.exotic.driver.crawl.scraper.ListenableScrapeTask
import ai.platon.exotic.driver.crawl.scraper.OutPageScraper
import ai.platon.exotic.driver.crawl.scraper.ScrapeTask
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.driver.DriverSettings
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import java.util.*
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.ConcurrentLinkedQueue

class ExoticCrawler(val env: Environment? = null): AutoCloseable {
    private val logger = LoggerFactory.getLogger(ExoticCrawler::class.java)

    val scrapeServer: String
        get() = env?.getProperty("scrape.server")
            ?: System.getProperty("scrape.server")
            ?: "localhost"
    val scrapeServerPort: Int
        get() = env?.getProperty("scrape.server.port")?.toIntOrNull()
            ?: System.getProperty("scrape.server.port")?.toIntOrNull()
            ?: 8182
    val scrapeServerContextPath: String
        get() = env?.getProperty("scrape.server.servlet.context-path")
            ?: System.getProperty("scrape.server.servlet.context-path")
            ?: "/"
    val authToken: String
        get() = env?.getProperty("scrape.authToken")
            ?: System.getProperty("scrape.authToken")
            ?: "b06test42c13cb000f74539b20be9550b8a1a90b9"

    val driverSettings get() = DriverSettings(
        scrapeServer,
        authToken,
        scrapeServerPort,
        scrapeServerContextPath
    )

    val outPageScraper = OutPageScraper(driverSettings)

    val driver get() = outPageScraper.taskSubmitter.driver

    val pendingPortalTasks: Deque<ListenablePortalTask> = ConcurrentLinkedDeque()

    val pendingItems = ConcurrentLinkedQueue<ItemDetail>()

    var maxPendingTaskCount = if (IS_DEVELOPMENT) 10 else 50

    init {
        Params.of(
            "scrapeServer", scrapeServer,
            "scrapeServerPort", scrapeServerPort,
            "scrapeServerContextPath", scrapeServerContextPath
        ).withLogger(logger).debug()
    }

    fun crawl() {
        val taskSubmitter = outPageScraper.taskSubmitter
        val submittedTaskCount = taskSubmitter.pendingTaskCount

        if (submittedTaskCount >= maxPendingTaskCount) {
            return
        }

        val n = (maxPendingTaskCount - submittedTaskCount).coerceAtMost(10)
        if (pendingPortalTasks.isNotEmpty()) {
            scrapeFromQueue(pendingPortalTasks, n)
        }
    }

    @Throws(Exception::class)
    fun scrape(task: ListenableScrapeTask) {
        try {
            outPageScraper.scrape(task)
        } catch (t: Throwable) {
            logger.warn("Unexpected exception", t)
        }
    }

    @Throws(Exception::class)
    fun scrapeOutPages(task: ListenablePortalTask) {
        try {
            outPageScraper.scrape(task)
        } catch (t: Throwable) {
            logger.warn("Unexpected exception", t)
        }
    }

    override fun close() {
        outPageScraper.close()
    }

    private fun scrapeFromQueue(queue: Queue<ListenablePortalTask>, n: Int) {
        var n0 = n
        while (n0-- > 0) {
            val task = queue.poll()
            if (task != null) {
                scrapeOutPages(task)
            }
        }
    }

    private fun createPendingItems(task: ScrapeTask) {
        val allowDuplicate = task.companionPortalTask?.rule != null
        task.response.resultSet
            ?.filter { it.isNotEmpty() }
            ?.map { ItemDetail.create(it["uri"].toString(), it, allowDuplicate) }
            ?.toCollection(pendingItems)
    }
}

fun main() {
    val scraper = ExoticCrawler()
    scraper.crawl()

    readlnOrNull()
}
