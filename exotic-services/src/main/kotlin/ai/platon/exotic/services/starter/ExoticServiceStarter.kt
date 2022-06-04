package ai.platon.exotic.services.starter

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration
import org.springframework.boot.builder.SpringApplicationBuilder

@SpringBootApplication(
    exclude = [MongoAutoConfiguration::class, MongoDataAutoConfiguration::class],
    scanBasePackages = ["ai.platon.exotic.services.api"]
)
@EntityScan(
    "ai.platon.exotic.driver.crawl.entity",
    "ai.platon.exotic.services.api.entity"
)
class ExoticStarterApplication

fun main(args: Array<String>) {
    SpringApplicationBuilder(ExoticStarterApplication::class.java)
        .profiles("h2")
        .registerShutdownHook(true)
        .run(*args)
}
