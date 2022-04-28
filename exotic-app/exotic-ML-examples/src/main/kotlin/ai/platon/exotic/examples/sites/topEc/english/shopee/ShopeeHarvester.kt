package ai.platon.exotic.examples.sites.topEc.english.shopee

import ai.platon.exotic.examples.common.VerboseHarvester

fun main() {
    val portalUrl = "https://shopee.sg/Computers-Peripherals-cat.11013247"
    val args = "-i 1s -ii 1d -ol a[href~=sp_atk] -tl 20 -ignoreFailure"

    val harvester = VerboseHarvester()
    harvester.harvest(portalUrl, args)
}
