package ai.platon.exotic.services.starter

import ai.platon.scent.boot.autoconfigure.ScentContextInitializer
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.ApplicationContext
import org.springframework.core.env.Environment
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.thymeleaf.templateresolver.ITemplateResolver

@SpringBootApplication(
    exclude = [MongoAutoConfiguration::class, MongoDataAutoConfiguration::class],
    scanBasePackages = ["ai.platon.exotic.services.api"]
)
@EnableJpaAuditing
@EntityScan(
    "ai.platon.exotic.driver.crawl.entity",
    "ai.platon.exotic.services.api.entity"
)
class ExoticStarterApplication

fun main(args: Array<String>) {
    SpringApplicationBuilder(ExoticStarterApplication::class.java)
        .profiles("h2")
        .initializers(ScentContextInitializer())
        .registerShutdownHook(true)
        .run(*args)
}
