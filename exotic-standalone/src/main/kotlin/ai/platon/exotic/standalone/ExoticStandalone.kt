package ai.platon.exotic.standalone

import ai.platon.exotic.driver.crawl.ExoticCrawler
import ai.platon.pulsar.common.AppFiles
import ai.platon.pulsar.common.AppPaths
import ai.platon.scent.boot.autoconfigure.ScentContextInitializer
import ai.platon.scent.boot.autoconfigure.persist.CrawlSeedV3Repository
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.ImportResource
import org.springframework.core.env.Environment
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories

@SpringBootApplication(
    scanBasePackages = [
        "ai.platon.scent.boot.autoconfigure",
        "ai.platon.scent.rest.api",
        "ai.platon.exotic.services",
        "ai.platon.exotic.standalone",
    ]
)
@EntityScan(
    "ai.platon.exotic.driver.crawl.entity"
)
@ComponentScan(
    "ai.platon.scent.boot.autoconfigure",
    "ai.platon.scent.rest.api",
    "ai.platon.exotic.services",
    "ai.platon.exotic.standalone",
)
@ImportResource("classpath:config/app/app-beans/app-context.xml")
@EnableMongoRepositories("ai.platon.scent.boot.autoconfigure.persist")
@EnableJpaAuditing
class ExoticStandalone(
    val env: Environment,
    val crawlSeedV3Repository: CrawlSeedV3Repository
) {
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
    SpringApplicationBuilder(ExoticStandalone::class.java)
        .initializers(ScentContextInitializer())
        .registerShutdownHook(true)
        .run(*args)
}
