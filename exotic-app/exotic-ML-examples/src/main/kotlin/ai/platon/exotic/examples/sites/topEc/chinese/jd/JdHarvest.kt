package ai.platon.exotic.examples.sites.topEc.chinese.jd

import ai.platon.exotic.examples.common.VerboseHarvester

fun main() {
    val portalUrl = "https://list.jd.com/list.html?cat=652,12345,12349"
    val args = "-requireSize 200000 -itemRequireSize 200000 -tl 60 -ol a[href~=item] -ignoreFailure"

    VerboseHarvester().harvest(portalUrl, args)
}
