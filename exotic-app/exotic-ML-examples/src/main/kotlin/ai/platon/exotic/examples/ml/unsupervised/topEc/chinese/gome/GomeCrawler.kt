package ai.platon.exotic.examples.ml.unsupervised.topEc.chinese.gome

import ai.platon.exotic.crawl.common.VerboseHarvester

fun main() {
    val portalUrl = "https://list.gome.com.cn/cat10000092.html"
    val args = "-i 1s -ii 5d -ol a[href~=item] -ignoreFailure"
    VerboseHarvester().harvest(portalUrl, args)
}