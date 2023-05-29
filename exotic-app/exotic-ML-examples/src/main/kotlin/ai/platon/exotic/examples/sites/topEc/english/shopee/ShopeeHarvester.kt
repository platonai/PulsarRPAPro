package ai.platon.exotic.examples.sites.topEc.english.shopee

import ai.platon.exotic.examples.common.VerboseHarvester

fun main() {
    val portalUrl = "https://shopee.sg/Laptops-cat.11013247.11013305"
    val args = "-ol a[href~=sp_atk] -tl 40 -ignoreFailure" +
            " -itemRequireSize 200000 -itemScrollCount 30" +
            " -diagnose -vj"

    VerboseHarvester().harvest(portalUrl, args)
}
