package ai.platon.exotic.services.api.controller.web

import ai.platon.exotic.driver.crawl.ExoticCrawler
import com.google.gson.GsonBuilder
import org.springframework.data.domain.PageRequest
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
        model: Model
    ): String {
        val pageable = PageRequest.of(pageNumber, pageSize)
        val tasks = driver.fetch(pageable.offset, pageable.pageSize)
        model.addAttribute("tasks", tasks)
        return "crawl/remote/tasks/index"
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
