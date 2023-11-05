package ai.platon.exotic.examples.ml.sites.topEc.english.shopee

import ai.platon.exotic.examples.ml.common.VerboseHarvester
import ai.platon.pulsar.browser.common.BrowserSettings

fun main() {
    val portalUrl = "https://shopee.sg/Laptops-cat.11013247.11013305"
    val args = "-i 1s -ii 1s -ol a[href~=sp_atk] -tl 100 -ignoreFailure" +
            " -itemRequireSize 200000 -itemScrollCount 5" +
            " -diagnose -vj"

    VerboseHarvester().harvest(portalUrl, args)
    
    readlnOrNull()
}
