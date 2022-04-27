package ai.platon.exotic.standalone

import ai.platon.scent.boot.autoconfigure.ScentContextInitializer
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.annotation.ImportResource
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication(
    scanBasePackages = [
        "ai.platon.scent.boot.autoconfigure",
        "ai.platon.scent.rest.api"
    ]
)
@EntityScan(
    "ai.platon.exotic.driver.crawl.entity",
    "ai.platon.exotic.services.entity"
)
@ImportResource("classpath:config/app/app-beans/app-context.xml")
@EnableMongoRepositories("ai.platon.scent.boot.autoconfigure.persist")
@EnableJpaAuditing
@EnableScheduling
class ExoticStandalone

fun main(args: Array<String>) {
    SpringApplicationBuilder(ExoticStandalone::class.java)
        .profiles("h2")
        .initializers(ScentContextInitializer())
        .registerShutdownHook(true)
        .run(*args)
}
