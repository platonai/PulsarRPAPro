package ai.platon.exotic.examples.ml.unsupervised.topEc.chinese.suning

import ai.platon.exotic.crawl.common.VerboseCrawler1

fun main() {
    val portalUrl = "https://search.suning.com/微单/&zw=0?safp=d488778a.shuma.44811515285.1"
    val args = "-i 1s -ii 5d -ol a[href~=item] -ignoreFailure"

    VerboseCrawler1().harvest(portalUrl, args)
}
