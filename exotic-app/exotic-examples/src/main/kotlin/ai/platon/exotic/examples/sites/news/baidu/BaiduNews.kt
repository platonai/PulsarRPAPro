package ai.platon.exotic.examples.sites.news.baidu

import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.scent.context.ScentContexts

class BaiduNews {
    fun scrape() {
        val url = "https://baijiahao.baidu.com/s?id=1794841338241546440"
        val session = ScentContexts.createSession()
        val document = session.harvestArticle(url, session.options())

        println(document.contentTitle)
        println(document.textContent)
    }
}

fun main() {
    BrowserSettings.withSystemDefaultBrowser()
    BaiduNews().scrape()
}
