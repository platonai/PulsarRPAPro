package ai.platon.exotic.services.starter

import ai.platon.exotic.services.api.ExoticContextInitializer
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.builder.SpringApplicationBuilder

@SpringBootApplication(
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
        .initializers(ExoticContextInitializer())
        .run(*args)
}
