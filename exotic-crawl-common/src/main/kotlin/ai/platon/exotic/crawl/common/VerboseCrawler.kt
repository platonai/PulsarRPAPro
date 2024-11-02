package ai.platon.exotic.crawl.common

import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.browser.Browsers
import ai.platon.pulsar.common.urls.UrlUtils
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.skeleton.common.options.LoadOptions
import ai.platon.pulsar.skeleton.context.PulsarContext
import ai.platon.pulsar.skeleton.context.PulsarContexts
import ai.platon.scent.context.ScentContexts
import ai.platon.scent.dom.HarvestOptions
import ai.platon.scent.entities.HarvestResult
import org.slf4j.LoggerFactory
import java.net.URL
import java.nio.file.Path
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
            val exportedDocuments = session.buildAll(result.tableGroup, options)
            val baseDir = exportedDocuments.keys.firstOrNull()?.parent ?: return
            logger.info("Harvest result: file://$baseDir")
        } catch (e: Exception) {
            warnUnexpected(this, e, "Failed to report harvest result")
        }
    }
    
    fun openExplorer(path: Path) {
        Runtimes.exec("explorer.exe $path")
    }
    
    fun openBrowser(path: Path) {
        openBrowser(path.toString())
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
