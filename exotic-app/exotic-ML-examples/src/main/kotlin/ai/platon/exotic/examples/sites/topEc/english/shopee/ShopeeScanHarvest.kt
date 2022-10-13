package ai.platon.exotic.examples.sites.topEc.english.shopee

import ai.platon.exotic.examples.common.VerboseHarvester
import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.gora.FileBackendPageStore
import ai.platon.scent.context.ScentContexts
import java.lang.management.ManagementFactory
import java.nio.file.Files
import kotlin.streams.toList

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
 * java -Xmx10g -Xms2G -cp exotic-ML-examples*.jar -D"loader.main=ai.platon.exotic.examples.sites.topEc.english.shopee.ShopeeScanHarvestKt" org.springframework.boot.loader.PropertiesLauncher -limit 2000
 * */
object ShopeeScanHarvest {
    val session = ScentContexts.createSession()
    val store = FileBackendPageStore()

    fun run(limit: Int) {
        println("Limit: $limit")

        val documents = Files.list(AppPaths.LOCAL_STORAGE_DIR)
            .filter { it.toString().contains("shopee-sg") }
            .filter { it.toString().endsWith(".avro") }
            .toList()
            .mapNotNull { store.readAvro(it) }
            .take(limit)
            .map { session.parse(WebPage.box(it.baseUrl.toString(), it, session.sessionConfig.toVolatileConfig())) }
            .filter { it.selectFirstOrNull(".page-product__breadcrumb") != null }

        if (documents.isEmpty()) {
            println("No documents to analysis")
            return
        } else {
            println("Total " + documents.size + " documents")
        }

        val args = " -component .page-product__breadcrumb" +
                " -component .product-briefing" +
                " -diagnose -vj -trustSamples"

        val options = session.options(args)

        val runtimeMxBean = ManagementFactory.getRuntimeMXBean()
        println(runtimeMxBean.inputArguments)

        val crawler = VerboseHarvester()
        crawler.harvest(documents, options)
    }
}

fun main(args: Array<String>) {
    var limit = Int.MAX_VALUE

    var i = 0
    while (i < args.size) {
        if (args[i] == "limit") limit = args[++i].toIntOrNull() ?: Int.MAX_VALUE
        ++i
    }

    ShopeeScanHarvest.run(limit)
}
