package ai.platon.exotic.crawl.common

import ai.platon.pulsar.common.warnUnexpected
import ai.platon.pulsar.dom.Documents
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.scent.ScentContext
import ai.platon.scent.analysis.corpus.annotateNodes
import ai.platon.scent.dom.HarvestOptions
import ai.platon.scent.dom.nodes.AnchorGroup
import ai.platon.scent.entities.HarvestResult
import ai.platon.scent.ml.encoding.EncodeProject
import ai.platon.scent.ql.h2.context.ScentSQLContext
import ai.platon.scent.ql.h2.context.ScentSQLContexts
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.notExists

open class VerboseHarvester(
    context: ScentContext = ScentSQLContexts.create()
): VerboseCrawler(context) {
    private val logger = LoggerFactory.getLogger(VerboseHarvester::class.java)
    private val closed = AtomicBoolean()

    val sqlContext get() = context as ScentSQLContext
    
    override val session = sqlContext.createSession()
    
    fun export(projectId: String, urls: Iterable<String>) {
        val project = EncodeProject(projectId, EncodeProject.Type.TRAINING)
        urls.forEach { url ->
            val page = session.load(url)
            session.exportTo(page, project.htmlBaseDir)
        }
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
    
    fun harvest(projectId: String, start: Int = 0, limit: Int = Int.MAX_VALUE) {
        val args2 = "-projectId $projectId -diagnose -vj -trustSamples"
//        val args2 = "$args -vj -trustSamples"
        val options = session.options(args2)
        
        val encodeProject = EncodeProject(projectId, EncodeProject.Type.TRAINING)
        val documents = loadDocuments(encodeProject.htmlBaseDir, start, limit)
        
        documents.chunked(200).forEach { chunk ->
            harvest1(chunk.asSequence(), options)
        }
    }
    
    fun harvest(portalUrl: String, args: String) = harvest(portalUrl, session.options(args))
    
    fun harvest(documents: List<FeaturedDocument>, options: HarvestOptions): HarvestResult {
        val result = runBlocking { session.harvest(documents.asSequence(), options) }
        report(result, options)
        return result
    }
    
    fun harvest(portalUrl: String, options: HarvestOptions): HarvestResult {
        val result = runBlocking {
            if (options.isDefault("-topLinks")) {
                options.topLinks = 40
            }
            session.harvest(portalUrl, options)
        }
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
    
    private fun harvest1(document: Sequence<FeaturedDocument>, options: HarvestOptions) {
        runCatching { harvest(document, options) }.onFailure { warnUnexpected(this, it) }
    }
    
    private fun loadDocuments(htmlBaseDir: Path, start: Int, limit: Int): Sequence<FeaturedDocument> {
        val count = when {
            htmlBaseDir.notExists() -> 0
            else -> Files.list(htmlBaseDir).filter { it.fileName.toFile().endsWith("htm") }.count()
        }
        if (count < 20) {
            logger.warn("Too few samples, might not generate a good result")
        }
        
        val documents = htmlBaseDir.listDirectoryEntries("*.htm")
            .asSequence()
            .drop(start)
            .take(limit)
            .map { Documents.parse(it, "UTF-8", it.toString()) }
            .onEach { it.document.setBaseUri(it.normalizedURI ?: it.baseURI) }
        
        return documents
    }
    
    override fun close() {
        if (closed.compareAndSet(false, true)) {
        }
    }
}
