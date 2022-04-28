package ai.platon.exotic.examples.common

import ai.platon.exotic.driver.common.ExoticUtils
import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.scent.ScentContext
import ai.platon.scent.ScentSession
import ai.platon.scent.context.ScentContexts
import ai.platon.scent.dom.HarvestOptions
import ai.platon.scent.dom.nodes.annotateNodes
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

open class VerboseHarvester(
    context: ScentContext = ScentContexts.create()
): VerboseCrawler(context) {

    private val logger = LoggerFactory.getLogger(VerboseHarvester::class.java)
    private val counter = AtomicInteger(0)
    private val taskTimes = ConcurrentHashMap<String, Duration>()

    override val session: ScentSession = context.createSession()

    init {
        System.setProperty(CapabilityTypes.BROWSER_IMAGES_ENABLED, "true")
        // BrowserSettings.pageLoadStrategy = "normal"
    }

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

    fun arrangeDocument(portalUrl: String) {
        val taskName = AppPaths.fromUri(portalUrl)

        val normUrl = session.normalize(portalUrl, session.options(defaultArgs))
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
                    .let { session.arrangeDocuments(normUrl, portalPage, it) }
        }

        portalDocument.also { it.annotateNodes(options) }.also { session.export(it, type = "portal") }
    }

    fun harvest(url: String) = harvest(url, defaultArgs)

    fun harvest(url: String, args: String) = harvest(url, session.options(args))

    fun harvest(url: String, options: HarvestOptions) = harvest(session, url, options)

    fun harvest(session: ScentSession, url: String, options: HarvestOptions) {
        val start = Instant.now()

        val result = runBlocking { session.harvest(url, options) }
        val exports = session.buildAll(result.tableGroup, options)

        val json = session.buildJson(result.tableGroup)
        val baseDir = AppPaths.REPORT_DIR.resolve("harvest/corpus/")
        val path = baseDir.resolve("last-page-tables.json")
        Files.writeString(path, json)

        taskTimes[url] = Duration.between(start, Instant.now())

        logger.info("Harvest reports: {}", path.parent)
        exports.keys.map { it.toString() }
            .filter { it.matches(".+/tables/.+".toRegex()) }
            .forEach { ExoticUtils.openBrowser(it) }
    }
}
