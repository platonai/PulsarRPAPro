package ai.platon.exotic.examples.ml.unsupervised.topEc.chinese.dangdang

import ai.platon.exotic.crawl.common.VerboseHarvester

fun main() {
    val portalUrl = "http://category.dangdang.com/cid4010209.html"
    val args = "-i 1s -ii 5d -ol a[href~=product] -ignoreFailure"

    VerboseHarvester().harvest(portalUrl, args)
}
