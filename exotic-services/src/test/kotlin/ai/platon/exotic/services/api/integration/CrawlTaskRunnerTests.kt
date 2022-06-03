package ai.platon.exotic.services.api.integration

import ai.platon.exotic.services.common.jackson.prettyScentObjectWritter
import ai.platon.exotic.services.common.jackson.scentObjectMapper
import ai.platon.exotic.services.api.component.CrawlTaskRunner
import ai.platon.exotic.driver.crawl.entity.PortalTask
import ai.platon.exotic.driver.crawl.scraper.ListenablePortalTask
import ai.platon.exotic.services.api.entity.converters.IntegratedProductConverter
import ai.platon.pulsar.driver.ScrapeResponse
import com.google.gson.Gson
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
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
        val mapper = prettyScentObjectWritter()
        val dashboard = driver.dashboard()
        println(mapper.writeValueAsString(dashboard))
    }

    @Test
    fun testFetch() {
        println(driver.statusApi)
        println(driver.count())

        val mapper = prettyScentObjectWritter()
        val response = driver.fetch(0, limit = 50)
        val taskIds = response.filter { it.statusCode == 200 }.mapNotNull { it.id }
//        println(mapper.writeValueAsString(response.filter { it.statusCode == 200 }))
        println(taskIds)

        taskIds.shuffled().take(10).forEach {
            val task = findById("62904827226f1a7f8bbf168e")
            println(mapper.writeValueAsString(task))
        }
    }

    /**
     * Find a scrape response by scrape task id which returned by [submit]
     * */
    fun findById(id: String): ScrapeResponse {
        val statusApi = driver.statusApi
        val authToken = scraper.authToken
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$statusApi?id=$id&authToken=$authToken&debug=100"))
            .GET().build()
        val httpClient = HttpClient.newHttpClient()
        val httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        println("Response body: ")
        println(httpResponse.body())
        return driver.createGson().fromJson(httpResponse.body(), ScrapeResponse::class.java)
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
