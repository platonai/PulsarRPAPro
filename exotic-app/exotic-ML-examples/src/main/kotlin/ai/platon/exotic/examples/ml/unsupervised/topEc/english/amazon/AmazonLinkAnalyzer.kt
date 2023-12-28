package ai.platon.exotic.examples.ml.unsupervised.topEc.english.amazon

import ai.platon.exotic.crawl.common.VerboseCrawler1

fun main() {
    val portalUrl = "https://www.amazon.com/Best-Sellers/zgbs"

    val harvester = VerboseCrawler1()
    val anchorGroups = VerboseCrawler1().arrangeLinks(portalUrl)
    harvester.printAnchorGroups(anchorGroups, true)
}
