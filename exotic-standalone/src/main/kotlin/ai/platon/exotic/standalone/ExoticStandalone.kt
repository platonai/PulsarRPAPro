package ai.platon.exotic.standalone

import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.scent.boot.autoconfigure.ScentContextInitializer
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.ImportResource
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication(
    scanBasePackages = [
        "ai.platon.scent.boot.autoconfigure",
        "ai.platon.scent.rest.api",
        "ai.platon.exotic.services"
    ]
)
@ComponentScan(
    "ai.platon.scent.rest.api",
    "ai.platon.exotic.services",
    "ai.platon.exotic.standalone",
)
@EntityScan(
    "ai.platon.exotic.driver.crawl.entity",
    "ai.platon.exotic.services.entity",
)
@ImportResource("classpath:config/app/app-beans/app-context.xml")
@EnableJpaRepositories("ai.platon.exotic.services.persist")
@EnableMongoRepositories("ai.platon.scent.boot.autoconfigure.persist")
// failed to import Applications
// @Import(ExoticApplication::class, ExoticServerApplication::class)
@EnableScheduling
@EnableJpaAuditing
class ExoticStandalone

fun main(args: Array<String>) {
    System.setProperty(CapabilityTypes.STORAGE_DATA_STORE_CLASS, AppConstants.FILE_BACKEND_STORE_CLASS)
//    BrowserSettings.headless()

    SpringApplicationBuilder(ExoticStandalone::class.java)
        .profiles("h2")
        .initializers(ScentContextInitializer())
        .registerShutdownHook(true)
        .run(*args)
}
