package ai.platon.scent.examples.sites.autohome

import ai.platon.scent.context.withContext
import ai.platon.scent.examples.common.VerboseCrawler

fun main() {
    val seed = "https://mall.autohome.com.cn/list/0-0-33-0-0-0-0-0-0-1.html"
    val args = "-i 1s -ii 100d -ol a[href~=item] -tl 100"

    withContext {
        val crawler = VerboseCrawler(it)
        repeat(10) {
            crawler.loadOutPages(seed, args)
        }
    }
}
