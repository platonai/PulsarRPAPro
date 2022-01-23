package ai.platon.scent.examples.sites.dangdang

import ai.platon.pulsar.common.Systems
import ai.platon.scent.context.withContext
import ai.platon.scent.examples.common.VerboseCrawler
import java.util.concurrent.atomic.AtomicInteger

fun main() {
    Systems.loadAllProperties("config/sites/amazon/other/application-p1d1.properties")
    val seeds = arrayOf(
            "http://category.dangdang.com/cid4004279.html"
    )
    val args = "-i 1s -ii 1s -ol a[href~=product] -tl 100"

    withContext { cx ->
        val index = AtomicInteger()
        IntRange(0, seeds.size)
                .map { VerboseCrawler(cx) }.parallelStream().forEach { crawler ->
                    val j = index.getAndIncrement().coerceAtMost(seeds.size - 1)
                    val seed = seeds[j]
                    crawler.loadOutPages(seed, args)
                }

    }
}
