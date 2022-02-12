package ai.platon.exotic.services.controller.web

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.GetMapping
import java.security.Principal

@Controller
internal class HomeController {
    @ModelAttribute("module")
    fun module(): String {
        return "home"
    }

    @GetMapping("/")
    fun index(principal: Principal?): String {
        return if (principal != null) "home/homeSignedIn" else "home/homeNotSignedIn"
    }
}
