package ai.platon.exotic

import ai.platon.exotic.handlers.AmazonHtmlIntegrityChecker
import ai.platon.exotic.handlers.JdHtmlIntegrityChecker
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.protocol.browser.emulator.BrowserResponseEvents
import ai.platon.pulsar.protocol.browser.emulator.BrowserResponseHandler
import ai.platon.scent.boot.autoconfigure.ScentContextInitializer
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ImportResource
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories

@SpringBootApplication(
    scanBasePackages = [
        "ai.platon.scent.boot.autoconfigure",
        "ai.platon.scent.rest.api",
    ]
)
@EntityScan(
    "ai.platon.exotic.driver.crawl.entity",
    "ai.platon.exotic.services.entity"
)
@ImportResource("classpath:config/app/app-beans/app-context.xml")
@EnableMongoRepositories("ai.platon.scent.boot.autoconfigure.persist")
class ExoticServerApplication(
    private val browserResponseHandler: BrowserResponseHandler,
    private val immutableConfig: ImmutableConfig
) {
    @Bean
    fun initBrowserResponseHandler() {
        browserResponseHandler.emit(BrowserResponseEvents.initHTMLIntegrityChecker,
            AmazonHtmlIntegrityChecker(immutableConfig))
        browserResponseHandler.emit(BrowserResponseEvents.initHTMLIntegrityChecker,
            JdHtmlIntegrityChecker(immutableConfig))
    }

    @Bean
    fun javaTimeModule(): JavaTimeModule {
        return JavaTimeModule()
    }
}

fun main(args: Array<String>) {
    SpringApplicationBuilder(ExoticServerApplication::class.java)
        .initializers(ScentContextInitializer())
        .registerShutdownHook(true)
        .run(*args)
}
