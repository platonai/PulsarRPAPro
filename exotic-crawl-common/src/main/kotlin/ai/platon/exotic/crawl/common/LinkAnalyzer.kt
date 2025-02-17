package ai.platon.exotic.crawl.common

import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.sql.ResultSetFormatter
import ai.platon.pulsar.common.urls.UrlUtils
import ai.platon.pulsar.dom.nodes.node.ext.ExportPaths
import ai.platon.pulsar.dom.nodes.node.ext.canonicalName
import ai.platon.scent.skeleton.ScentContext
import ai.platon.scent.analysis.corpus.annotateNodes
import ai.platon.scent.dom.HNormUrl
import ai.platon.scent.dom.HarvestOptions
import ai.platon.scent.dom.nodes.AnchorGroup
import ai.platon.scent.ql.h2.context.ScentSQLContexts
import org.apache.commons.lang3.StringUtils
import org.nield.kotlinstatistics.standardDeviation
import java.util.*

class LinkAnalyzer {
    private val logger = getLogger(this)
    
    val context: ScentContext = ScentSQLContexts.create()
    val session = context.createSession()
    
    val defaultArgs = "" +
        " -scrollCount 6" +
        " -itemScrollCount 4" +
        " -i 10d -ii 10000d" +
        " -requireSize 10000 -itemRequireSize 200000" +
        " -topLinks 100" +
        " -ignoreFailure" +
        ""
    
    fun arrangeLinks(portalUrl: String, args: String): SortedSet<AnchorGroup> {
        val normUrl = session.normalize(portalUrl, session.options(args))
        val portalPage = session.load(normUrl)
        val portalDocument = session.parse(portalPage)
        return session.arrangeLinks(normUrl, portalDocument)
    }
    
    fun arrangeLinks(portalUrl: String): SortedSet<AnchorGroup> {
        logger.info("Arranging links in page $portalUrl")
        val normUrl = HNormUrl.parse(portalUrl, session.sessionConfig.toVolatileConfig())
        val doc = session.load(portalUrl).let { session.parse(it) }
        doc.also { it.annotateNodes(normUrl.hOptions) }.also { session.export(it) }
        return session.arrangeLinks(normUrl, doc)
    }
    
    fun arrangeDocument(portalUrl: String) {
        val taskName = AppPaths.fromUri(portalUrl)
        
        val options = session.options(defaultArgs) as HarvestOptions
        val normUrl = session.normalize(portalUrl, options)
        val portalPage = session.load(normUrl)
        val portalDocument = session.parse(portalPage)
        val anchorGroups = session.arrangeLinks(normUrl, portalDocument)
        logger.info("------------------------------")
        anchorGroups.take(1).forEach {
            it.urlStrings.shuffled().take(10).forEachIndexed { i, url -> println("${1 + i}.\t$url") }
            it.urlStrings.take(options.topLinks)
                .parallelStream()
                .filter { UrlUtils.isStandard(it) }
                .map { session.load(it, options) }
                .map { session.parse(it, options) }
                .toList()
                .let { session.arrangeDocuments(normUrl, portalPage, it.asSequence()) }
        }
        
        portalDocument.also { it.annotateNodes(options) }.also { session.export(it, type = ExportPaths.Type.PORTAL) }
    }
    
    fun printAnchorGroups(anchorGroups: Collection<AnchorGroup>, showBestGroups: Boolean = false) {
        if (anchorGroups.isEmpty()) {
            return
        }
        
        println(anchorGroups.first().urlStrings.first())
        println(ResultSetFormatter(AnchorGroup.toResultSet(anchorGroups), withHeader = true))
        if (showBestGroups) {
            println("The urls in the best group: ")
            anchorGroups.first().anchorSpecs.forEachIndexed { i, anchorSpec ->
                println("${i.inc()}.\t${anchorSpec.url}")
            }
        }
    }
    
    fun printAllAnchorGroups(anchorGroups: Collection<AnchorGroup>) {
        if (anchorGroups.isEmpty()) {
            return
        }
        
        println(anchorGroups.first().urlStrings.first())
        println(ResultSetFormatter(AnchorGroup.toResultSet(anchorGroups), withHeader = true))
        anchorGroups.forEachIndexed { i, group ->
            println()
            println(toReport(i, group))
            group.anchorSpecs.forEachIndexed { j, anchorSpec ->
                println("${j.inc()}.\t${anchorSpec.url}")
            }
        }
    }
    
    private fun toReport(i: Int, group: AnchorGroup): String {
        val component = group.component
        val e = component?.element ?: return ""
        val name = StringUtils.abbreviateMiddle(e.canonicalName, "..", 40)
        val label = StringUtils.abbreviateMiddle(group.label, "..", 30)
        val lengthStd = group.urlStrings.map { it.length }.standardDeviation().toInt()
        val score = String.format("%s", group.score.toString())
        // very slow
        val distortion = String.format("%.4f", group.distortion)
        val path = group.path
        
        return """
            ${i.inc()}.
            Group: ${group.id}
            Label: $label
            Name: $name
            Depth: ${group.depth}
            Size: ${group.size}
            LengthStd: $lengthStd
            Score: $score
            Distortion: $distortion
            Path: $path""".trimIndent()
    }
}
