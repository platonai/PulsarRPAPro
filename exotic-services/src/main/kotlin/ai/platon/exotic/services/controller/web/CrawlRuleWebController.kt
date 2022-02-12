package ai.platon.exotic.services.controller.web

import ai.platon.exotic.driver.crawl.entity.CrawlRule
import ai.platon.exotic.services.jackson.prettyScentObjectWritter
import ai.platon.exotic.services.component.CrawlTaskRunner
import ai.platon.exotic.services.persist.CrawlRuleRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.*
import javax.validation.Valid

@Controller
@RequestMapping("crawl/rules")
class CrawlRuleWebController(
    private val repository: CrawlRuleRepository,
    private val crawlTaskRunner: CrawlTaskRunner,
) {
    @GetMapping("/")
    fun list(
        @RequestParam(defaultValue = "0") pageNumber: Int = 0,
        @RequestParam(defaultValue = "500") pageSize: Int = 500,
        model: Model
    ): String {
        val sort = Sort.Direction.DESC
        val sortProperty = "id"
        val pageable = PageRequest.of(pageNumber, pageSize, sort, sortProperty)
        val rules = repository.findAll(pageable)
        model.addAttribute("rules", rules)
        return "rules/index"
    }

    @GetMapping("/view/{id}")
    fun view(@PathVariable id: Long, model: Model): String {
        model.addAttribute("rule", repository.getById(id))
        return "rules/view"
    }

    @GetMapping("/add")
    fun showAddForm(model: Model): String {
        val rule = CrawlRule()
        rule.sqlTemplate = """
select
    dom_base_uri(dom) as `url`,
    dom_first_text(dom, '#productTitle') as `title`
from load_and_select('{{url}}', ':root');
        """.trimIndent()
        rule.portalUrls = """
https://channel1.example.com/
https://channel2.example.com/
        """.trimIndent()

        model.addAttribute("rule", rule)

        return "rules/add"
    }

    @GetMapping("/jd/add")
    fun showJdAddForm(model: Model): String {
        model.addAttribute("rule", CrawlRule())
        return "rules/jd/add"
    }

    @PostMapping("/add")
    fun add(rule: CrawlRule, result: BindingResult, model: Model): String {
        if (result.hasErrors()) {
            return "rules/add"
        }

        println(prettyScentObjectWritter().writeValueAsString(rule))

        rule.adjustFields()
        repository.save(rule)
        return "redirect:/crawl/rules/"
    }

    @PostMapping("/add3")
    fun add3(rule: CrawlRule, result: BindingResult, model: Model): String {
        if (result.hasErrors()) {
            return "rules/add3"
        }

        rule.adjustFields()
        repository.save(rule)
        return "redirect:/crawl/rules/"
    }

    @GetMapping("/edit/{id}")
    fun edit(@PathVariable("id") id: Long, model: Model): String {
        val rule = repository.findById(id).orElseThrow { IllegalArgumentException("Invalid rule Id: $id") }
        model.addAttribute("rule", rule)
        return "rules/edit"
    }

    @PostMapping("update/{id}")
    fun update(
        @PathVariable("id") id: Long, @Valid rule: CrawlRule, result: BindingResult,
        model: Model
    ): String? {
        if (result.hasErrors()) {
            rule.id = id
            return "rules/edit"
        }

        val fullRule = repository.findById(id).orElseThrow { IllegalArgumentException("Invalid rule Id: $id") }
        fullRule.name = rule.name
        fullRule.label = rule.label
        fullRule.period = rule.period
        fullRule.maxPages = rule.maxPages
        fullRule.description = rule.description

        fullRule.outLinkSelector = rule.outLinkSelector
        fullRule.sqlTemplate = rule.sqlTemplate

        fullRule.adjustFields()

        repository.save(fullRule)

        return "redirect:/crawl/rules/view/$id"
    }

    @GetMapping("pause/{id}")
    fun pause(@PathVariable("id") id: Long, model: Model): String {
        val rule = repository.findById(id).orElseThrow { IllegalArgumentException("Invalid rule Id: $id") }

        rule.status = "Paused"
        rule.adjustFields()
        repository.save(rule)

        return "redirect:/crawl/rules/"
    }

    @GetMapping("start/{id}")
    fun start(@PathVariable("id") id: Long, model: Model): String {
        val rule = repository.findById(id).orElseThrow { IllegalArgumentException("Invalid rule Id: $id") }

        rule.status = "Created"
        rule.adjustFields()
        repository.save(rule)

        crawlTaskRunner.startCrawl(rule)

        return "redirect:/crawl/rules/"
    }
}
