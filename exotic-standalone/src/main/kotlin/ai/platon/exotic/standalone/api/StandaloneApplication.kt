package ai.platon.exotic.standalone.api

import ai.platon.exotic.driver.common.ExoticUtils
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.persist.WebDb
import ai.platon.scent.boot.autoconfigure.ScentContextInitializer
import de.flapdoodle.embed.mongo.MongodExecutable
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.annotation.*
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
    "ai.platon.exotic.services.api",
    "ai.platon.exotic.standalone",
)
@EntityScan(
    "ai.platon.exotic.driver.crawl.entity",
    "ai.platon.exotic.services.entity",
)
@ImportResource("classpath:config/app/app-beans/app-context.xml")
@EnableJpaRepositories("ai.platon.exotic.services.api.persist")
@EnableMongoRepositories("ai.platon.scent.boot.autoconfigure.persist")
// failed to import Applications
// @Import(ExoticApplication::class, ExoticServerApplication::class)
@EnableScheduling
@EnableJpaAuditing
class StandaloneApplication(
    private val embeddedMongoServer: MongodExecutable,
    private val immutableConfig: ImmutableConfig
) {
    private val logger = getLogger(this)

    @Primary
    @DependsOn("embeddedMongoServer")
    @Bean
    fun createWebDb(): WebDb {
        logger.info("User the overridden WebDb bean which depends on embeddedMongoServer" +
                " to ensure the correct shutdown order")
        return WebDb(immutableConfig)
    }
}

fun main(argv: Array<String>) {
    ExoticUtils.prepareDatabaseOrFail()

    // System.setProperty("scrape.submitter.dry.run", "true")
    SpringApplicationBuilder(StandaloneApplication::class.java)
        .profiles("h2")
        .initializers(ScentContextInitializer())
        .registerShutdownHook(true)
        .run(*argv)
}
