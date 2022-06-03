package ai.platon.exotic.services.api.controller.web

import ai.platon.exotic.driver.crawl.ExoticCrawler
import ai.platon.exotic.services.api.entity.api.ExpandedScrapeResponse
import ai.platon.exotic.services.common.jackson.scentObjectMapper
import com.google.gson.GsonBuilder
import org.bson.types.ObjectId
import org.springframework.data.domain.PageRequest
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
@RequestMapping("crawl/remote/tasks")
class RemoteTaskWebController(
    private val exoticCrawler: ExoticCrawler,
) {
    private val driver get() = exoticCrawler.driver

    @GetMapping("/")
    fun list(
        @RequestParam(defaultValue = "0") pageNumber: Int = 0,
        @RequestParam(defaultValue = "500") pageSize: Int = 500,
        @RequestParam(defaultValue = "desc") direction: String = "desc",
        model: Model
    ): String {
        val ascPageNumber = if (direction == "desc") {
            val count = driver.count()
            val totalPageNumber = 1 + count / pageSize
            totalPageNumber - pageNumber - 1
        } else pageNumber

        val pageable = PageRequest.of(ascPageNumber.toInt(), pageSize)
        val tasks: List<ExpandedScrapeResponse> = driver.fetch(pageable.offset, pageable.pageSize)
            .map { ExpandedScrapeResponse(it) }
            .sortedByDescending { it.timestamp }
        model.addAttribute("tasks", tasks)
        return "crawl/remote/tasks/index"
    }

    @GetMapping(
        "/download",
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun download(
        @RequestParam(defaultValue = "0") pageNumber: Int = 0,
        @RequestParam(defaultValue = "500") pageSize: Int = 500,
        @RequestParam(defaultValue = "desc") direction: String = "desc",
    ): String {
        val ascPageNumber = if (direction == "desc") {
            val count = driver.count()
            val totalPageNumber = 1 + count / pageSize
            totalPageNumber - pageNumber - 1
        } else pageNumber

        val pageable = PageRequest.of(ascPageNumber.toInt(), pageSize)
        val tasks: List<ExpandedScrapeResponse> = driver.fetch(pageable.offset, pageable.pageSize)
            .map { ExpandedScrapeResponse(it) }
            .sortedByDescending { it.timestamp }

        return scentObjectMapper().writeValueAsString(tasks)
    }

    @GetMapping("/view/{id}")
    fun view(@PathVariable id: String, model: Model): String {
        val task = driver.findById(id)
        model.addAttribute("task", task)
        val gson = GsonBuilder().setPrettyPrinting().create()
        model.addAttribute("json", gson.toJson(task))
        return "crawl/remote/tasks/view"
    }
}
