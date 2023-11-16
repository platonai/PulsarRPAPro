package ai.platon.exotic.examples.ml.supervised

//import ai.platon.exotic.ml.RandomForestClassifier
import ai.platon.exotic.crawl.common.AmazonAsinUrlNormalizer
import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.persist.gora.generated.GWebPage
import ai.platon.scent.common.clearMLLabels
import ai.platon.scent.common.mlLabels
import ai.platon.scent.ml.EncodeOptions
import ai.platon.scent.tools.VerboseCrawler
import java.lang.management.ManagementFactory
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
    private val urlBase = "https://www.amazon.com/dp/"
    private val crawler = VerboseCrawler()
    private val session = crawler.session
    private val datasetPath = AppPaths.getTmp("amazon.dataset.libsvm.txt")
    
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
        
        val encodeOptions = EncodeOptions(labels, datasetPath)
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
    
//    fun train() {
//        classifier.train()
//    }
//
//    fun predict(url: String) {
//        val document = session.loadDocument(url)
//        val encodeOptions = EncodeOptions(labels = labels)
//        val df = session.encodeNodes(document, encodeOptions) { it.isText && it.nthScreen <= 2 }
//        df.points.forEach { point ->
//            classifier.predict(point.dataRef)
//        }
//    }
}

fun main(args: Array<String>) {
    var task = "train"
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
//        "train" -> harvester.train()
//        "predict" -> harvester.predict(url)
        "clearAnnotations" -> harvester.clearAnnotations(start, limit)
    }
}
