package ai.platon.exotic.examples.perf

import ai.platon.exotic.crawl.common.proxy.ProxyVendorLoader
import ai.platon.pulsar.browser.common.BlockRule
import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.browser.common.InteractSettings
import ai.platon.pulsar.common.LinkExtractors
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.proxy.ProxyPoolManager
import ai.platon.pulsar.skeleton.common.metrics.MetricsSystem
import ai.platon.pulsar.skeleton.context.PulsarContexts
import ai.platon.pulsar.skeleton.crawl.common.url.ListenableHyperlink
import jakarta.annotation.PostConstruct
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.ImportResource
import org.springframework.context.support.AbstractApplicationContext

class HighPerformanceCrawlerInitializer: ApplicationContextInitializer<AbstractApplicationContext> {
    override fun initialize(applicationContext: AbstractApplicationContext) {
        System.setProperty(CapabilityTypes.PROXY_POOL_MANAGER_CLASS, ProxyPoolManager::class.java.name)
        System.setProperty(CapabilityTypes.PROXY_LOADER_CLASS, ProxyVendorLoader::class.java.name)
        BrowserSettings.maxBrowsers(4).maxOpenTabs(8).withSequentialBrowsers()
        PulsarContexts.create(applicationContext)
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
    private val session = PulsarContexts.getOrCreateSession()

    @PostConstruct
    fun generate() {
        val resource = "seeds/amazon/best-sellers/leaf-categories.txt"
        val args = "-i 10s -ignoreFailure"
        // block unnecessary resources, we must be very careful to choose the resource to block
        val blockingUrls = BlockRule().blockingUrls
        // less interaction with the page, faster crawl speed
        val interactSettings = InteractSettings(initScrollPositions = "0.2,0.5", scrollCount = 0)
        session.sessionConfig.putBean(interactSettings)

        val links = LinkExtractors.fromResource(resource).asSequence()
            .map { ListenableHyperlink(it, "", args = args) }
            .onEach {
                it.eventHandlers.browseEventHandlers.onWillNavigate.addLast { page, driver ->
                    driver.addBlockedURLs(blockingUrls)
                }
            }.toList()

        session.submitAll(links)
    }
}

fun main(args: Array<String>) {
    // NOTE: Enable proxy for best demonstration, you can find the instruction to enable proxy in README.md

    runApplication<HighPerformanceCrawler>(*args) {
        addInitializers(HighPerformanceCrawlerInitializer())
    }
}
