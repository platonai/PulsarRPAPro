package ai.platon.exotic.examples.sites.topEc.english.amazon

import ai.platon.exotic.examples.common.VerboseHarvester

fun main() {
    val portalUrl = "https://www.amazon.com/b?node=1292115011"
    val args = "-i 1d -ii 7d -itemRequireSize 300000 -ol a[href~=/dp/] -ignoreFailure -tl 40"

    VerboseHarvester().harvest(portalUrl, args)
}
