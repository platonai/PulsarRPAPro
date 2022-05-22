package ai.platon.exotic.standalone.common

import ai.platon.exotic.driver.common.ExoticUtils
import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.sql.ResultSetFormatter
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.scent.ScentContext
import ai.platon.scent.ScentSession
import ai.platon.scent.context.ScentContexts
import ai.platon.scent.dom.HNormUrl
import ai.platon.scent.dom.HarvestOptions
import ai.platon.scent.dom.nodes.AnchorGroup
import ai.platon.scent.dom.nodes.annotateNodes
import ai.platon.scent.entities.HarvestResult
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

open class VerboseHarvester(
    context: ScentContext = ScentContexts.create()
) {

    private val logger = LoggerFactory.getLogger(VerboseHarvester::class.java)

    val session: ScentSession = context.createSession()

    val defaultArgs = "" +
            " -expires 1d" +
            " -itemExpires 1d" +
//                " -scrollCount 6" +
//                " -itemScrollCount 4" +
            " -nScreens 5" +
//                " -polysemous" +
            " -diagnose" +
            " -nVerbose 1" +
            " -preferParallel false" +
//                " -pageLoadTimeout 60s" +
            " -showTip" +
            " -showImage" +
//                " -cellType PLAIN_TEXT" +
            ""

    fun arrangeLinks(portalUrl: String): SortedSet<AnchorGroup> {
        logger.info("Arranging links in page $portalUrl")
        val normUrl = HNormUrl.parse(portalUrl, session.sessionConfig.toVolatileConfig())
        val doc = session.load(portalUrl).let { session.parse(it) }
        doc.also { it.annotateNodes(normUrl.hOptions) }.also { session.export(it) }
        return session.arrangeLinks(normUrl, doc)
    }

    fun harvest(url: String) = harvest(url, defaultArgs)

    fun harvest(url: String, args: String) = harvest(url, session.options(args))

    fun harvest(url: String, options: HarvestOptions) = harvest(session, url, options)

    fun harvest(documents: List<FeaturedDocument>, options: HarvestOptions): HarvestResult {
        val result = runBlocking { session.harvest(documents, options) }
        report(result, options)
        return result
    }

    fun harvest(session: ScentSession, url: String, options: HarvestOptions) {
        val result = runBlocking { session.harvest(url, options) }
        val exports = session.buildAll(result.tableGroup, options)

        val json = session.buildJson(result.tableGroup)
        val baseDir = AppPaths.REPORT_DIR.resolve("harvest/corpus/")
        Files.createDirectories(baseDir)
        val path = baseDir.resolve("last-page-tables.json")
        Files.writeString(path, json)

        logger.info("Harvest reports: {}", path.parent)
        exports.keys.map { it.toString() }
            .filter { it.matches(".+/tables/.+".toRegex()) }
            .forEach { ExoticUtils.openBrowser(it) }
    }

    private fun report(result: HarvestResult, options: HarvestOptions) {
        session.buildAll(result.tableGroup, options)

        val json = session.buildJson(result.tableGroup)
        val path = AppPaths.REPORT_DIR.resolve("harvest/corpus/last-page-tables.json")
        Files.createDirectories(path.parent)
        Files.writeString(path, json)

        logger.info("Harvest result: file://${path.parent}")
    }
}
