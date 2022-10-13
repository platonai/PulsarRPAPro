package ai.platon.exotic.examples.sites.language

import ai.platon.pulsar.context.PulsarContexts
import ai.platon.pulsar.crawl.event.impl.CloseMaskLayerHandler

fun main() {
    val portalUrl = "https://shopee.co.th/กระเป๋าเป้ผู้ชาย-cat.49.1037.10297?page=1"
    val args = """
        -i 1s -ii 1s -ol ".shopee-search-item-result__item a" -sc 10
    """.trimIndent()
    val closeMaskLayerSelector = ".language-selection button:nth-child(1)"

    val session = PulsarContexts.createSession()
    val options = session.options(args)

    val closeMaskLayerHandler = CloseMaskLayerHandler(closeMaskLayerSelector)
    options.event.browseEvent.onDocumentActuallyReady.addLast(closeMaskLayerHandler)

    session.loadOutPages(portalUrl, options)
}
