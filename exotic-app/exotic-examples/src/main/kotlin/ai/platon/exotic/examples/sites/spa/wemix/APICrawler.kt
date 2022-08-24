package ai.platon.exotic.examples.sites.spa.wemix

import ai.platon.pulsar.context.PulsarContexts
import ai.platon.pulsar.crawl.AbstractWebDriverHandler
import ai.platon.pulsar.crawl.AbstractWebPageWebDriverHandler
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.persist.WebPage
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

private class InitBrowserHandler(val initUrl: String): AbstractWebPageWebDriverHandler() {
    override suspend fun invokeDeferred(page: WebPage, driver: WebDriver): Any? {
        driver.navigateTo(initUrl)
        delay(10_000)
        return null
    }
}

private class APICrawler {
    private val mainUrl = "https://scope.wemixnetwork.com/1003/token/0xcb7615cb4322cddc518f670b4da042dbefc69500"
    private val apiTemplate = "https://scopi.wemixnetwork.com/api/v1/chain/1003/account/0xcb7615cb4322cddc518f670b4da042dbefc69500/tx"

    private val session = PulsarContexts.createSession()

    /**
     * Crawl with api with a single page application
     * Note that a proxy might be required in some country
     * */
    suspend fun crawl() {
        val initBrowserHandler = InitBrowserHandler(mainUrl)
        val options = session.options("-refresh")
        options.ensureEventHandler().loadEventHandler.onAfterBrowserLaunch.addLast(initBrowserHandler)

        IntRange(1, 100).forEach { pageNo ->
            val url = "$apiTemplate?page=$pageNo&pageSize=20"
            val json = session.loadResource(url, mainUrl, options).contentAsString
            println(json)
        }
    }
}

fun main() = runBlocking { APICrawler().crawl() }
