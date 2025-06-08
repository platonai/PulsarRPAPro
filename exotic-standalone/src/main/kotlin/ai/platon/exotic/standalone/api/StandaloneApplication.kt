package ai.platon.exotic.standalone.api

import ai.platon.exotic.common.ExoticUtils
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

    @Value("\${server.port}")
    var port: Int = 2718

    @Value("\${server.servlet.context-path}")
    lateinit var contextPath: String

    @PostConstruct
    fun showHelp() {
        val baseURL = "http://localhost:$port"
        val frontendURL = baseURL + "/" + "$contextPath/crawl/rules/".trimStart('/')
        val backendURL = baseURL + "/" + "$contextPath/api/hello/whoami".trimStart('/')

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
