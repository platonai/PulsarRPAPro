package ai.platon.exotic.examples.sites.news.eeo

import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.scent.context.ScentContexts

class EEO {
    fun scrape() {
        val url = "https://www.eeo.com.cn/2024/0330/648712.shtml"
        val session = ScentContexts.createSession()
        val document = session.harvestArticle(url, session.options())

        println(document.contentTitle)
        println(document.textContent)
    }
}

fun main() {
    BrowserSettings.withSystemDefaultBrowser()
    EEO().scrape()
}
