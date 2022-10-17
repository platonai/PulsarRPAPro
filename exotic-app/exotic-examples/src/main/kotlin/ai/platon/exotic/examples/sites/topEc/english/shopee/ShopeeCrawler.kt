package ai.platon.exotic.examples.sites.topEc.english.shopee

import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.context.PulsarContexts
import ai.platon.pulsar.crawl.common.url.ParsableHyperlink
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.persist.WebPage

fun main() {
    BrowserSettings.headless()
    BrowserSettings.withWorseNetwork()

    val portalUrls = """
        https://shopee.sg/Mobile-Gadgets-cat.11013350
        https://shopee.sg/Home-Appliances-cat.11027421
        https://shopee.sg/Food-Beverages-cat.11011871
        https://shopee.sg/Kids-Fashion-cat.11012218
        https://shopee.sg/Women's-Apparel-cat.11012819
        https://shopee.sg/Sports-Outdoors-cat.11012018
    """.trimIndent().split("\n").map { it.trim() }.map { "$it?page={pageNumber}" }
        .flatMap { url -> IntRange(1, 8).map { url.replace("{pageNumber}", "$it") } }
        .shuffled()
    val args = "-i 1s -ignoreFailure -refresh"

    val context = PulsarContexts.create()

    val parseHandler = { _: WebPage, document: FeaturedDocument ->
        context.submitAll(document.selectHyperlinks("a[href~=sp_atk]"))
    }

    val links = portalUrls.map { ParsableHyperlink("$it $args", parseHandler) }
    context.submitAll(links).await()
}
