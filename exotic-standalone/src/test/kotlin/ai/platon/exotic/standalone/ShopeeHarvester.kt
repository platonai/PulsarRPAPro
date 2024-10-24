package ai.platon.exotic.standalone

import ai.platon.exotic.crawl.common.VerboseCrawler1

fun main() {
    val portalUrl = "https://www.amazon.com/b?node=1292115011"
    val args = "-ol a[href~=sp_atk] -tl 20 -ignoreFailure" +
//            " -itemRequireSize 200000 -itemScrollCount 30"
            " -component .page-product__breadcrumb" +
            " -component .product-briefing" +
            " -diagnose"

    VerboseCrawler1().harvest(portalUrl, args)
}
