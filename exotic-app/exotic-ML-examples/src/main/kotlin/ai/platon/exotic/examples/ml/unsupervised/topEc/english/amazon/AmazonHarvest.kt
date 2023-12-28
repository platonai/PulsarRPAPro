package ai.platon.exotic.examples.ml.unsupervised.topEc.english.amazon

import ai.platon.exotic.crawl.common.VerboseCrawler1

fun main() {
    val portalUrl = "https://www.amazon.com/b?node=1292115011"
    val args = "-i 7d -ii 1000d -itemRequireSize 1000000 -ol a[href~=/dp/] -ignoreFailure -tl 60"

    VerboseCrawler1().harvest(portalUrl, args)
}
