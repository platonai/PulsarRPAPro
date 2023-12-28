package ai.platon.exotic.examples.ml.unsupervised.topEc.chinese.gome

import ai.platon.exotic.crawl.common.VerboseCrawler1
import ai.platon.scent.ScentEnvironment

fun main() {
    val portalUrl = "https://list.gome.com.cn/cat10000092.html"
    val args = "-i 10d -ii 500d -ol a[href~=item] -ignoreFailure"
    ScentEnvironment().checkEnvironment()
    val harvester = VerboseCrawler1()
    harvester.harvest(portalUrl, args)
}
