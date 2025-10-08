package ai.platon.exotic.server

import ai.platon.exotic.server.handlers.AmazonHtmlIntegrityChecker
import ai.platon.exotic.server.handlers.JdHtmlIntegrityChecker
import ai.platon.pulsar.common.AppFiles
import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.urls.URLUtils
import ai.platon.pulsar.external.ChatModelFactory
import ai.platon.pulsar.protocol.browser.emulator.BrowserResponseEvents
import ai.platon.pulsar.protocol.browser.emulator.BrowserResponseHandler
import ai.platon.pulsar.skeleton.crawl.fetch.privacy.PrivacyContextMonitor
import ai.platon.scent.boot.autoconfigure.ScentContextInitializer
import ai.platon.scent.skeleton.ScentSession
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import jakarta.annotation.PostConstruct
import org.h2.tools.Server
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ImportResource
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories
import java.net.URI
import java.net.URISyntaxException
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
    private val logger = getLogger(ExoticServerApplication::class)

    @Value("\${server.port}")
    var port: Int = 8182

    @Value("\${server.servlet.context-path}")
    lateinit var contextPath: String

    @Autowired
    lateinit var session: ScentSession

    @PostConstruct
    fun initBrowserResponseHandler() {
        browserResponseHandler.emit(
            BrowserResponseEvents.initHTMLIntegrityChecker,
            AmazonHtmlIntegrityChecker(immutableConfig)
        )
        browserResponseHandler.emit(
            BrowserResponseEvents.initHTMLIntegrityChecker,
            JdHtmlIntegrityChecker(immutableConfig)
        )
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
            val path = AppPaths.getTmpDirectory("spring-beans.txt")
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

    @PostConstruct
    fun showHelp() {
        val hasLLM = ChatModelFactory.isModelConfigured(session.unmodifiedConfig)
        val llmHelp = if (hasLLM) {
            "LLM is configured, you can use LLM commands."
        } else {
            "LLM is not configured, you can only use non-LLM commands. X-SQL is still available. " +
                    "It is highly recommended to set DEEPSEEK_API_KEY or other LLM keys to enable LLM features."
        }
        // val baseURL = URLUtils.buildServerUrl("localhost", port, contextPath)
        val baseURL = buildServerUrl("localhost", port, contextPath)
            .replace("/api", "") // Ensure the URL is correct even if the API is upgraded
            .trimEnd('/')
        val frontendURL = "$baseURL/command.html"
        val commandEndpoint = "$baseURL/api/commands/plain"
        val scrapingEndpoint = "$baseURL/api/x/e"

        val help = buildString {
            appendLine("====================================================================================")
            appendLine(llmHelp)
            appendLine("------------------------------------------------------------------------------")
            appendLine("Example 1: Using the WebUI to run a command:")
            appendLine(frontendURL)
            appendLine("------------------------------------------------------------------------------")
            appendLine("Example 2: For Beginners – Just Text, No Code:")
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
            appendLine("Example 3: For Advanced Users — LLM + X-SQL: Precise, Flexible, Powerful:")
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

    /**
     * Normalizes a URL by removing redundant slashes and normalizing the URI.
     * @param url The URL to normalize.
     * @return The normalized URL as a string.
     * */
    private fun removeRedundantSlashes(url: String): String {
        val uri = URI(url).normalize()

        // 直接判断路径是否为null或空，简化逻辑
        val normalizedPath = uri.path?.replace(Regex("/+"), "/") ?: "/"

        return URI(
            uri.scheme,
            uri.userInfo,
            uri.host,
            uri.port,
            normalizedPath,
            uri.query,
            uri.fragment
        ).toString()
    }

    /**
     * Builds a server URL from the given hostname, port, context path, and path.
     * @param hostname The hostname of the server.
     * @param port The port number of the server.
     * @param contextPath The context path of the server.
     * @param path The specific path to append to the context path.
     * @return The constructed server URL as a string.
     * */
    @Throws(URISyntaxException::class)
    private fun buildServerUrl(hostname: String, port: Int, contextPath: String, path: String = ""): String {
        val combinedPath = listOf(contextPath, path)
            .filter { it.isNotBlank() }
            .joinToString("/")
            .replace(Regex("/+"), "/")
            .let { if (!it.startsWith("/")) "/$it" else it }
        val url = "http://$hostname:$port$combinedPath"
        return removeRedundantSlashes(url)
    }
}

fun main(args: Array<String>) {
    SpringApplicationBuilder(ExoticServerApplication::class.java)
        .initializers(ScentContextInitializer())
        .registerShutdownHook(true)
        .run(*args)
}
