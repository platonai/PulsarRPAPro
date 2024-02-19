package ai.platon.exotic.services.api.controller.web

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody

@Controller
@RequestMapping("/")
class HomeWebController {
    @RequestMapping(value = ["/robots.txt", "/robot.txt"])
    @ResponseBody
    fun getRobotsTxt(): String {
        return """
        User-agent: *
        Disallow: /admin
        
        """.trimIndent()
    }
}
