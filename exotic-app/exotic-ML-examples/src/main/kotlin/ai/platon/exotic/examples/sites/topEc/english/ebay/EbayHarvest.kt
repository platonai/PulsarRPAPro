package ai.platon.exotic.examples.sites.topEc.english.ebay

import ai.platon.exotic.examples.common.VerboseHarvester

fun main() {
    val portalUrl = "https://www.ebay.com/b/Dolce-Gabbana-Bags-Handbags-for-Women/169291/bn_716146"
    val args = "-i 1s -ii 5d -ol a[href~=itm] -ignoreFailure"

    VerboseHarvester().harvest(portalUrl, args)
}
