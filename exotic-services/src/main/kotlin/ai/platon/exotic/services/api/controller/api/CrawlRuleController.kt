package ai.platon.exotic.services.api.controller.api

import ai.platon.exotic.driver.crawl.entity.CrawlRule
import ai.platon.exotic.services.api.persist.CrawlRuleRepository
import ai.platon.pulsar.common.ResourceLoader
import org.springframework.web.bind.annotation.*
import java.time.Instant

@RestController
@RequestMapping("/api/crawl/rules")
class CrawlRuleController(
    private val repository: CrawlRuleRepository
) {
    @GetMapping("/")
    fun list(): List<CrawlRule> {
        return repository.findAll()
    }

    @PostMapping("add")
    fun add(@RequestBody rule: CrawlRule): CrawlRule {
        rule.createdDate = Instant.now()
        rule.lastModifiedDate = rule.createdDate

        if (rule.portalUrls.contains("jd.com")) {
            val sqlTemplate = ResourceLoader.readAllLines("sites/jd/template/extract/x-item.sql")
                .filter { line: String -> !line.startsWith("-- ") }
                .filter { line: String -> line.isNotBlank() }
                .joinToString("\n")
            rule.outLinkSelector = "#J_goodsList li[data-sku] a[href~=item]"
            rule.sqlTemplate = sqlTemplate
        }

//        rule.adjustFields()
        repository.save(rule)

        return rule
    }
}
