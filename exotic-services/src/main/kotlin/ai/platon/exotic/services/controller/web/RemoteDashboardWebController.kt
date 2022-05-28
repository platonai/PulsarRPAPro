package ai.platon.exotic.services.controller.web

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("crawl/remote/dashboard")
class RemoteDashboardWebController {
    @GetMapping("/")
    fun home(): String {
        return "crawl/remote/tasks/dashboard"
    }
}
