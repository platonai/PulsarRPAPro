package ai.platon.exotic.services.api

import ai.platon.exotic.driver.crawl.ExoticCrawler
import ai.platon.pulsar.common.getLogger
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafProperties
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.core.env.Environment
import org.springframework.core.env.get
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.thymeleaf.templateresolver.FileTemplateResolver
import org.thymeleaf.templateresolver.ITemplateResolver
import java.nio.file.Files
import java.nio.file.Paths
import javax.annotation.PostConstruct

@SpringBootApplication
@EnableJpaAuditing
@EntityScan(
    "ai.platon.exotic.driver.crawl.entity",
    "ai.platon.exotic.services.api.entity"
)
class ExoticApplication(
    val applicationContext: ApplicationContext,
    val defaultThymeleafTemplateResolver: ITemplateResolver,
    val env: Environment
) {
    private val logger = getLogger(this)

    @Autowired
    private lateinit var properties: ThymeleafProperties

    @Value("\${spring.thymeleaf.templates_root:}")
    private val templatesRoot: String? = null

    @PostConstruct
    fun postConstruct() {
        logger.info("Database url: {}", env["spring.datasource.url"])
    }

    @Bean
    fun autoReloadThymeleafTemplateResolver(): ITemplateResolver? {
        if (templatesRoot == null || !Files.exists(Paths.get(templatesRoot))) {
            // fail back to default resolver
            return defaultThymeleafTemplateResolver
        }

        val resolver = FileTemplateResolver()
        resolver.suffix = properties.suffix
        resolver.prefix = templatesRoot
        resolver.setTemplateMode(properties.mode)
        resolver.isCacheable = properties.isCache

        return resolver
    }

    @Bean
    fun javaTimeModule(): JavaTimeModule {
        return JavaTimeModule()
    }

    @Bean(destroyMethod = "close")
    fun exoticCrawler(): ExoticCrawler {
        return ExoticCrawler(env)
    }
}
