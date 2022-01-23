package ai.platon.scent.examples.sites.ccgp

import ai.platon.pulsar.PulsarSession
import ai.platon.pulsar.browser.driver.BrowserSettings
import ai.platon.pulsar.common.ResourceLoader
import ai.platon.pulsar.crawl.AbstractJsEventHandler
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.persist.WebPage
import ai.platon.scent.context.withContext
import org.slf4j.LoggerFactory
import java.io.BufferedWriter
import java.io.FileWriter
import java.util.concurrent.atomic.AtomicInteger

class JsEventHandler : AbstractJsEventHandler() {
    val expressions = """
let message = "Start searching ...";
document.querySelector(".search-cont input#key").value = '软件';
document.querySelector(".search-page-info button:contains('查询')").click();
    """.trimIndent()

    override suspend fun onAfterComputeFeature(page: WebPage, driver: WebDriver): Any? {
        return evaluate(driver, expressions.split(";"))
    }
}

class Search(
    val portalUrl: String,
    val loadArguments: String,
    val session: PulsarSession
) {
    private val logger = LoggerFactory.getLogger(Search::class.java)

    val options = session.options(loadArguments)

    fun search() {
        // 1. warn up
        val page = session.load(portalUrl, options)

        var document = session.parse(page)
        var text = document.selectFirstOrNull(".serach-page-state")?.text() ?: "(unknown)"
        println("Search message: $text")

        val jsEventHandler = JsEventHandler()
        options.conf.putBean(JsEventHandler())
        session.load(portalUrl, options)
        options.conf.removeBean(jsEventHandler)

        document = session.loadDocument(portalUrl, options)

        text = document.selectFirstOrNull(".serach-page-state")?.text() ?: "(unknown)"
        println("Search message: $text")
    }
}

fun main() {
    BrowserSettings.withGUI()

    withContext {
        val session = it.createSession()
        val urls = ResourceLoader.readAllLines("ccgp/seeds.txt")
        val filename = "/tmp/bidding-20211013.txt"
        val writer = BufferedWriter(FileWriter(filename))

        val count = AtomicInteger()
        urls.parallelStream().forEach { url ->
            try {
                count.incrementAndGet()
                println("$count.\tProcessing $url")
                val page = session.load(url)
                val textDocument = session.harvestArticle(page)
                val document = session.parse(page)
                val article = document.selectFirstOrNull(".art_con")?.text()?:""
                val textContent = if (article.length > textDocument.textContent.length) article else textDocument.textContent

                writer.write("\n\n\n")
                writer.write(textDocument.baseUrl)
                writer.newLine()
                writer.write(textDocument.contentTitle)
                writer.newLine()
                writer.write(textDocument.publishTime.toString())
                writer.newLine()
                writer.write(textContent)
                writer.newLine()
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }
    }
}
