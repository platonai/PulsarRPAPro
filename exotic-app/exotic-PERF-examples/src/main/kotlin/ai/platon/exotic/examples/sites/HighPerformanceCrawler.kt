package ai.platon.exotic.examples.sites

import ai.platon.pulsar.browser.common.BlockRule
import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.browser.common.InteractSettings
import ai.platon.pulsar.common.LinkExtractors
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.metrics.MetricsSystem
import ai.platon.pulsar.context.PulsarContexts
import ai.platon.pulsar.crawl.common.url.ListenableHyperlink
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.ImportResource
import org.springframework.context.support.AbstractApplicationContext

class HighPerformanceCrawlerInitializer: ApplicationContextInitializer<AbstractApplicationContext> {
    override fun initialize(applicationContext: AbstractApplicationContext) {
        System.setProperty(CapabilityTypes.PROXY_POOL_MANAGER_CLASS, "ai.platon.pulsar.common.proxy.ProxyPoolManager")
        System.setProperty(CapabilityTypes.PROXY_LOADER_CLASS, "ai.platon.exotic.examples.sites.proxy.ProxyVendorLoader")
        BrowserSettings.privacy(4).maxTabs(12)
        PulsarContexts.create()
    }
}

@SpringBootApplication
@ImportResource("classpath:pulsar-beans/app-context.xml")
@ComponentScan("ai.platon.pulsar.rest.api")
class HighPerformanceCrawler(
    /**
     * Activate MetricsSystem
     * */
    private val metricsSystem: MetricsSystem
) {
    private val session = PulsarContexts.createSession()

    @Bean
    fun generate() {
        val resource = "seeds/amazon/best-sellers/leaf-categories.txt"
        val args = "-i 10s -ignoreFailure"
        // block unnecessary resources, we must be very careful to choose the resource to block
        val blockingUrls = BlockRule().blockingUrls
        // less interaction with the page, faster crawl speed
        val interactSettings = InteractSettings(initScrollPositions = "0.2,0.5", scrollCount = 0)

        val links = LinkExtractors.fromResource(resource).asSequence()
            .map { ListenableHyperlink(it, "", args = args) }
            .onEach {
                it.event.browseEvent.onWillNavigate.addLast { page, driver ->
                    // This is a temporary solution to override InteractSettings, will be improved in the future
                    page.setVar("InteractSettings", interactSettings)
                    driver.addBlockedURLs(blockingUrls)
                }
            }.toList()

        session.submitAll(links)
    }
}

fun main(args: Array<String>) {
    // NOTE: Enable proxy for best demonstration, you can find the instruction to enable proxy in README.md

    runApplication<HighPerformanceCrawler>(*args) {
        setRegisterShutdownHook(true)
        addInitializers(HighPerformanceCrawlerInitializer())
        setLogStartupInfo(true)
    }
}
