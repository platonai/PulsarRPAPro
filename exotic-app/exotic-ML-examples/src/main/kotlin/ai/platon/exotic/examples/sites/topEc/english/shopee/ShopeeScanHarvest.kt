package ai.platon.exotic.examples.sites.topEc.english.shopee

import ai.platon.exotic.examples.common.VerboseHarvester
import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.persist.WebPage
import ai.platon.scent.context.ScentContexts
import ai.platon.scent.persist.SimpleAvroStore
import java.lang.management.ManagementFactory
import java.nio.file.Files
import kotlin.streams.toList

/**
 * Scan webpages in a directory and run an un-supervised ML algorithm.
 *
 * When sample size > 1000, add VM flags: -Xmx6g -Xms2G
 * When sample size > 2000, add VM flags: -Xmx10g -Xms2G
 *
 * The flag Xmx specifies the maximum memory allocation pool for a Java Virtual Machine (JVM),
 * while Xms specifies the initial memory allocation pool.
 * */
object ShopeeScanHarvest {

    fun run() {
        val session = ScentContexts.createSession()
        val store = SimpleAvroStore()
        val limit = 1000

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

fun main() {
    ShopeeScanHarvest.run()
}
