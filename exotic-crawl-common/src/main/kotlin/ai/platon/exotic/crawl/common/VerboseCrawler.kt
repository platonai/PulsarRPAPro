package ai.platon.exotic.crawl.common

import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.NetUtil
import ai.platon.pulsar.common.ProcessLauncher
import ai.platon.pulsar.common.browser.Browsers
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.common.stringify
import ai.platon.pulsar.common.urls.UrlUtils
import ai.platon.pulsar.context.PulsarContext
import ai.platon.pulsar.context.PulsarContexts
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.dom.nodes.node.ext.isRegularText
import ai.platon.pulsar.persist.WebPage
import ai.platon.scent.analysis.corpus.annotateNodes
import ai.platon.scent.context.ScentContexts
import ai.platon.scent.dom.HarvestOptions
import ai.platon.scent.dom.nodes.AnchorGroup
import ai.platon.scent.dom.nodes.node.ext.nthScreen
import ai.platon.scent.entities.HarvestResult
import ai.platon.scent.ml.EncodeOptions
import ai.platon.scent.ml.data.SimpleDataFrame
import kotlinx.coroutines.runBlocking
import org.jsoup.nodes.Element
import org.slf4j.LoggerFactory
import java.net.URL
import java.nio.file.Files
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

open class VerboseCrawler(
    val context: PulsarContext = PulsarContexts.create()
) : AutoCloseable {
    private val logger = LoggerFactory.getLogger(VerboseCrawler::class.java)
    private val closed = AtomicBoolean()
    
    open val isActive get() = !closed.get() && session.isActive
    open val session = ScentContexts.createSession()
    
    fun load(url: String, args: String) {
        return load(url, session.options(args))
    }
    
    fun load(url: String, options: LoadOptions) {
        val page = session.load(url)
        val doc = session.parse(page)
        doc.absoluteLinks()
        doc.stripScripts()
        
        doc.select(options.outLinkSelector) { it.attr("abs:href") }.asSequence()
            .filter { UrlUtils.isStandard(it) }
            .mapTo(HashSet()) { it.substringBefore(".com") }
            .asSequence()
            .filter { it.isNotBlank() }
            .mapTo(HashSet()) { "$it.com" }
            .filter { NetUtil.testHttpNetwork(URL(it)) }
            .take(10)
            .joinToString("\n") { it }
            .also { println(it) }
        
        val path = session.export(doc)
        logger.info("Export to: file://{}", path)
    }
    
    fun loadOutPages(portalUrl: String, args: String): Collection<WebPage> {
        return loadOutPages(portalUrl, session.options(args))
    }
    
    fun loadOutPages(portalUrl: String, options: LoadOptions): Collection<WebPage> {
        val page = session.load(portalUrl, options)
        if (!page.protocolStatus.isSuccess) {
            logger.warn("Failed to load page | {}", portalUrl)
        }
        
        val document = session.parse(page)
        document.absoluteLinks()
        document.stripScripts()
        val path = session.export(document)
        logger.info("Portal page is exported to: file://$path")
        
        val links = document.select(options.outLinkSelector) { it.attr("abs:href") }
            .mapTo(mutableSetOf()) { session.normalize(it, options) }
            .take(options.topLinks).map { it.spec }
        logger.info("Total {} items to load", links.size)
        
        val itemOptions = options.createItemOptions().apply { parse = true }
        return session.loadAll(links, itemOptions)
    }
    
    fun parseOutLinks(portalUrl: String, args: String): List<String> {
        val normUrl = session.normalize(portalUrl, session.options(args))
        val options = normUrl.hOptions
        val portalPage = session.load(normUrl)
        val portalDocument = session.parse(portalPage)
        return portalDocument.select(options.outLinkSelector)
            .take(options.topLinks)
            .map { it.attr("abs:href") }
    }
    
    fun report(result: HarvestResult, options: HarvestOptions) {
        try {
            session.buildAll(result.tableGroup, options)
            
            val json = session.buildJson(result.tableGroup)
            val path = AppPaths.REPORT_DIR.resolve("harvest/corpus/last-page-tables.json")
            val baseDir = path.parent
            Files.createDirectories(baseDir)
            Files.writeString(path, json)
            
            logger.info("Harvest result: file://$baseDir")
            
            // openBrowser("$baseDir")
        } catch (e: Exception) {
            logger.warn(e.stringify("Failed to report harvest result - "))
        }
    }
    
    fun openBrowser() {
        val path = AppPaths.REPORT_DIR.resolve("harvest/corpus/last-page-tables.json")
        val baseDir = path.parent
        openBrowser(baseDir.toString())
    }
    
    fun openBrowser(url: String) {
        val chromeBinary = Browsers.searchChromeBinary()
        val dataDir = AppPaths.getTmp("exotic-chrome")
        val args = listOf(
            url,
            "--user-data-dir=$dataDir",
            "--no-first-run",
            "--no-default-browser-check"
        )
        ProcessLauncher.launch("$chromeBinary", args)
    }
    
    override fun close() {
        if (closed.compareAndSet(false, true)) {
        }
    }
}
