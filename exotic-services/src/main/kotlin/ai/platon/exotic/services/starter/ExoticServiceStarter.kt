package ai.platon.exotic.services.starter

import ai.platon.exotic.services.api.ExoticContextInitializer
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@SpringBootApplication(
    scanBasePackages = ["ai.platon.exotic.services.api"]
)
@EnableJpaRepositories("ai.platon.exotic.services.api.persist")
@EntityScan(
    "ai.platon.exotic.driver.crawl.entity",
    "ai.platon.exotic.services.api.entity"
)
class ExoticStarterApplication {

}

fun main(args: Array<String>) {
    SpringApplicationBuilder(ExoticStarterApplication::class.java)
        .profiles("hsqldb")
        .registerShutdownHook(true)
        .initializers(ExoticContextInitializer())
        .run(*args)
}
