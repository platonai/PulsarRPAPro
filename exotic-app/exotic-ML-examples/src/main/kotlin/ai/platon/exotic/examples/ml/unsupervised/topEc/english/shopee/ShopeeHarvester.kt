package ai.platon.exotic.examples.ml.unsupervised.topEc.english.shopee

import ai.platon.exotic.crawl.common.VerboseHarvester

fun main() {
    val portalUrl = "https://shopee.sg/Laptops-cat.11013247.11013305"
    val args = "-i 1s -ii 1s -ol a[href~=sp_atk] -tl 100 -ignoreFailure" +
            " -itemRequireSize 200000 -itemScrollCount 5" +
            " -diagnose -vj"

    VerboseHarvester().harvest(portalUrl, args)
}
