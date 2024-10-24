package ai.platon.exotic.examples.agents

import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.urls.Hyperlink
import ai.platon.pulsar.crawl.common.url.ListenableHyperlink
import ai.platon.pulsar.crawl.filter.AbstractScopedUrlNormalizer
import ai.platon.scent.tools.VerboseCrawler
import java.nio.file.Files

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
class EBayTaskGenerator(
    val args: String,
    val projectId: String
) {
    private val crawler = VerboseCrawler()
    private val session = crawler.session
    
    private val project = EncodeProject(projectId, EncodeProject.Type.PREDICT)
    
    init {
        project.createDirectories()
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

        project.createEncodeInfo(mapOf("args" to args, "itemArgs" to itemOptions.args))
        project.createConfigFile(mapOf("args" to args))
    }

    private fun createListenableHyperlink(link: Hyperlink, args: String): ListenableHyperlink {
        val l = ListenableHyperlink(link.url, link.text, link.order, link.referrer, link.args, link.href)

        l.args = "$args -parse -requireSize 500000"
        l.event.loadEventHandlers.onHTMLDocumentParsed.addLast { page, document ->
            val url = page.url
            if (page.protocolStatus.isSuccess && EBayUrls.isProductPage(url)) {
                val path = project.htmlBaseDir.resolve(AppPaths.fromUri(url, suffix = ".html"))
                Files.writeString(path, document.outerHtml, Charsets.UTF_8)
            }
        }
        
        return l
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
    
    val portalUrls2 = """
        https://www.ebay.com/b/Bath-Body-Products/11838/bn_1850348
        https://www.ebay.com/b/Bath-Body-Mixed-Items/29584/bn_2310611
        https://www.ebay.com/b/Bath-Bombs-Fizzies/72759/bn_2310690
        https://www.ebay.com/b/Bath-Oils/31751/bn_2311546
        https://www.ebay.com/b/Bath-Salts/67390/bn_2313805
        https://www.ebay.com/b/Bath-Sets-Kits/67391/bn_2314007
        https://www.ebay.com/b/Body-Powders/29581/bn_2309590
        https://www.ebay.com/b/Body-Sprays-Mists/31753/bn_2313838
        https://www.ebay.com/b/Bubble-Baths/31755/bn_2313619
        https://www.ebay.com/b/Deodorants-Antiperspirants/29580/bn_2310460
    """.trimIndent().split("\n").map { it.trim() }

//    val projectId = "p1727773434"
    val projectId = "p1729409382"
    val args = " -i 10d -ii 100d -tl 1000 -ol a[href*=/itm/] -component #mainContent -itemRequireSize 500000 "

    val harvester = EBayTaskGenerator(args, projectId)
    harvester.loadAllAndExportToEncode(portalUrls2)
    // harvester.createHarvestResultDatasetView()
}
