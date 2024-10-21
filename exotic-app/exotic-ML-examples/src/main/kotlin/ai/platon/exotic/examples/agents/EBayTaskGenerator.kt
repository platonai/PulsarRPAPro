package ai.platon.exotic.examples.agents

import ai.platon.exotic.crawl.common.ExoticMLPaths
import ai.platon.exotic.crawl.common.VerboseCrawler1
import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.urls.Hyperlink
import ai.platon.pulsar.crawl.common.url.ListenableHyperlink
import ai.platon.pulsar.crawl.filter.AbstractScopedUrlNormalizer
import java.nio.file.Files
import java.time.Instant

object EBayUrls {
    val itemUrlPrefix = "https://www.ebay.com/itm/"
    
    fun isProductPage(url: String): Boolean {
        return url.startsWith(itemUrlPrefix)
    }
    
    fun normalizeProductUrl(url: String): String? {
        if (!isProductPage(url)) {
            return null
        }
        
        return url.substringBefore("?")
    }
}

class EBayProductUrlNormalizer : AbstractScopedUrlNormalizer() {
    override fun isRelevant(url: String, scope: String) = EBayUrls.isProductPage(url)
    
    override fun normalize(url: String, scope: String) = EBayUrls.normalizeProductUrl(url)
    
    fun normalize(link: Hyperlink, scope: String = ""): Hyperlink? {
        val url = normalize(link.url) ?: return null
        link.href = link.url
        link.url = url
        return link
    }
}

/**
 * Copy EBay product pages to the directory to wait for training.
 * */
class EBayHarvesterStarter(
    val args: String,
    val projectId: String
) {
    private val crawler = VerboseCrawler1()
    private val session = crawler.session
    
    private val datasetPath = ExoticMLPaths.datasetDir.resolve(projectId)
    private val htmlBaseDir = datasetPath.resolve("html")
    
    init {
        Files.createDirectories(htmlBaseDir)
        session.context.urlNormalizer.add(EBayProductUrlNormalizer())
    }
    
    fun collectListPageLinks(): List<Hyperlink> {
        return session.loadDocument("https://www.ebay.com/b/Apple/bn_21819543").selectHyperlinks("a[href~=/b/]")
    }

    fun loadAllAndExportToEncode(portalUrls: List<String>) {
        val options = session.options(args)
        val itemOptions = options.createItemOptions()
        val documents = session.loadDocuments(portalUrls, options)
        val urlNormalizer = EBayProductUrlNormalizer()
        val urls = documents.flatMap { it.selectHyperlinks(options.outLinkSelector) }
            .mapNotNullTo(HashSet()) { urlNormalizer.normalize(it) }
            .map { createListenableHyperlink(it, itemOptions.args) }

        session.submitAll(urls)
        session.context.await()

        createInfoFile()
    }

    private fun createListenableHyperlink(link: Hyperlink, args: String): ListenableHyperlink {
        val l = ListenableHyperlink(link.url, link.text, link.order, link.referrer, link.args, link.href)

        l.args = "$args -parse"
        l.event.loadEventHandlers.onHTMLDocumentParsed.addLast { page, document ->
            val url = page.url
            if (page.protocolStatus.isSuccess && EBayUrls.isProductPage(url)) {
                val path = htmlBaseDir.resolve(AppPaths.fromUri(url, suffix = ".html"))
                Files.writeString(path, document.outerHtml, Charsets.UTF_8)
            }
        }

        return l
    }

    private fun createInfoFile() {
        val path = datasetPath.resolve("htmlExportInfo.txt")
        val info = """
            buildTime: ${Instant.now()}
            args: $args
        """.trimIndent()
        Files.writeString(path, info)
    }
}

fun main() {
    val portalUrls = listOf(
        "https://www.ebay.com/b/Apple/bn_21819543",
        "https://www.ebay.com/b/Dell/bn_21823255",
        "https://www.ebay.com/b/HP-Laptops-and-Netbooks/177/bn_349568",
        "https://www.ebay.com/b/Lenovo/bn_21829183",
        "https://www.ebay.com/b/Microsoft/bn_21830663",
        "https://www.ebay.com/b/Canon-Digital-Cameras/31388/bn_740",
        "https://www.ebay.com/b/Nikon-Digital-Cameras/31388/bn_759",
        "https://www.ebay.com/b/LG/bn_21829255",
        "https://www.ebay.com/b/GoPro-Digital-Cameras/31388/bn_748",
        "https://www.ebay.com/b/Cameras-Photo/625/bn_1865546",
        "https://www.ebay.com/b/Video-Games-Consoles/1249/bn_1850232",
        "https://www.ebay.com/b/Portable-Audio-Headphones/15052/bn_1642614",
        "https://www.ebay.com/b/Cell-Phone-Displays/136699/bn_317614"
    )
    
    val projectId = "p1727773434"
    val args = " -i 10d -ii 100d -tl 1000 -ol a[href*=/itm/] -component #mainContent -itemRequireSize 800000 "
    
    val harvester = EBayHarvesterStarter(args, projectId)
    harvester.loadAllAndExportToEncode(portalUrls)
}
