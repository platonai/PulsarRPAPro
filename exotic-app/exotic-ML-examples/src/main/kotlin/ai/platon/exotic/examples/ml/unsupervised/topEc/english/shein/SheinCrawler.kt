package ai.platon.exotic.examples.ml.unsupervised.topEc.english.shein

import ai.platon.exotic.crawl.common.VerboseCrawler1

fun main() {
    val portalUrl = "https://us.shein.com/New-in-Trends-sc-00654187.html"
    val args = "-i 1s -ii 5d -ol a[href~=-cat-] -ignoreFailure" +
            " -component .product-intro__info" +
            " -component .product-intro__head"
    VerboseCrawler1().harvest(portalUrl, args)
}
