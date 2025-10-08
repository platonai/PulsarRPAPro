package ai.platon.exotic.standalone.api

import ai.platon.exotic.common.ExoticUtils
import ai.platon.exotic.common.ScentURLUtils
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.external.ChatModelFactory
import ai.platon.scent.boot.autoconfigure.ScentContextInitializer
import ai.platon.scent.skeleton.ScentSession
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ImportResource
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories
import org.springframework.scheduling.annotation.EnableScheduling

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
class StandaloneApplication(
    val session: ScentSession
) {
    private val logger = getLogger(this::class)

    val hostname get() = "127.0.0.1"

    @Value("\${server.port}")
    var port: Int = 2718

    @Value("\${server.servlet.context-path}")
    lateinit var contextPath: String

    val baseURL get() = ScentURLUtils.buildServerUrl("localhost", port, contextPath)
        .replace("/api", "") // Ensure the URL is correct even if the API is upgraded
        .trimEnd('/')

    @PostConstruct
    fun showHelp() {
        val hasLLM = ChatModelFactory.isModelConfigured(session.unmodifiedConfig)
        val llmHelp = if (hasLLM) {
            "LLM is configured, you can use LLM commands."
        } else {
            "LLM is not configured, you can only use non-LLM commands. X-SQL is still available. " +
                    "It is highly recommended to set DEEPSEEK_API_KEY or other LLM keys to enable LLM features."
        }
        // val frontendURL = "$baseURL/command.html"
        val scrapingFrontendURL = "$baseURL/crawl/rules/"
        val commandFrontendURL = "$baseURL/command.html"
        val commandEndpoint = "$baseURL/api/commands/plain"
        val scrapingEndpoint = "$baseURL/api/x/e"

        val help = buildString {
            appendLine("====================================================================================")
            appendLine(llmHelp)
            appendLine("------------------------------------------------------------------------------")
            appendLine("Example 1: Using the WebUI to run a command:")
            appendLine(commandFrontendURL)
            appendLine("Example 2: Using the WebUI to run X-SQLs with schedule:")
            appendLine(scrapingFrontendURL)
            appendLine("------------------------------------------------------------------------------")
            appendLine("Example 3: For Beginners – Just Text, No Code:")
            appendLine(
                """
            ```shell
            curl -X POST "$commandEndpoint" -H "Content-Type: text/plain" -d "
                Go to https://www.amazon.com/dp/B08PP5MSVB

                After browser launch: clear browser cookies.
                After page load: scroll to the middle.

                Summarize the product.
                Extract: product name, price, ratings.
                Find all links containing /dp/.
            "
            ```
            """.trimIndent()
            )
            appendLine("------------------------------------------------------------------------------")
            appendLine("Example 4: For Advanced Users — LLM + X-SQL: Precise, Flexible, Powerful:")
            appendLine(
                """
            ```shell
            curl -X POST "$scrapingEndpoint" -H "Content-Type: text/plain" -d "
            select
              llm_extract(dom, 'product name, price, ratings') as llm_extracted_data,
              dom_base_uri(dom) as url,
              dom_first_text(dom, '#productTitle') as title,
              dom_first_slim_html(dom, 'img:expr(width > 400)') as img
            from load_and_select('https://www.amazon.com/dp/B08PP5MSVB', 'body');
            "
            ```
            """.trimIndent()
            )
        }

        logger.info("Welcome to PulsarRPAPro! \n{}", help)
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
