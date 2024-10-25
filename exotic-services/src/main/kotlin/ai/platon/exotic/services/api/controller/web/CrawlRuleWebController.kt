package ai.platon.exotic.services.api.controller.web

import ai.platon.exotic.driver.crawl.entity.CrawlRule
import ai.platon.exotic.driver.crawl.scraper.RuleStatus
import ai.platon.exotic.driver.crawl.scraper.TaskStatus
import ai.platon.exotic.services.api.component.CrawlTaskRunner
import ai.platon.exotic.services.api.persist.CrawlRuleRepository
import ai.platon.exotic.services.api.persist.PortalTaskRepository
import ai.platon.exotic.services.common.jackson.prettyScentObjectWritter
import ai.platon.pulsar.common.DateTimes
import ai.platon.pulsar.common.LinkExtractors
import ai.platon.pulsar.common.ResourceLoader
import ai.platon.pulsar.common.getLogger
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.*
import java.time.Duration
import java.time.Instant
import javax.validation.Valid
import kotlin.random.Random

@Controller
@RequestMapping("crawl/rules")
class CrawlRuleWebController(
    private val repository: CrawlRuleRepository,
    private val portalTaskRepository: PortalTaskRepository,
    private val crawlTaskRunner: CrawlTaskRunner,
) {
    private val amazonSeeds = LinkExtractors.fromResource("sites/amazon/best-sellers.txt")
    private val amazonItemSQLTemplate = ResourceLoader.readString("sites/amazon/sqls/x-item.sql").trim()

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
        rule.sqlTemplate = amazonItemSQLTemplate

        val n = 2 + Random.nextInt(4)
        rule.portalUrls = amazonSeeds.shuffled().take(n).joinToString("\n")
        rule.outLinkSelector = "a[href~=/dp/]"
        rule.nextPageSelector = "ul.a-pagination li.a-last a"

        model.addAttribute("rule", rule)

        return "crawl/rules/add"
    }

    @GetMapping("/jd/add")
    fun showJdAddForm(model: Model): String {
        val rule = CrawlRule()
        model.addAttribute("rule", rule)
        return "crawl/rules/jd/add"
    }

    @PostMapping("/add")
    fun add(@Valid @ModelAttribute("rule") rule: CrawlRule, result: BindingResult, model: Model): String {
        // getLogger(this).info(prettyScentObjectWritter().writeValueAsString(rule))

        if (result.hasErrors()) {
            // model.addAttribute("rule", rule)
            return "crawl/rules/add"
        }

        if (rule.period.toDays() >= 3650) {
            if (rule.deadTime == DateTimes.doomsday) {
                // Run only once, and set the dead time to 2 days later, so it has to be scheduled in 2 days
                rule.deadTime = rule.startTime + Duration.ofDays(2)
            }
        }
        
        rule.createdDate = Instant.now()
        rule.status = RuleStatus.Created.toString()

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

        val old = repository.findById(id).orElseThrow { IllegalArgumentException("Invalid rule Id: $id") }
        rule.status = old.status
        rule.crawlCount = old.crawlCount
        rule.createdDate = old.createdDate
        rule.lastCrawlTime = old.lastCrawlTime
        rule.crawlHistory = old.crawlHistory

        repository.save(rule)

        return "redirect:/crawl/rules/view/$id"
    }

    @GetMapping("pause/{id}")
    fun pause(@PathVariable("id") id: Long, model: Model): String {
        val rule = repository.findById(id).orElseThrow { IllegalArgumentException("Invalid rule Id: $id") }

        rule.status = RuleStatus.Paused.toString()
        repository.save(rule)
        val portalTasks = portalTaskRepository.findAllByRuleId(rule.id!!).toList()
        portalTasks
            .filter { it.status.value < TaskStatus.PAUSED.value }
            .forEach { it.status = TaskStatus.PAUSED }
        portalTaskRepository.saveAll(portalTasks)

        return "redirect:/crawl/rules/"
    }

    @GetMapping("start/{id}")
    fun start(@PathVariable("id") id: Long, model: Model): String {
        val rule = repository.findById(id).orElseThrow { IllegalArgumentException("Invalid rule Id: $id") }

        rule.status = RuleStatus.Created.toString()
//        rule.adjustFields()
        repository.save(rule)

        var portalTasks = portalTaskRepository.findAllByRuleId(rule.id!!).toList()
        portalTasks = portalTasks
            .filter { it.status == TaskStatus.PAUSED }
            .onEach { it.status = TaskStatus.CREATED }
        portalTaskRepository.saveAll(portalTasks)

        crawlTaskRunner.startCrawl(rule)

        return "redirect:/crawl/rules/"
    }

    @GetMapping("admin/")
    fun adminList(
        @RequestParam(defaultValue = "0") pageNumber: Int = 0,
        @RequestParam(defaultValue = "500") pageSize: Int = 500,
        model: Model
    ): String {
        val sort = Sort.Direction.DESC
        val sortProperty = "id"
        val pageable = PageRequest.of(pageNumber, pageSize, sort, sortProperty)
        val rules = repository.findAll(pageable)
        model.addAttribute("rules", rules)
        return "crawl/rules/admin/index"
    }

    @GetMapping("admin/archive/{id}")
    fun adminArchive(@PathVariable("id") id: Long, model: Model): String {
        val rule = repository.findById(id).orElseThrow { IllegalArgumentException("Invalid rule Id: $id") }

        rule.status = RuleStatus.Archived.toString()
//        rule.adjustFields()
        repository.save(rule)
        val portalTasks = portalTaskRepository.findAllByRuleId(rule.id!!)
        portalTasks
            .filter { it.status.value < TaskStatus.CANCELED.value }
            .forEach { it.status = TaskStatus.CANCELED }
        portalTaskRepository.saveAll(portalTasks)

        return "redirect:/crawl/rules/admin/"
    }
}
