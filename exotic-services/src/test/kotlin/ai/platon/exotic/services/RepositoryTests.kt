package ai.platon.exotic.services

import ai.platon.exotic.driver.common.PROP_FETCH_NEXT_OFFSET
import ai.platon.exotic.driver.crawl.entity.CrawlRule
import ai.platon.exotic.driver.crawl.entity.PortalTask
import ai.platon.exotic.driver.crawl.scraper.TaskStatus
import ai.platon.exotic.services.entity.SysProp
import ai.platon.exotic.services.entity.generated.IntegratedProduct
import ai.platon.exotic.services.persist.*
import com.google.gson.GsonBuilder
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.repository.findByIdOrNull
import java.util.*
import kotlin.test.assertEquals

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

        val portalTasks = pagedPortalUrls.map {
            PortalTask(it, "-refresh", 3).also {
                it.rule = rule
                it.status = TaskStatus.CREATED
            }
        }

        crawlRuleRepository.save(rule)
        portalTaskRepository.saveAll(portalTasks)
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
