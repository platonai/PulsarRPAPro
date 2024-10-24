package ai.platon.exotic.examples.ml.supervised

import ai.platon.exotic.crawl.common.AmazonAsinUrlNormalizer
import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.dom.nodes.node.ext.isRegularText
import ai.platon.pulsar.persist.gora.generated.GWebPage
import ai.platon.scent.common.clearMLLabels
import ai.platon.scent.common.mlLabels
import ai.platon.scent.ml.EncodeOptions
import ai.platon.scent.tools.VerboseCrawler
import java.lang.management.ManagementFactory
import java.nio.file.Files
import java.time.MonthDay

/**
 * Scan webpages in a directory and run an un-supervised ML algorithm.
 *
 * When corpus size > 1000, add VM flags: -Xmx6g -Xms2G
 * When corpus size > 2000, add VM flags: -Xmx10g -Xms2G
 *
 * The flag Xmx specifies the maximum memory allocation pool for a Java Virtual Machine (JVM),
 * while Xms specifies the initial memory allocation pool.
 *
 * The best way to run the program is using command line:
 *
 * java -Xmx10g -Xms2G -cp scent-examples-1.12.0-SNAPSHOT.jar \
 * -D"loader.main=ai.platon.scent.examples.sites.amazon.AmazonScanHarvesterKt" \
 * org.springframework.boot.loader.PropertiesLauncher -task harvest -limit 300
 * */
class AmazonMLRunner(
    val start: Int,
    val limit: Int
) {
    private val logger = getLogger(AmazonMLRunner::class)
    private val urlBase = "https://www.amazon.com/dp/"
    private val crawler = VerboseCrawler()
    private val session = crawler.session
    private val monthValue = MonthDay.now().monthValue
    private val dayOfMonth = MonthDay.now().dayOfMonth
    private val datasetPath = AppPaths.getTmp("ml/amazon.dataset.$monthValue.$dayOfMonth.$start-$limit.csv")

    // val labels = listOf("stars", "stars_text", "ratings", "qas", "price_text", "brand")

    init {
        session.context.urlNormalizer.add(AmazonAsinUrlNormalizer())
    }
    
    fun check() {
        val fields = (GWebPage.Field.entries.toSet() - GWebPage.Field.PAGE_MODEL).map { it.toString() }.toTypedArray()
        val pages = session.context.webDb.scan(urlBase, fields)
        pages.asSequence().filter { it.contentLength > 800_000 }.drop(start).take(limit).forEachIndexed { i, page ->
            println("$i. ${page.contentLength} | ${page.mlLabels?.values} | ${page.url}")
            val document = session.parse(page)
            println(document)
        }
    }
    
    fun clearAnnotations() {
        val pages = session.context.webDb.scan(urlBase)
        pages.asSequence().drop(start).take(limit).forEachIndexed { i, page ->
            page.clearMLLabels()
            session.persist(page)
        }
    }
    
    fun encode(args: String, annotated: Boolean = false) {
        Files.deleteIfExists(datasetPath)
        
        val options = session.options(args)
        val rootElements = session.scan(urlBase, options, start, limit, start, limit)
            .filter { !annotated || it.mlLabels?.isNotEmpty() == true }
            .map { session.parse(it, options) }
            .mapNotNull { it.selectFirstOrNull("#ppd") }

        // val encodeOptions = EncodeOptions(labels, datasetPath, nGram = 1, nodeType = 1, textStrategy = 1)
        val encodeOptions = EncodeOptions(datasetPath)
//        crawler.encodeElements(rootElements.asIterable(), encodeOptions)
        session.encodeForElements(rootElements.asIterable(), encodeOptions) {
            it.isRegularText
        }
        
        println("Dataset is exported | $datasetPath")
        println("All done.")
    }
    
    fun harvest(args: String = "") {
        val args2 = "$args -diagnose -vj -trustSamples -showCombinedTable -minimalColumnCount 3"
        val options = session.options(args2)
        
        // scan with start/limit seems not working
        val documents = session.scan(urlBase, options, start, limit, start, limit)
            .map { session.parse(it, options) }
        val runtimeMxBean = ManagementFactory.getRuntimeMXBean()
        println(runtimeMxBean.inputArguments)
        
        crawler.harvest(documents, options)
        
        // labeling & encoding
    }
}

fun main(args: Array<String>) {
    // Keep consistent with spring environment
//    System.setProperty(CapabilityTypes.STORAGE_CRAWL_ID, "pulsar_rpa_tmp")

    var task = "check"
    var start = 0
    var limit = 30000
    var url = "https://www.amazon.com/dp/B0C1H26C46"

    var i = 0
    val remainders = mutableListOf<String>()
    while (i < args.size) {
        when {
            args[i] == "-task" -> task = args[++i]
            args[i] == "-start" -> start = args[++i].toIntOrNull() ?: start
            args[i] == "-limit" -> limit = args[++i].toIntOrNull() ?: limit
            args[i] == "-url" -> url = args[++i]
            else -> remainders.add(args[i])
        }
        ++i
    }

    val harvester = AmazonMLRunner(start, limit)
    var args2 = remainders.joinToString(" ")
//    args2 += " -component #ppd"
    when (task) {
        "check" -> harvester.check()
        "harvest" -> harvester.harvest(args2)
        "encode" -> harvester.encode(args2)
        "encodeAnnotated" -> harvester.encode(args2, true)
        "clearAnnotations" -> harvester.clearAnnotations()
    }
}
