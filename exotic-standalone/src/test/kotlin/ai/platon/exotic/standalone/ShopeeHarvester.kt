package ai.platon.exotic.standalone

import ai.platon.exotic.crawl.common.VerboseHarvester

fun main() {
    val portalUrl = "https://shopee.sg/Computers-Peripherals-cat.11013247"
    val args = "-ol a[href~=sp_atk] -tl 20 -ignoreFailure" +
//            " -itemRequireSize 200000 -itemScrollCount 30"
            " -component .page-product__breadcrumb" +
            " -component .product-briefing" +
            " -diagnose"

    VerboseHarvester().harvest(portalUrl, args)
}
