package ai.platon.exotic.services.api.controller.web

import ai.platon.exotic.driver.crawl.ExoticCrawler
import com.google.gson.GsonBuilder
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("crawl/remote/dashboard")
class RemoteDashboardWebController(
    private val exoticCrawler: ExoticCrawler,
) {
    private val driver get() = exoticCrawler.driver

    @GetMapping
    fun dashboard(model: Model): String {
        val gson = GsonBuilder().setPrettyPrinting().create()
        val dashboard = driver.dashboard()
        model.addAttribute("dashboard", gson.toJson(dashboard))
        return "crawl/remote/tasks/dashboard"
    }
}
