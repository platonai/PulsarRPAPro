package ai.platon.scent.examples.sites.dangdang

import ai.platon.scent.context.withContext
import ai.platon.scent.examples.common.WebHarvester

fun main() = withContext {
    WebHarvester(it).harvest("http://category.dangdang.com/cid4004279.html", "-i 1s -ii 1s -ol a[href~=product]")
}
