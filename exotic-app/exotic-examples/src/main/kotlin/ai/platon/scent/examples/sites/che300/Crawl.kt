package ai.platon.scent.examples.sites.che300

import ai.platon.pulsar.browser.driver.BrowserSettings
import ai.platon.scent.examples.common.VerboseCrawler
import ai.platon.scent.ql.h2.context.withSQLContext

fun main() {
    val seed = "https://www.che300.com/buycar?rt=1"
    val args = "-i 1s -ii 1s -ol a[href~=buycar/x] -tl 100"

    withSQLContext {
        BrowserSettings.withGUI()

        val crawler = VerboseCrawler(it)
        crawler.loadOutPages(seed, args)
    }
}
