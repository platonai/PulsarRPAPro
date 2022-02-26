package ai.platon.exotic.services

import ai.platon.exotic.driver.crawl.ExoticCrawler
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafProperties
import org.springframework.boot.runApplication
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.core.env.Environment
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver
import org.thymeleaf.templateresolver.FileTemplateResolver
import org.thymeleaf.templateresolver.ITemplateResolver

@SpringBootApplication
@EnableJpaAuditing
@EntityScan(
    "ai.platon.exotic.driver.crawl.entity",
    "ai.platon.exotic.services.entity"
)
class ExoticApplication(
    val applicationContext: ApplicationContext,
    val env: Environment
) {
    @Autowired
    private lateinit var properties: ThymeleafProperties

    @Value("\${spring.thymeleaf.templates_root}")
    private val templatesRoot: String? = null

    @Bean
    fun defaultTemplateResolver(): ITemplateResolver? {
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

    @Bean
    fun exoticCrawler(): ExoticCrawler {
        return ExoticCrawler(env)
    }
}

fun main(args: Array<String>) {
    runApplication<ExoticApplication>(*args)
}
