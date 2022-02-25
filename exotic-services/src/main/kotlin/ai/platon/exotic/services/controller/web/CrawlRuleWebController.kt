package ai.platon.exotic.services.controller.web

import ai.platon.exotic.driver.crawl.entity.CrawlRule
import ai.platon.exotic.driver.crawl.scraper.RuleStatus
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
        val rules = repository.findAllByStatusNot(RuleStatus.Archived.toString(), pageable)
        model.addAttribute("rules", rules)
        return "crawl/rules/index"
    }

    @GetMapping("/view/{id}")
    fun view(@PathVariable id: Long, model: Model): String {
        val rule = repository.getById(id)

        model.addAttribute("rule", rule)
        val tasks = rule.portalTasks.sortedByDescending { it.id }
        model.addAttribute("tasks", tasks)

        return "crawl/rules/view"
    }

    @GetMapping("/add")
    fun showAddForm(model: Model): String {
        val rule = CrawlRule()
        rule.sqlTemplate = """
select
    dom_base_uri(dom) as `url`,
    dom_first_text(dom, '#productTitle') as `title`,
    dom_first_text(dom, '#bylineInfo') as `brand`,
    dom_first_text(dom, '#price') as `price`
from load_and_select('{{url}}', ':root');
        """.trimIndent()

        rule.portalUrls = """
https://www.amazon.com/Best-Sellers-Beauty-Personal-Care/zgbs/beauty
https://www.amazon.com/Best-Sellers-Electronics/zgbs/electronics
        """.trimIndent()
        rule.outLinkSelector = "a[href~=/dp/]"
        rule.nextPageSelector = "ul.a-pagination li.a-last a"

        model.addAttribute("rule", rule)

        return "crawl/rules/add"
    }

    @GetMapping("/jd/add")
    fun showJdAddForm(model: Model): String {
        model.addAttribute("rule", CrawlRule())
        return "crawl/rules/jd/add"
    }

    @PostMapping("/add")
    fun add(@Valid @ModelAttribute("rule") rule: CrawlRule, result: BindingResult, model: Model): String {
        println(prettyScentObjectWritter().writeValueAsString(rule))

        if (result.hasErrors()) {
            // model.addAttribute("rule", rule)
            return "crawl/rules/add"
        }

        rule.adjustFields()
        repository.save(rule)
        return "redirect:/crawl/rules/"
    }

    @GetMapping("/edit/{id}")
    fun edit(@PathVariable("id") id: Long, model: Model): String {
        val rule = repository.findById(id).orElseThrow { IllegalArgumentException("Invalid rule Id: $id") }
        model.addAttribute("rule", rule)
        return "crawl/rules/edit"
    }

    @PostMapping("update/{id}")
    fun update(
        @PathVariable("id") id: Long, @Valid rule: CrawlRule, result: BindingResult,
        model: Model
    ): String? {
        if (result.hasErrors()) {
            return "crawl/rules/edit"
        }

        rule.adjustFields()

        repository.save(rule)

        return "redirect:/crawl/rules/view/$id"
    }

    @GetMapping("pause/{id}")
    fun pause(@PathVariable("id") id: Long, model: Model): String {
        val rule = repository.findById(id).orElseThrow { IllegalArgumentException("Invalid rule Id: $id") }

        rule.status = RuleStatus.Paused.toString()
        rule.adjustFields()
        repository.save(rule)

        return "redirect:/crawl/rules/"
    }

    @GetMapping("start/{id}")
    fun start(@PathVariable("id") id: Long, model: Model): String {
        val rule = repository.findById(id).orElseThrow { IllegalArgumentException("Invalid rule Id: $id") }

        rule.status = RuleStatus.Created.toString()
        rule.adjustFields()
        repository.save(rule)

        crawlTaskRunner.startCrawl(rule)

        return "redirect:/crawl/rules/"
    }

    @GetMapping("archive/{id}")
    fun archive(@PathVariable("id") id: Long, model: Model): String {
        val rule = repository.findById(id).orElseThrow { IllegalArgumentException("Invalid rule Id: $id") }

        rule.status = RuleStatus.Archived.toString()
        rule.adjustFields()
        repository.save(rule)

        crawlTaskRunner.startCrawl(rule)

        return "redirect:/crawl/rules/"
    }
}
