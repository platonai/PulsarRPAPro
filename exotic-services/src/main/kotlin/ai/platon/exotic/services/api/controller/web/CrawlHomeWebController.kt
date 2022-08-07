package ai.platon.exotic.services.api.controller.web

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("crawl")
class CrawlHomeWebController {
    @GetMapping("/")
    fun home(): String {
        return "crawl/home"
    }
}
