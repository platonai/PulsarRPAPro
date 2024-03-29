package ai.platon.exotic.services.api

import ai.platon.exotic.driver.common.PROP_FETCH_NEXT_OFFSET
import ai.platon.exotic.driver.crawl.entity.CrawlRule
import ai.platon.exotic.driver.crawl.entity.PortalTask
import ai.platon.exotic.driver.crawl.scraper.TaskStatus
import ai.platon.exotic.services.api.entity.SysProp
import ai.platon.exotic.services.api.entity.generated.IntegratedProduct
import ai.platon.exotic.services.api.persist.*
import com.google.gson.GsonBuilder
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.repository.findByIdOrNull
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@SpringBootTest
class RepositoryTests @Autowired constructor(
    val fullFieldProductRepository: FullFieldProductRepository,
    val integratedProductRepository: IntegratedProductRepository,
    val crawlRuleRepository: CrawlRuleRepository,
    val portalTaskRepository: PortalTaskRepository,
    val sysPropRepository: SysPropRepository,
) {
    @Test
    fun testDate() {
    }

    @Test
    fun testSysProp() {
        sysPropRepository.save(SysProp(PROP_FETCH_NEXT_OFFSET, "99999"))
        val prop = sysPropRepository.findByIdOrNull(PROP_FETCH_NEXT_OFFSET)
        println(prop)
        requireNotNull(prop)
        assertEquals("99999", prop.value)
    }

    @Test
    fun testPortalTaskRepository() {
        val pagedPortalUrls = mutableListOf(
            "https://list.jd.com/list.html?cat=670,671,672&page=1",
            "https://list.jd.com/list.html?cat=670,671,672&page=2",
            "https://list.jd.com/list.html?cat=670,671,672&page=3",
            "https://list.jd.com/list.html?cat=670,671,672&page=4",
        )

        val rule = CrawlRule()
        rule.portalUrls = pagedPortalUrls.joinToString("\n")
        assertEquals(pagedPortalUrls.size, rule.portalUrlList.size)

        val portalTasks = rule.portalUrlList.map {
            PortalTask(it, "-refresh", 3).also {
                it.rule = rule
                it.status = TaskStatus.CREATED
            }
        }

        crawlRuleRepository.save(rule)
        val ruleId = rule.id
        assertNotNull(ruleId)
        portalTaskRepository.saveAll(portalTasks)
        portalTasks.forEach {
            assertNotNull(it.id) { "PortalTask.id should be non-null" }
        }

        val rule2 = crawlRuleRepository.findById(ruleId).get()
        assertEquals(pagedPortalUrls.size, rule2.portalUrlList.size)
        assertEquals(pagedPortalUrls.size, rule2.portalTasks.size)

        val portalTasks2 = portalTaskRepository.findAllByRuleId(ruleId)
        assertEquals(pagedPortalUrls.size, portalTasks2.size)
        portalTasks2.forEach {
            assertNotNull(it.rule) { "PortalTask.rule should be non-null" }
        }
    }

    @Test
    fun testSyncRepository() {
        println(Date())

        val categoryUrl = "https://list.jd.com/list.html?cat=737,794,798"
//        scraper.jdScraper.loadProductOverviews(categoryUrl)

        val gson = GsonBuilder().setPrettyPrinting().create()
        val integratedProducts =
            fullFieldProductRepository.findAll().map { gson.fromJson(gson.toJson(it), IntegratedProduct::class.java) }
        println(gson.toJson(integratedProducts))
        // integratedProductRepository.saveAll(integratedProducts)
    }
}
