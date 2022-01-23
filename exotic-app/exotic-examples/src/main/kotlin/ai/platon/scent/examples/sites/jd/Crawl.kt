package ai.platon.scent.examples.sites.jd

import ai.platon.pulsar.common.Systems
import ai.platon.scent.context.withContext
import ai.platon.scent.examples.common.VerboseCrawler
import java.util.concurrent.atomic.AtomicInteger

fun main() {
    val seeds = arrayOf(
            "https://list.jd.com/list.html?cat=652,12345,12349",
            "https://list.jd.com/list.html?cat=652,829",
            "https://list.jd.com/list.html?cat=652,16951"
//            "https://list.jd.com/list.html?cat=652,654",
//            "https://list.jd.com/list.html?cat=652,12345"
    )
    val args = "-i 1s -ii 1s -ol a[href~=item] -tl 100"

    withContext { cx ->
        val index = AtomicInteger()
        IntRange(0, seeds.size)
                .map { VerboseCrawler(cx) }
                .parallelStream().forEach { crawler ->
                    val j = index.getAndIncrement().coerceAtMost(seeds.size - 1)
                    val seed = seeds[j]
                    repeat(10) {
                        crawler.loadOutPages(seed, args)
                    }
                }
    }
}
