package ai.platon.exotic.examples.ml.supervised

import ai.platon.exotic.common.ExoticUtils
import ai.platon.exotic.crawl.common.AmazonAsinUrlNormalizer
import ai.platon.exotic.crawl.common.VerboseCrawler
import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.dom.nodes.node.ext.isText
import ai.platon.pulsar.persist.gora.generated.GWebPage
import ai.platon.scent.common.clearMLLabels
import ai.platon.scent.common.mlLabels
import ai.platon.scent.dom.nodes.node.ext.nthScreen
import ai.platon.scent.ml.EncodeOptions
import java.lang.management.ManagementFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers
import java.nio.file.Files


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
class AmazonScanHarvester {
    private val logger = getLogger(AmazonScanHarvester::class)
    private val urlBase = "https://www.amazon.com/dp/"
    private val crawler = VerboseCrawler()
    private val session = crawler.session
    private val datasetPath = AppPaths.getTmp("amazon.dataset.libsvm.txt")
    private val ML_SERVER_PORT = 8382
    private val predictServer = "http://localhost:$ML_SERVER_PORT/api/ml/predict"
    private val predictAPI = "$predictServer/api/ml/predict"
    private val client = HttpClient.newHttpClient()
    
    val labels = listOf("stars", "stars_text", "ratings", "qas", "price_text", "brand")

//    private val classifier = RandomForestClassifier(labels.size, datasetPath)

    init {
        session.context.urlNormalizer.add(AmazonAsinUrlNormalizer())
    }
    
    fun check(start: Int, limit: Int) {
        val fields = (GWebPage.Field.values().toSet() - GWebPage.Field.PAGE_MODEL).map { it.toString() }.toTypedArray()
        val pages = session.context.webDb.scan(urlBase, fields)
        pages.asSequence().filter { it.contentLength > 800_000 }.drop(start).take(limit).forEachIndexed { i, page ->
            println("$i. ${page.contentLength} | ${page.mlLabels?.values} | ${page.url}")
            val document = session.parse(page)
            println(document)
        }
    }
    
    fun clearAnnotations(start: Int, limit: Int) {
        val pages = session.context.webDb.scan(urlBase)
        pages.asSequence().drop(start).take(limit).forEachIndexed { i, page ->
            page.clearMLLabels()
            session.persist(page)
        }
    }
    
    fun encode(start: Int, limit: Int, args: String) {
        Files.deleteIfExists(datasetPath)
        
        val options = session.options(args)
        val documents = session.scan(urlBase, options, start, limit).map { session.parse(it, options) }
        
        val encodeOptions = EncodeOptions(datasetPath, labels = labels)
        crawler.encodeDocuments(documents.asIterable(), encodeOptions)
        
        println("Dataset is exported | $datasetPath")
        println("All done.")
    }
    
    fun harvest(start: Int, limit: Int, args: String) {
        val args2 = "$args -diagnose -vj -trustSamples"
        val options = session.options(args2)
        
        val documents = session.scan(urlBase, options, start, limit).map { session.parse(it, options) }
        val runtimeMxBean = ManagementFactory.getRuntimeMXBean()
        println(runtimeMxBean.inputArguments)
        
        crawler.harvest(documents, options)
        
        // labeling & encoding
    }

    fun predict(url: String) {
        val page = session.load(url)
        if (page.isInternal) {
            logger.warn("Failed to load page | {} | {}", page.protocolStatus, url)
            return
        }
        val document = session.parse(page)
        
        val encodeOptions = EncodeOptions(labels = labels)
        val df = session.encodeNodes(document, encodeOptions) {
            it.isText && it.nthScreen <= 2 && it.extension.immutableText.isNotBlank() }
        val points = df.points.map { ExoticUtils.encodeToLibSVMRecord(it.dataRef, -1).toString() }
        val requestBody = points.joinToString("\n")
//        val requestBody = "1 1:523.7 2:636.7 3:23.6 4:17.3 5:5 11:15 12:4559 33:5 34:5 35:5 64:23.6 65:17.3 70:523.7 71:636.7 72:23.6 73:17.3 74:5 75:1 78:3 80:14 81:4558 82:409.21 90:523.7 91:636.7 95:1 102:5 103:5 104:5 105:1 109:4558 122:23.6 124:17.3 133:23.6 134:17.3 139:523.7 140:636.7 141:119.6 142:17.3 149:14 150:4557 208:523.7 209:636.7 210:119.6 211:17.3 218:14 219:4560"
        val request = HttpRequest.newBuilder()
            .uri(URI.create(predictAPI))
            .method("POST", HttpRequest.BodyPublishers.ofString(requestBody))
            .build()
        client.sendAsync(request, BodyHandlers.ofString())
            .thenApply { it.body() }
            .thenAccept {
                if (!it.startsWith("-")) {
                    println(it)
                }
            }
            .join()
    }
}

fun main(args: Array<String>) {
    var task = "encode"
    var start = 0
    var limit = 2000
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
    
    val harvester = AmazonScanHarvester()
    val args2 = remainders.joinToString(" ")
    when(task) {
        "check" -> harvester.check(start, limit)
        "harvest" -> harvester.harvest(start, limit, args2)
        "encode" -> harvester.encode(start, limit, args2)
        "predict" -> harvester.predict(url)
        "clearAnnotations" -> harvester.clearAnnotations(start, limit)
    }
}
