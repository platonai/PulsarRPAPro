package ai.platon.exotic.services.api.integration

import ai.platon.exotic.services.common.jackson.prettyScentObjectWritter
import ai.platon.exotic.services.common.jackson.scentObjectMapper
import ai.platon.exotic.services.api.component.CrawlTaskRunner
import ai.platon.exotic.driver.crawl.entity.PortalTask
import ai.platon.exotic.driver.crawl.scraper.ListenablePortalTask
import ai.platon.exotic.services.api.entity.converters.IntegratedProductConverter
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import kotlin.test.assertEquals

@SpringBootTest
@Disabled
class CrawlTaskRunnerTests @Autowired constructor(
    private val crawlTaskRunner: CrawlTaskRunner
) {
    val scraper get() = crawlTaskRunner.scraper

    val driver get() = scraper.driver

    @Test
    fun testCrawlTaskRunner() {
        val portalUrls = """
            https://list.jd.com/list.html?cat=737,794,798
            https://list.jd.com/list.html?cat=737,17394,17395
        """.trimIndent().split("\n").map { it.trim() }
        portalUrls.flatMap { url -> IntRange(1, 30).map { pg -> "$url&page=$pg" } }
            .map { PortalTask(it).apply { priority = 3 } }
            .map { ListenablePortalTask.create(it).also { it.refresh = true } }
            .forEach { task ->
                scraper.pendingPortalTasks.addFirst(task)
            }

        println(portalUrls[0])
        println(scraper.pendingPortalTasks.last().task.url)

        assertEquals(60, scraper.pendingPortalTasks.size)
    }

    @Test
    fun testDashboard() {
        val dashboard = driver.dashboard()
        println(scentObjectMapper().writeValueAsString(dashboard))
    }

    @Test
    fun testFetch() {
        val response = driver.fetch(100, limit = 5)
        println(prettyScentObjectWritter().writeValueAsString(response))
    }

    @Test
    fun testDownload() {
        val lastFetchedPage = 1
        val response = driver.download(pageNumber = 1 + lastFetchedPage, pageSize = 5)
        println(prettyScentObjectWritter().writeValueAsString(response))

        val converter = IntegratedProductConverter()
        val products = response.content.mapNotNull { result ->
            result.resultSet?.filter { it.isNotEmpty() }?.map { converter.convert(it) }
        }

        if (products.isNotEmpty()) {
            println(prettyScentObjectWritter().writeValueAsString(products[0]))
        }

        // persist the entities
    }

    @Test
    fun testGetFinishedTasks() {
        val response = driver.download(pageNumber = 1, pageSize = 5)
        println(prettyScentObjectWritter().writeValueAsString(response))
    }

    @Test
    fun testGetPendingTasks() {
        val response = driver.download(pageNumber = 1, pageSize = 5)
        println(prettyScentObjectWritter().writeValueAsString(response))
    }

    @Test
    fun testGetTasksByRuleId() {
        val response = driver.download(pageNumber = 1, pageSize = 5)
        println(prettyScentObjectWritter().writeValueAsString(response))
    }
}
