package ai.platon.exotic.examples.sites.topEc.chinese.dangdang

import ai.platon.exotic.examples.common.VerboseHarvester

fun main() {
    val portalUrl = "http://category.dangdang.com/cid4010209.html"
    val args = "-i 1s -ii 5d -ol a[href~=product] -ignoreFailure"

    VerboseHarvester().harvest(portalUrl, args)
}
