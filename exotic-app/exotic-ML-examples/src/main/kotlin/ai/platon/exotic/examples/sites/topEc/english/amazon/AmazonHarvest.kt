package ai.platon.exotic.examples.sites.topEc.english.amazon

import ai.platon.exotic.examples.common.VerboseHarvester

fun main() {
    val portalUrl = "https://www.amazon.com/Best-Sellers/zgbs"
    val args = "-i 1s -ii 5s -itemRequireSize 300000 -ol a[href~=/dp/] -ignoreFailure"

    VerboseHarvester().harvest(portalUrl, args)
}
