package ai.platon.exotic.examples.ml.unsupervised.topEc.english.amazon

import ai.platon.exotic.crawl.common.VerboseHarvester
import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.dom.Documents
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.scent.context.ScentContexts
import java.lang.management.ManagementFactory
import java.nio.file.Files
import kotlin.streams.asSequence

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
 * java -Xmx10g -Xms2G -cp exotic-ML-examples*.jar -D"loader.main=ai.platon.exotic.examples.ml.unsupervised.topEc.english.amazon.AmazonScanHarvestKt" org.springframework.boot.loader.PropertiesLauncher -limit 2000
 * */
object AmazonScanHarvest {
    val urlBase = "https://www.amazon.com/dp/"
    val session = ScentContexts.createSession()

    fun loadFromFile(limit: Int): List<FeaturedDocument> {
        return Files.walk(AppPaths.LOCAL_STORAGE_DIR.resolve("amazon-com"))
            .filter { it.fileName.toString().contains("amazon-com") }
            .filter { it.fileName.toString().endsWith(".htm") }
            .asSequence()
            .filter { Files.size(it) > 1_000_000 } // more than 1M
            .take(limit)
            .map { Documents.parse(it, "UTF-8") }
            .filter { it.selectFirstOrNull("#productTitle") != null }
            .toList()
    }

    fun loadFromDB() {
        val options = session.options()
        // session.scan(urlBase, 100)
    }
    
    fun run(limit: Int) {
        val documents = loadFromFile(limit)
        
        if (documents.isEmpty()) {
            println("No documents to analysis")
            return
        } else {
            println("Total " + documents.size + " documents")
        }
        
        val args = " -diagnose -vj -trustSamples"
        
        val options = session.options(args)
        
        val runtimeMxBean = ManagementFactory.getRuntimeMXBean()
        println(runtimeMxBean.inputArguments)
        
        val crawler = VerboseHarvester()
        crawler.harvest(documents, options)
        
        // labeling & encoding
    }
}

fun main(args: Array<String>) {
    var limit = Int.MAX_VALUE
    
    var i = 0
    while (i < args.size) {
        if (args[i] == "limit") limit = args[++i].toIntOrNull() ?: Int.MAX_VALUE
        ++i
    }
    
    AmazonScanHarvest.run(limit)
}
