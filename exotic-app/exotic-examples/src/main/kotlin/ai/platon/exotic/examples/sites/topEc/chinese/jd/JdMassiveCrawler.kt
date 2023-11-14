package ai.platon.exotic.examples.sites.topEc.chinese.jd

import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.common.LinkExtractors
import ai.platon.pulsar.common.urls.Hyperlink
import ai.platon.pulsar.context.PulsarContexts
import ai.platon.pulsar.crawl.common.url.ParsableHyperlink
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.persist.WebPage

fun main() {
    BrowserSettings.privacyContext(2).maxTabs(8).headless()

    val context = PulsarContexts.create()
    val parseHandler = { _: WebPage, document: FeaturedDocument ->
        val urls = document.select("#J_goodsList a[href~=item]")
            .mapTo(mutableSetOf()) { it.attr("abs:href") }
        println("" + urls.size + "\t|\t" + document.title + "\t|\t" + document.baseURI)

        context.submitAll(urls.take(10).map { Hyperlink(it) })
    }

    val urls = LinkExtractors.fromResource("sites/jd/categories.txt")
        .map { ParsableHyperlink("$it -refresh", parseHandler) }
    context.submitAll(urls)
    context.await()
}
