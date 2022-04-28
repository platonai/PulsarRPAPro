package ai.platon.exotic.examples.sites.video

import ai.platon.pulsar.context.PulsarContexts

fun main() {
    val portalUrl = "https://www.bilibili.com/movie/?spm_id_from=333.1007.0.0"
    val args = "-i 1s -ii 1d -ol a[href~=play] -tl 20 -ignoreFailure"
    val session = PulsarContexts.createSession()

    val fieldSelectors = mapOf(
        "title" to ".media-title",
        "addedTime" to ".pub-info",
        "score" to "h4.score",
        "ratings" to ".media-rating p"
    )
    val fields = session.scrapeOutPages(portalUrl, args, fieldSelectors)
    println(fields)
}
