package ai.platon.exotic.examples.sites.topEc.chinese.jd

import ai.platon.pulsar.browser.common.DisplayMode
import ai.platon.pulsar.common.LinkExtractors
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.urls.Hyperlink
import ai.platon.pulsar.context.PulsarContexts
import ai.platon.pulsar.crawl.common.url.ParsableHyperlink
import ai.platon.pulsar.persist.WebPage
import org.jsoup.nodes.Document

fun main() {
    System.setProperty(CapabilityTypes.BROWSER_DISPLAY_MODE, DisplayMode.HEADLESS.name)
    System.setProperty(CapabilityTypes.PRIVACY_CONTEXT_NUMBER, "2")
    System.setProperty(CapabilityTypes.BROWSER_MAX_ACTIVE_TABS, "8")

    val context = PulsarContexts.create()
    val parseHandler = { _: WebPage, document: Document ->
        val urls = document.select("#J_goodsList a[href~=item]")
            .mapTo(mutableSetOf()) { it.attr("abs:href") }
        println("" + urls.size + "\t|\t" + document.title() + "\t|\t" + document.baseUri())

        context.submitAll(urls.take(10).map { Hyperlink(it) })
        Unit
    }

    val urls = LinkExtractors.fromResource("sites/jd/categories.txt")
        .map { ParsableHyperlink("$it -refresh", parseHandler) }
    context.submitAll(urls)
    context.await()
}
