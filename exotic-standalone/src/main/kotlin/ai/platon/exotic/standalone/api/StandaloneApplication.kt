package ai.platon.exotic.standalone.api

import ai.platon.exotic.common.ExoticUtils
import ai.platon.exotic.common.ScentURLUtils
import ai.platon.pulsar.common.getLogger
import ai.platon.scent.boot.autoconfigure.ScentContextInitializer
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ImportResource
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories
import org.springframework.scheduling.annotation.EnableScheduling
import javax.ws.rs.core.UriBuilder

@SpringBootApplication(
    scanBasePackages = [
        "ai.platon.scent.boot.autoconfigure",
        "ai.platon.scent.rest.api",
        "ai.platon.exotic.services.api",
        "ai.platon.exotic.standalone.api",
    ]
)
@EntityScan(
    "ai.platon.exotic.driver.crawl.entity",
    "ai.platon.exotic.services.entity",
)
@ImportResource("classpath:config/app/app-beans/app-context.xml")
@EnableJpaRepositories("ai.platon.exotic.services.api.persist")
@EnableMongoRepositories("ai.platon.scent.boot.autoconfigure.persist")
@EnableScheduling
class StandaloneApplication {
    private val logger = getLogger(this::class)

    val hostname get() = "127.0.0.1"

    @Value("\${server.port}")
    var port: Int = 2718

    @Value("\${server.servlet.context-path}")
    lateinit var contextPath: String

    val baseUri get() = ScentURLUtils.buildServerUrl(hostname, port, contextPath, "api")

    @PostConstruct
    fun showHelp() {
        val frontendURL = "$baseUri/crawl/rules/"
        val backendURL = "$baseUri/api/hello/whoami".replace("/api/api/", "/api/") // Ensure the URL is correct even if the API is upgraded

        val help = """
frontend: $frontendURL
backend: $backendURL

        """.trimIndent()

        logger.info("Endpoint: \n{}", help)
    }
}

fun main(argv: Array<String>) {
    ExoticUtils.prepareDatabaseOrFail()

    val addProfiles = mutableListOf("hsqldb")
    runApplication<StandaloneApplication> {
        setAdditionalProfiles(*addProfiles.toTypedArray())
        addInitializers(ScentContextInitializer())
        setRegisterShutdownHook(true)
        setLogStartupInfo(true)
    }
}
