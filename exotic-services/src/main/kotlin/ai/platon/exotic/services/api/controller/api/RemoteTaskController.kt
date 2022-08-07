package ai.platon.exotic.services.api.controller.api

import ai.platon.exotic.driver.crawl.ExoticCrawler
import ai.platon.exotic.services.api.entity.api.ExpandedScrapeResponse
import ai.platon.pulsar.driver.ScrapeResponse
import org.springframework.data.domain.PageRequest
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/crawl/remote/tasks")
class RemoteTaskController(
    private val exoticCrawler: ExoticCrawler,
) {
    private val driver get() = exoticCrawler.driver

    @GetMapping("/")
    fun list(
        @RequestParam(defaultValue = "0") pageNumber: Int = 0,
        @RequestParam(defaultValue = "500") pageSize: Int = 500,
        @RequestParam(defaultValue = "desc") direction: String = "desc"
    ): List<ExpandedScrapeResponse> {
        val ascPageNumber = if (direction == "desc") {
            val count = driver.count()
            val totalPageNumber = 1 + count / pageSize
            totalPageNumber - pageNumber - 1
        } else pageNumber

        val pageable = PageRequest.of(ascPageNumber.toInt(), pageSize)
        return driver.fetch(pageable.offset, pageable.pageSize)
            .map { ExpandedScrapeResponse(it) }
            .sortedByDescending { it.timestamp }
    }

    @GetMapping("/view/{id}")
    fun view(@PathVariable id: String): ScrapeResponse {
        return driver.findById(id)
    }
}
