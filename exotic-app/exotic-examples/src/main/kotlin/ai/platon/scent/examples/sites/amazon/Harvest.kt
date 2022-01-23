package ai.platon.scent.examples.sites.amazon

import ai.platon.scent.context.withContext
import ai.platon.scent.examples.common.WebHarvester

private const val asinBestUrl = "https://www.amazon.com/Best-Sellers-Automotive/zgbs/automotive/ref=zg_bs_nav_0"
private const val asinCategory = "https://www.amazon.com/gp/bestsellers/electronics/565108"
private const val args = "-i 1d -ii 100d -irs 500000 -tl 100 -diagnose -vj"

fun main() = withContext {
    WebHarvester(it).harvest(asinCategory, "-i 1s")
}
