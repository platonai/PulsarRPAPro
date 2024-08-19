package ai.platon.exotic.crawl.common

import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.dom.nodes.node.ext.isRegularText
import ai.platon.scent.ScentContext
import ai.platon.scent.analysis.corpus.annotateNodes
import ai.platon.scent.dom.HarvestOptions
import ai.platon.scent.dom.nodes.AnchorGroup
import ai.platon.scent.dom.nodes.node.ext.nthScreen
import ai.platon.scent.entities.HarvestResult
import ai.platon.scent.ml.EncodeOptions
import ai.platon.scent.ml.data.SimpleDataFrame
import ai.platon.scent.ql.h2.context.ScentSQLContext
import ai.platon.scent.ql.h2.context.ScentSQLContexts
import kotlinx.coroutines.runBlocking
import org.jsoup.nodes.Element
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

open class AdvancedVerboseCrawler(
    context: ScentContext = ScentSQLContexts.create()
): VerboseCrawler(context) {
    private val logger = LoggerFactory.getLogger(AdvancedVerboseCrawler::class.java)
    private val closed = AtomicBoolean()

    val sqlContext get() = context as ScentSQLContext
    
    val defaultHarvestArgs = "" +
        " -scrollCount 6" +
        " -itemScrollCount 4" +
        " -i 10d -ii 10000d" +
        " -requireSize 10000 -itemRequireSize 200000" +
        " -topLinks 100" +
        " -ignoreFailure" +
        " -showCombinedTable" +
        " -minimalColumnCount 3" +
        " -nScreens 5" +
//      " -polysemous" +
        " -diagnose" +
        " -nVerbose 1" +
//      " -pageLoadTimeout 60s" +
        " -showTip" +
//        " -showImage" +
//      " -cellType PLAIN_TEXT" +
        ""
    
    override val session = sqlContext.createSession()
    
    fun arrangeLinks(portalUrl: String, args: String): SortedSet<AnchorGroup> {
        val normUrl = session.normalize(portalUrl, session.options(args))
        val portalPage = session.load(normUrl)
        val portalDocument = session.parse(portalPage)
        return session.arrangeLinks(normUrl, portalDocument)
    }
    
    fun arrangeDocument(portalUrl: String, args: String): AnchorGroup? {
        val normUrl = session.normalize(portalUrl, session.options(args))
        val options = normUrl.hOptions
        val portalPage = session.load(normUrl)
        val portalDocument = session.parse(portalPage)
        val anchorGroups = session.arrangeLinks(normUrl, portalDocument)
        
        logger.info("------------------------------")
        anchorGroups.take(1).forEach {
            it.urlStrings.shuffled().take(10).forEachIndexed { i, url -> println("${1 + i}.\t$url") }
            it.urlStrings.take(options.topLinks)
                .map { session.load(it, options) }
                .map { session.parse(it, options) }
                .let { session.arrangeDocuments(normUrl, portalPage, it.asSequence()) }
        }
        
        portalDocument.also { it.annotateNodes(options) }.also { session.export(it, type = "portal") }
        
        return anchorGroups.firstOrNull()
    }
    
    fun encodeOutDocuments(portalUrl: String, args: String, encodeOptions: EncodeOptions): SimpleDataFrame {
        val urls = parseOutLinks(portalUrl, args).map { session.normalize(it).spec }
        return session.encodeNodes(urls, args, encodeOptions) { it.isRegularText && it.nthScreen <= 2 }
    }
    
    fun encodeDocuments(
        documents: Iterable<FeaturedDocument>, encodeOptions: EncodeOptions
    ) = session.encodeDocuments(documents, encodeOptions) { it.isRegularText && it.nthScreen <= 2 }
    
    fun encodeElements(
        rootElements: Iterable<Element>, encodeOptions: EncodeOptions
    ) = session.encodeElements(rootElements, encodeOptions) { it.isRegularText && it.nthScreen <= 2 }
    
    fun harvest(url: String, args: String) = harvest(url, session.options(args))
    
    fun harvest(url: String, options: HarvestOptions): HarvestResult {
        val result = runBlocking { session.harvest(url, options) }
        report(result, options)
        return result
    }
    
    fun harvest(urls: Iterable<String>, args: String) =
        harvest(urls.asSequence().map { session.loadDocument(it) }, session.options(args))
    
    fun harvest(documents: Sequence<FeaturedDocument>, options: HarvestOptions): HarvestResult {
        val result = session.harvest(documents, options)
        logger.info("Harvest finished.")
        logger.info("Ready to report the harvest result ...")
        report(result, options)
        return result
    }
    
    override fun close() {
        if (closed.compareAndSet(false, true)) {
        }
    }
}
