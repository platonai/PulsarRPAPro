package ai.platon.exotic

import ai.platon.exotic.handlers.AmazonHtmlIntegrityChecker
import ai.platon.exotic.handlers.JdHtmlIntegrityChecker
import ai.platon.pulsar.common.AppFiles
import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.protocol.browser.emulator.BrowserResponseEvents
import ai.platon.pulsar.protocol.browser.emulator.BrowserResponseHandler
import ai.platon.pulsar.skeleton.crawl.fetch.privacy.PrivacyContextMonitor
import ai.platon.scent.boot.autoconfigure.ScentContextInitializer
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.h2.tools.Server
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ImportResource
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories
import java.sql.SQLException

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
    /**
     * Activate WebDriverPoolMonitor
     * */
    private val privacyContextMonitor: PrivacyContextMonitor,
    /**
     * Activate WebDriverPoolMonitor
     * */
//    private val driverPoolMonitor: WebDriverPoolMonitor,
    /**
     * Activate BrowserMonitor
     * */
//    private val browserMonitor: BrowserMonitor,
    /**
     * The unmodified config
     * */
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

    @Bean
    fun commandLineRunner(ctx: ApplicationContext): CommandLineRunner {
        println("Context: $ctx")
        return CommandLineRunner { args ->
            val beans = ctx.beanDefinitionNames.sorted()
            val s = beans.joinToString("\n") { it }
            val path = AppPaths.getTmp("spring-beans.txt")
            AppFiles.saveTo(s, path)
        }
    }

    /**
     *
     * */
    @Bean(initMethod = "start", destroyMethod = "stop")
    @Throws(SQLException::class)
    fun h2Server(): Server {
        // return Server.createTcpServer("-trace")
        return Server.createTcpServer()
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    @Throws(SQLException::class)
    fun h2WebServer(): Server {
        return Server.createWebServer("-webAllowOthers")
    }
}

fun main(args: Array<String>) {
    SpringApplicationBuilder(ExoticServerApplication::class.java)
        .initializers(ScentContextInitializer())
        .registerShutdownHook(true)
        .run(*args)
}
