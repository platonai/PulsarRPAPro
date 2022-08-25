package ai.platon.exotic.standalone.common

import ai.platon.exotic.driver.common.ExoticUtils
import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.sql.ResultSetFormatter
import ai.platon.pulsar.common.urls.UrlUtils
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.dom.nodes.node.ext.canonicalName
import ai.platon.pulsar.ql.ResultSets
import ai.platon.scent.ScentContext
import ai.platon.scent.ScentSession
import ai.platon.scent.context.ScentContexts
import ai.platon.scent.dom.HNormUrl
import ai.platon.scent.dom.HarvestOptions
import ai.platon.scent.dom.nodes.AnchorGroup
import ai.platon.scent.dom.nodes.annotateNodes
import ai.platon.scent.entities.HarvestResult
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.StringUtils
import org.h2.tools.SimpleResultSet
import org.h2.value.DataType
import org.h2.value.Value
import org.nield.kotlinstatistics.standardDeviation
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.util.*

open class VerboseHarvester(
    context: ScentContext = ScentContexts.create()
) {
    private val logger = LoggerFactory.getLogger(VerboseHarvester::class.java)

    val session: ScentSession = context.createSession()

    val defaultArgs = "" +
//            " -expires 1d" +
//            " -itemExpires 1d" +
//                " -scrollCount 6" +
//                " -itemScrollCount 4" +
            " -topLinks 40" +
            " -nScreens 5" +
//                " -polysemous" +
//            " -diagnose" +
            " -nVerbose 3" +
//                " -pageLoadTimeout 60s" +
            " -showTip" +
//            " -showImage" +
//                " -cellType PLAIN_TEXT" +
            ""

    fun arrangeLinks(portalUrl: String): SortedSet<AnchorGroup> {
        logger.info("Arranging links in page $portalUrl")
        val normUrl = HNormUrl.parse(portalUrl, session.sessionConfig.toVolatileConfig())
        val doc = session.load(portalUrl).let { session.parse(it) }
        doc.also { it.annotateNodes(normUrl.hOptions) }.also { session.export(it) }
        return session.arrangeLinks(normUrl, doc)
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

    fun harvest(url: String) = harvest(url, defaultArgs)

    fun harvest(url: String, args: String) = harvest(url, session.options(args))

    fun harvest(url: String, options: HarvestOptions) = harvest(session, url, options)

    fun harvest(documents: List<FeaturedDocument>, options: HarvestOptions): HarvestResult {
        val result = runBlocking { session.harvest(documents, options) }
        report(result, options)
        return result
    }

    fun harvest(session: ScentSession, portalUrl: String, options: HarvestOptions) {
        val (url0, args0) = UrlUtils.splitUrlArgs(portalUrl)
        val options0 = session.options("$options $args0")
        options0.topLinks = options0.topLinks.coerceAtLeast(40)
        val result = runBlocking { session.harvest(url0, options0) }
        report(result, options)
    }

    private fun report(result: HarvestResult, options: HarvestOptions) {
        val exports = session.buildAll(result.tableGroup, options)

        // table view
        val builder = ExoticPageTableViewBuilder(result.tableGroup, devMode = options.diagnose)
        val doc = builder.build()

        var path = result.tableGroup.tableViewPath
        doc.document.setBaseUri(path.toString())
        Files.createDirectories(path.parent)
        session.exportTo(doc, path)

        path = AppPaths.REPORT_DIR.resolve("harvest/corpus/last-page-tables.htm")
        doc.document.setBaseUri(path.toString())
        session.exportTo(doc, path)

        // json view
        val json = session.buildJson(result.tableGroup)
        path = AppPaths.REPORT_DIR.resolve("harvest/corpus/last-page-tables.json")
        Files.writeString(path, json)

        logger.info("Harvest reports: {}", path.parent)
        exports.keys.map { it.toString() }
            .filter { it.matches(".+/tables/.+".toRegex()) }
            .forEach { ExoticUtils.openBrowser(it) }
    }

    private fun toReport(i: Int, group: AnchorGroup): String {
        val component = group.component
        val e = component?.element?:return ""
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
