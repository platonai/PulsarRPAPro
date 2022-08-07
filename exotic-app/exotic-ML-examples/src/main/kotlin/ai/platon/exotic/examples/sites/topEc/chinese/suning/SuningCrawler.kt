package ai.platon.exotic.examples.sites.topEc.chinese.suning

import ai.platon.exotic.examples.common.VerboseHarvester

fun main() {
    val portalUrl = "https://search.suning.com/微单/&zw=0?safp=d488778a.shuma.44811515285.1"
    val args = "-i 1s -ii 5d -ol a[href~=item] -ignoreFailure"

    VerboseHarvester().harvest(portalUrl, args)
}
