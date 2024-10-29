package ai.platon.exotic.examples.ml.unsupervised.topEc.english.amazon

import ai.platon.exotic.crawl.common.VerboseHarvester

fun main() {
    val portalUrl = "https://www.amazon.com/Best-Sellers/zgbs"

    val harvester = VerboseHarvester()
    val anchorGroups = VerboseHarvester().arrangeLinks(portalUrl)
    harvester.printAnchorGroups(anchorGroups, true)
}
