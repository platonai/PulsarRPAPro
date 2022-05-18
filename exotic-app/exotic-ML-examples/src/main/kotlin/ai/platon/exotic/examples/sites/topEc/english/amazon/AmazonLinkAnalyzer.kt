package ai.platon.exotic.examples.sites.topEc.english.amazon

import ai.platon.exotic.examples.common.VerboseHarvester

fun main() {
    val portalUrl = "https://www.amazon.com/Best-Sellers/zgbs"

    val harvester = VerboseHarvester()
    val anchorGroups = VerboseHarvester().arrangeLinks(portalUrl)
    harvester.printAnchorGroups(anchorGroups, true)
}
